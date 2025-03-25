package com.github.stefanbirkner.avaulgit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.copyOfRange;
import static javax.crypto.Cipher.DECRYPT_MODE;

import java.security.*;
import java.security.spec.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * A {@code Decryptor} decrypts Strings that have been encrypted by
 * <a href="https://docs.ansible.com/ansible/latest/cli/ansible-vault.html">Ansible
 * Vault</a>.
 */
class Decryptor {
    private static final HexFormat HEX_PARSER = HexFormat.of();
    private static final String SUPPORTED_HEADER = "$ANSIBLE_VAULT;1.1;AES256\n";
    private final String vaultPassword;

    /**
     * Creates a {@code Decryptor} that decrypts secrets which are encrypted
     * with the {@code vaultPassword}.
     * @param vaultPassword the password that was used for encrypting the
     *                      secrets.
     */
    Decryptor(
        String vaultPassword
    ) {
        this.vaultPassword = vaultPassword;
    }

    String decrypt(
        String vaultTextWithHeader
    ) throws GeneralSecurityException, InvalidVaultTextException {
        validateNotBlank(vaultTextWithHeader);
        validateHeader(vaultTextWithHeader);

        var vaultText = vaultTextWithHeader
            .substring(SUPPORTED_HEADER.length())
            .replace("\n", "")
            .replace("\r", "");
        var unhexlifiedVaultText = unhexlifyToString(vaultText);
        var parts = split(unhexlifiedVaultText);
        var salt = unhexlify(parts[0]);
        var hmac = unhexlify(parts[1]);
        var ciphertext = unhexlify(parts[2]);
        return getPlaintext(salt, hmac, ciphertext);
    }

    private void validateNotBlank(
        String vaultTextWithHeader
    ) throws InvalidVaultTextException {
        if (vaultTextWithHeader == null || vaultTextWithHeader.isBlank())
            throw new InvalidVaultTextException("The vault text is blank.");
    }

    private void validateHeader(
        String vaultTextWithHeader
    ) throws InvalidVaultTextException {
        if (!vaultTextWithHeader.startsWith(SUPPORTED_HEADER))
            throw new InvalidVaultTextException(
                "Cannot decrypt vault text because only "
                    + SUPPORTED_HEADER.trim() + " is supported.");
    }

    private String[] split(
        String unhexlifiedVaultText
    ) throws InvalidVaultTextException {
        var parts = unhexlifiedVaultText.split("\n");
        if (parts.length == 3)
            return parts;
        else
            throw new InvalidVaultTextException(
                "The vault text is not valid because it has "
                    + parts.length
                    + " parts instead of 3 (salt, HMAC, cipher text).");
    }

    private String unhexlifyToString(
        String hexlifiedData
    ) throws InvalidVaultTextException {
        var raw = unhexlify(hexlifiedData);
        return new String(raw, UTF_8);
    }

    // Java port of "unhexlify" of
    // https://docs.python.org/3/library/binascii.html
    private byte[] unhexlify(
        String hexlifiedData
    ) throws InvalidVaultTextException {
        try {
            return HEX_PARSER.parseHex(hexlifiedData);
        } catch (IllegalArgumentException e) {
            throw new InvalidVaultTextException(
                "The vault text is corrupted.",
                e);
        }
    }

    private String getPlaintext(
        byte[] salt,
        byte[] expectedHmac,
        byte[] ciphertext
    ) throws GeneralSecurityException, WrongSignatureException {
        var secretKey = getSecretKey(salt);
        var cipherKey = new SecretKeySpec(secretKey, 0, 32, "AES");
        var hmacKey = new SecretKeySpec(secretKey, 32, 32, "AES");
        var iv = new IvParameterSpec(copyOfRange(secretKey, 64, 64 + 16));
        validateMessage(ciphertext, hmacKey, expectedHmac);
        var plaintext = decrypt(cipherKey, iv, ciphertext);
        return new String(plaintext, UTF_8);
    }

    private byte[] getSecretKey(
        byte[] salt
    ) throws InvalidKeySpecException, NoSuchAlgorithmException {
        var keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        var secretKey = keyFactory.generateSecret(new PBEKeySpec(
            vaultPassword.toCharArray(),
            salt,
            10_000,
            (32 + 32 + 16) * 8));
        return secretKey.getEncoded();
    }

    private byte[] getHmac(
        byte[] ciphertext,
        SecretKeySpec key
    ) throws InvalidKeyException, NoSuchAlgorithmException {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        return mac.doFinal(ciphertext);
    }

    private void validateMessage(
        byte[] ciphertext,
        SecretKeySpec hmacKey,
        byte[] expectedHmac
    ) throws InvalidKeyException, NoSuchAlgorithmException, WrongSignatureException {
        var hmac = getHmac(ciphertext, hmacKey);
        if (!Arrays.equals(hmac, expectedHmac))
            throw new WrongSignatureException();
    }

    private byte[] decrypt(
        SecretKeySpec cipherKey,
        IvParameterSpec iv,
        byte[] ciphertext
    ) throws GeneralSecurityException {
        var cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(DECRYPT_MODE, cipherKey, iv);
        var payload = cipher.doFinal(ciphertext);
        return stripPKCS5Padding(payload, cipher);
    }

    private byte[] stripPKCS5Padding(
        byte[] payload,
        Cipher cipher
    ) {
        var padding = payload[payload.length - 1];
        if (padding <= cipher.getBlockSize() && padding > 0)
            return copyOfRange(payload, 0, payload.length - padding);
        else
            return payload;
    }
}
