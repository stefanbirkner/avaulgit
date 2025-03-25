package com.github.stefanbirkner.avaulgit;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayNameGenerator.*;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DecryptorTest {
    private static final String VAULT_TEXT_FOR_ORIGINAL_SECRET = """
        $ANSIBLE_VAULT;1.1;AES256
        33376630363236353839326136323337616663396463656632623265363339343537653937616139
        6430356636313138366364643337653765383231656234630a613732363764383865613361656331
        30323763636135383930323538356537326133613736633737343361373035626239653738393562
        3062313433373737330a363931323135336163656337393630353536396530383366663030613738
        3833
        """;
    private static final String VAULT_TEXT_FOR_ORIGINAL_SECRET_WITH_UPPER_CASE_HEX_ENCODING = """
        $ANSIBLE_VAULT;1.1;AES256
        33374630363236353839324136323337414643394443454632423245363339343537453937414139
        4430354636313138364344443337453745383231454234430A413732363744383845413341454331
        30323743434135383930323538354537324133413736433737343341373035424239453738393542
        3042313433373737330A363931323135334143454337393630353536394530383346463030413738
        3833
        """;
    private final Decryptor decryptor = new Decryptor("the-secret-vault-key");

    @Test
    void encrypted_string_with_lower_case_hex_digits_is_decrypted_to_its_plain_text() throws Exception {
        var plainText = decryptor.decrypt(VAULT_TEXT_FOR_ORIGINAL_SECRET);

        assertThat(plainText).isEqualTo("original secret");
    }

    @Test
    void encrypted_string_with_upper_case_hex_digits_is_decrypted_to_its_plain_text() throws Exception {
        var plainText = decryptor.decrypt(
            VAULT_TEXT_FOR_ORIGINAL_SECRET_WITH_UPPER_CASE_HEX_ENCODING);

        assertThat(plainText).isEqualTo("original secret");
    }

    @Test
    void rejects_vault_text_with_unsupported_version() {
        var vaultText = VAULT_TEXT_FOR_ORIGINAL_SECRET.replaceFirst("1.1", "1.0");

        assertThatThrownBy(() -> decryptor.decrypt(vaultText))
            .isInstanceOf(InvalidVaultTextException.class)
            .hasMessage("Cannot decrypt vault text because only"
                + " $ANSIBLE_VAULT;1.1;AES256 is supported.");
    }

    @Test
    void rejects_vault_text_with_only_two_parts() {
        //remove one new line
        var vaultText = VAULT_TEXT_FOR_ORIGINAL_SECRET.replaceFirst("0a", "eb");

        assertThatThrownBy(() -> decryptor.decrypt(vaultText))
            .isInstanceOf(InvalidVaultTextException.class)
            .hasMessage("The vault text is not valid because it has 2 parts"
                + " instead of 3 (salt, HMAC, cipher text).");
    }

    @Test
    void rejects_vault_text_with_non_hexlify_characters() {
        assertThatThrownBy(() -> decryptor.decrypt("""
            $ANSIBLE_VAULT;1.1;AES256
            33376630363236353839326136323337616663396463656632623265363339343537653937616139
            6430356636313r38366364643337653765383231656234630a613732363764383865613361656331
            30303763636135383930323538356537326133613736633737343361373035626239653738393562
            3062313433373737330a363931323135336163656337393630353536396530383366663030613738
            3833
            """))
            .isInstanceOf(InvalidVaultTextException.class)
            .hasMessage("The vault text is corrupted.")
            .hasRootCauseInstanceOf(NumberFormatException.class)
            .hasRootCauseMessage("not a hexadecimal digit: \"r\" = 114");
    }

    @Test
    void rejects_vault_text_with_non_matching_hmac() {
        assertThatThrownBy(() -> decryptor.decrypt("""
            $ANSIBLE_VAULT;1.1;AES256
            33376630363236353839326136323337616663396463656632623265363339343537653937616139
            6430356636313138366364643337653765383231656234630a613732363764383865613361656331
            30303763636135383930323538356537326133613736633737343361373035626239653738393562
            3062313433373737330a363931323135336163656337393630353536396530383366663030613738
            3833
            """))
            .isInstanceOf(WrongSignatureException.class)
            .hasMessage("The vault password is wrong or the vault text is"
                + " corrupt because the HMAC does not match.");
    }

    @Test
    void rejects_vault_text_with_odd_number_of_characters() {
        assertThatThrownBy(() -> decryptor.decrypt("""
            $ANSIBLE_VAULT;1.1;AES256
            32303666343964656137356266613665663266653261646438626133626134353863636630643931
            6263623332343066323439323635623365356332626461370a373834313835396334663562363833
            37396434376433613863373563313737653566366431613938393132633763623137663462626561
            6232353530306430620a353863306563663138303731376363623762636530656633306635653463
            303
            """))
            .isInstanceOf(InvalidVaultTextException.class)
            .hasMessage("The vault text is corrupted.")
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("string length not even: 323");
    }

    @Test
    void rejects__null_vault_text() {
        assertThatThrownBy(() -> decryptor.decrypt(null))
            .isInstanceOf(InvalidVaultTextException.class)
            .hasMessage("The vault text is blank.");
    }

    @Test
    void rejects__blank_vault_text() {
        assertThatThrownBy(() -> decryptor.decrypt("   "))
            .isInstanceOf(InvalidVaultTextException.class)
            .hasMessage("The vault text is blank.");
    }
}
