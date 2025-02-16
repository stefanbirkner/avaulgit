package com.github.stefanbirkner.avaulgit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;

// Test the complete feature with the happy path and the major failure cases.
@DisplayNameGeneration(ReplaceUnderscores.class)
public class AvaulgitTest {
    private final StandardEnvironment environment = new StandardEnvironment();
    private final YamlPropertySourceLoaderWithAnsibleVaultSupport yamlPropertySourceLoader
        = new YamlPropertySourceLoaderWithAnsibleVaultSupport();
    private final VaultProcessor processor = new VaultProcessor();

    @Test
    void encrypted_password_is_decrypted() {
        setProperty("vault.password", "the-secret-vault-password");
        addConfiguration("""
            database:
              password: !vault |
                $ANSIBLE_VAULT;1.1;AES256
                36306266363535333031316134333331323830393336663830373536663338393664623733663739
                3362643935363430626331363532646665613431636230660a383166306238616637333161653832
                33666539653464373737616161646434353962653862306564323639666639393538346132363339
                3732636234626437610a336537316365663264366131363762666235666530336664366365623335
                6538
            """);

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("database.password"))
            .isEqualTo("original secret");
    }

    @Test
    void user_is_told_that_they_use_wrong_password() {
        setProperty("vault.password", "wrong password");
        addConfiguration("""
            database:
              password: !vault |
                $ANSIBLE_VAULT;1.1;AES256
                36306266363535333031316134333331323830393336663830373536663338393664623733663739
                3362643935363430626331363532646665613431636230660a383166306238616637333161653832
                33666539653464373737616161646434353962653862306564323639666639393538346132363339
                3732636234626437610a336537316365663264366131363762666235666530336664366365623335
                6538
            """);

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, null))
            .hasMessage("Cannot decrypt property 'database.password'. Either"
                + " the vault password is wrong or the property's value is"
                + " corrupt.");
    }

    @Test
    void user_is_told_that_the_vault_password_is_missing() {
        addConfiguration("""
            database:
              password: !vault |
                $ANSIBLE_VAULT;1.1;AES256
                36306266363535333031316134333331323830393336663830373536663338393664623733663739
                3362643935363430626331363532646665613431636230660a383166306238616637333161653832
                33666539653464373737616161646434353962653862306564323639666639393538346132363339
                3732636234626437610a336537316365663264366131363762666235666530336664366365623335
                6538
            """);

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, null))
            .hasMessage("Cannot decrypt secrets because property"
                + " 'vault.password' is not set.");
    }

    private void setProperty(
        String name,
        String value
    ) {
        addResource(
            new MapPropertySource(
                "property from environment",
                Map.of(name, value)));
    }

    private void addConfiguration(
        String yaml
    ) {
        var yamlResource = new ByteArrayResource(yaml.getBytes(UTF_8));
        yamlPropertySourceLoader.load("yaml with password", yamlResource)
            .forEach(
                this::addResource);
    }

    private void addResource(
        PropertySource<?> resource
    ) {
        environment.getPropertySources().addLast(resource);
    }
}
