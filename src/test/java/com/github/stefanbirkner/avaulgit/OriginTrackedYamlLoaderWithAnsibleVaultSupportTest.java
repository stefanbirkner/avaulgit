package com.github.stefanbirkner.avaulgit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayNameGenerator.*;
import org.springframework.boot.origin.*;
import org.springframework.core.io.*;

@DisplayNameGeneration(ReplaceUnderscores.class)
class OriginTrackedYamlLoaderWithAnsibleVaultSupportTest {
    @Test
    public void reads_string_values_from_yaml() {
        var loader = createLoaderForYaml("""
            animal: beaver
            """);

        var result = loader.load();

        assertThat(getYamlValue(result, "animal"))
            .extracting("value")
            .isEqualTo("beaver");
    }

    @Test
    public void reads_secrets_as_Secret_objects_from_yaml() {
        var loader = createLoaderForYaml("""
            password: !vault |
              $ANSIBLE_VAULT;1.1;AES256
              33376630363236353839326136323337616663396463656632623265363339343537653937616139
              6430356636313138366364643337653765383231656234630a613732363764383865613361656331
              30323763636135383930323538356537326133613736633737343361373035626239653738393562
              3062313433373737330a363931323135336163656337393630353536396530383366663030613738
              3833
            """);

        var result = loader.load();

        assertThat(getYamlValue(result, "password"))
            .extracting("value")
            .isInstanceOf(Secret.class)
            .extracting("value")
            .isEqualTo("""
                $ANSIBLE_VAULT;1.1;AES256
                33376630363236353839326136323337616663396463656632623265363339343537653937616139
                6430356636313138366364643337653765383231656234630a613732363764383865613361656331
                30323763636135383930323538356537326133613736633737343361373035626239653738393562
                3062313433373737330a363931323135336163656337393630353536396530383366663030613738
                3833
                """);
    }

    private OriginTrackedYamlLoaderWithAnsibleVaultSupport createLoaderForYaml(
        String yaml
    ) {
        var resource = new ByteArrayResource(yaml.getBytes(UTF_8));
        return new OriginTrackedYamlLoaderWithAnsibleVaultSupport(resource);
    }

    private OriginTrackedValue getYamlValue(
        List<Map<String, Object>> result,
        String name
    ) {
        return (OriginTrackedValue) result.get(0).get(name);
    }
}
