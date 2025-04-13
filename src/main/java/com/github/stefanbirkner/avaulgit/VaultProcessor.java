package com.github.stefanbirkner.avaulgit;

import static java.util.Objects.requireNonNull;

import java.security.GeneralSecurityException;
import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;

/**
 * Decrypt's properties that have been encrypted with Ansible Vault.
 */
public class VaultProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(
        ConfigurableEnvironment environment,
        SpringApplication application
    ) {
        for (var propertySource : getEnumerablePropertySources(environment)) {
            decryptProperties(environment, propertySource);
        }
    }

    private void decryptProperties(
        ConfigurableEnvironment environment,
        EnumerablePropertySource<?> propertySource
    ) {
        var atLeastOnePropertyWasASecret = false;
        Decryptor decryptor = null;
        var decryptedProperties = new HashMap<String, Object>();
        for (var name : propertySource.getPropertyNames()) {
            var property = propertySource.getProperty(name);
            if (property instanceof Secret secret) {
                try {
                    if (decryptor == null)
                        decryptor = createDecryptor(environment);
                    var plaintext = decryptor.decrypt(secret.value());
                    decryptedProperties.put(name, plaintext);
                    atLeastOnePropertyWasASecret = true;
                } catch (WrongSignatureException e) {
                    throw new RuntimeException(
                        "Cannot decrypt property '" + name + "'. Either the"
                            + " vault password is wrong or the property's"
                            + " value is corrupt.",
                        e);
                } catch (GeneralSecurityException | InvalidVaultTextException e) {
                    throw new RuntimeException(
                        "Cannot decrypt property '" + name + "'.",
                        e);
                }
            }
            else
                decryptedProperties.put(name, property);
        }
        if (atLeastOnePropertyWasASecret)
            environment.getPropertySources().replace(
                propertySource.getName(),
                new MapPropertySource(
                    propertySource.getName() + " decrypted",
                    decryptedProperties));
    }

    private Decryptor createDecryptor(
        ConfigurableEnvironment environment
    ) {
        var password = requireNonNull(
            environment.getProperty("vault.password", String.class),
            "Cannot decrypt secrets because property 'vault.password' is not"
                + " set.");
        return new Decryptor(password);
    }

    private List<EnumerablePropertySource> getEnumerablePropertySources(
        ConfigurableEnvironment environment
    ) {
        return environment.getPropertySources().stream()
            .filter(EnumerablePropertySource.class::isInstance)
            .map(EnumerablePropertySource.class::cast)
            .toList();
    }
}
