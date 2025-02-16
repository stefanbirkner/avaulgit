package com.github.stefanbirkner.avaulgit;

import static java.util.Collections.unmodifiableMap;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import java.util.*;
import org.springframework.boot.env.*;
import org.springframework.core.annotation.*;
import org.springframework.core.env.*;
import org.springframework.core.io.*;

/**
 * Strategy to load {@code .yml} (or {@code .yaml}) files with Ansible Vault
 * encrypted secrets into a {@code PropertySource}.
 */
// We need to use @Order so that
// YamlPropertySourceLoaderWithAnsibleVaultSupport is used before the original
// YamlPropertySourceLoader. Otherwise, the original YamlPropertySourceLoader
// fails when reading the encrypted secret and the Spring application does not
// start.
@Order(HIGHEST_PRECEDENCE + 1)
public class YamlPropertySourceLoaderWithAnsibleVaultSupport
    implements PropertySourceLoader
{
    private static final boolean IMMUTABLE = true;

    @Override
    public String[] getFileExtensions() {
        return new String[]{"yml", "yaml"};
    }

    @Override
    public List<PropertySource<?>> load(
        String name,
        Resource resource
    ) {
        var loaded = new OriginTrackedYamlLoaderWithAnsibleVaultSupport(resource).load();
        var propertySources = new ArrayList<PropertySource<?>>(loaded.size());
        for (var i = 0; i < loaded.size(); ++i) {
            var nameSuffix = loaded.size() == 1 ? "" : "document #" + i + ")";
            propertySources.add(new OriginTrackedMapPropertySource(
                name + nameSuffix,
                unmodifiableMap(loaded.get(i)),
                IMMUTABLE));
        }
        return propertySources;
    }
}
