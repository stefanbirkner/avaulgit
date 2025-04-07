package com.github.stefanbirkner.avaulgit;

import static com.github.stefanbirkner.systemlambda.SystemLambda.muteSystemOut;
import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import static java.lang.System.setProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;

// Test the complete feature with the happy path and the major failure cases.
@DisplayNameGeneration(ReplaceUnderscores.class)
public class AvaulgitTest {
    private final SpringApplication application = new SpringApplication(CustomApplication.class);

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

    static class CustomApplication {
        final String password;

        CustomApplication(
            @Value("${database.password}") String password
        ) {
            this.password = password;
        }
    }
}
