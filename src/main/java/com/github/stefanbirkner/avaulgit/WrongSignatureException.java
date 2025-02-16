package com.github.stefanbirkner.avaulgit;

/**
 * Thrown by {@link Decryptor} for a secret with a wrong signature.
 */
public class WrongSignatureException extends InvalidVaultTextException {
    public WrongSignatureException() {
        super("The vault password is wrong or the vault text is corrupt because"
            + " the HMAC does not match.");
    }
}
