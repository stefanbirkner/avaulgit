package com.github.stefanbirkner.avaulgit;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayNameGenerator.*;
import org.springframework.core.env.*;

@DisplayNameGeneration(ReplaceUnderscores.class)
class VaultProcessorTest {
    private final StandardEnvironment environment = new StandardEnvironment();
    private final VaultProcessor processor = new VaultProcessor();

    @Test
    void property_is_decrypted() {
        environment.getPropertySources().addFirst(
            new MapPropertySource(
                "test properties",
                Map.of(
                    "vault.password",
                    "the-secret-vault-key",
                    "my.secret",
                    new Secret(
                        """
                            $ANSIBLE_VAULT;1.1;AES256
                            33376630363236353839326136323337616663396463656632623265363339343537653937616139
                            6430356636313138366364643337653765383231656234630a613732363764383865613361656331
                            30323763636135383930323538356537326133613736633737343361373035626239653738393562
                            3062313433373737330a363931323135336163656337393630353536396530383366663030613738
                            3833
                            """))));

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("my.secret"))
            .isEqualTo("original secret");
    }

    @Test
    void unencrypted_property_is_not_changed() {
        environment.getPropertySources().addFirst(
            new MapPropertySource(
                "test properties",
                Map.of(
                    "vault.password",
                    "the-secret-vault-key",
                    "my.secret",
                    new Secret(
                        """
                            $ANSIBLE_VAULT;1.1;AES256
                            33376630363236353839326136323337616663396463656632623265363339343537653937616139
                            6430356636313138366364643337653765383231656234630a613732363764383865613361656331
                            30323763636135383930323538356537326133613736633737343361373035626239653738393562
                            3062313433373737330a363931323135336163656337393630353536396530383366663030613738
                            3833
                            """),
                    "some.property",
                    "some value")));

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("some.property"))
            .isEqualTo("some value");
    }

    @Test
    void processor_fails_if_password_for_decryption_is_missing() {
        environment.getPropertySources().addFirst(
            new MapPropertySource(
                "test properties",
                Map.of(
                    "my.secret",
                    new Secret(
                        """
                            $ANSIBLE_VAULT;1.1;AES256
                            33376630363236353839326136323337616663396463656632623265363339343537653937616139
                            6430356636313138366364643337653765383231656234630a613732363764383865613361656331
                            30323763636135383930323538356537326133613736633737343361373035626239653738393562
                            3062313433373737330a363931323135336163656337393630353536396530383366663030613738
                            3833
                            """))));

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, null))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Cannot decrypt secrets because property"
                + " 'vault.password' is not set.");
    }

    @Test
    void processor_fails_if_vault_text_is_invalid() {
        environment.getPropertySources().addFirst(
            new MapPropertySource(
                "test properties",
                Map.of(
                    "vault.password",
                    "the-secret-vault-key",
                    "my.secret",
                    new Secret(
                        """
                            $ANSIBLE_VAULT;1.1;AES256
                            33376630363236353839326136323337616663396463656632623265363339343537653937616139
                            6430356636313138366364643337653765383231656234630a613732363764383865613361656331
                            30323763636135383930323538356537326133613736633737343361373035626239653738393562
                            3062313433373737330a363931323135336163656337393630353536396530383366663030613738
                            383
                            """))));

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, null))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Cannot decrypt property 'my.secret'.")
            .hasCauseInstanceOf(InvalidVaultTextException.class);
    }

    @Test
    void no_password_is_needed_if_there_is_no_encrypted_property() {
        environment.getPropertySources().addFirst(
            new MapPropertySource(
                "test properties",
                Map.of(
                    "first.property",
                    "one",
                    "second.property",
                    "two")));

        assertThatCode(() -> processor.postProcessEnvironment(environment, null))
            .doesNotThrowAnyException();
    }
}
