package com.github.stefanbirkner.avaulgit;

import static com.github.stefanbirkner.systemlambda.SystemLambda.muteSystemOut;
import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import static java.lang.System.setProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Test the complete feature with the happy path and the major failure cases.
@DisplayNameGeneration(ReplaceUnderscores.class)
public class AvaulgitTest {
    private final SpringApplication application = new SpringApplication(CustomApplication.class);

    @BeforeEach
    void addConfigurationSource() {
        application.addPrimarySources(List.of(CustomConfiguration.class));
    }

    @Test
    void encrypted_password_is_decrypted() throws Exception {
        muteSystemOut(() -> restoreSystemProperties(() -> {
            setProperty("vault.password", "the-secret-vault-password");

            try (var context = application.run()) {
                var customApplication = context.getBean(CustomApplication.class);
                assertThat(customApplication.password)
                    .isEqualTo("original secret");
            }
        }));
    }

    @Test
    void user_is_told_that_they_use_wrong_password() throws Exception {
        restoreSystemProperties(() -> {
            setProperty("vault.password", "wrong password");

            assertThatThrownBy(application::run)
                .hasMessage("Cannot decrypt property 'database.password'. Either"
                    + " the vault password is wrong or the property's value is"
                    + " corrupt.");
        });
    }

    @Test
    void user_is_told_that_the_vault_password_is_missing() {
        // Property "vault.password" is not set.

        assertThatThrownBy(application::run)
            .hasMessage("Cannot decrypt secrets because property"
                + " 'vault.password' is not set.");
    }

    public static class CustomApplication {
        public final CustomConfiguration configuration;
        public final String password;

        CustomApplication(
            CustomConfiguration configuration,
            @Value("${database.password}") String password
        ) {
            this.configuration = configuration;
            this.password = password;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConfigurationProperties("custom")
    public static class CustomConfiguration {
        private List<User> users;

        public List<User> getUsers() {
            return users;
        }

        public void setUsers(List<User> users) {
            this.users = users;
        }
    }

    public static record User(
        String name,
        String password
    ) {
    }
}
