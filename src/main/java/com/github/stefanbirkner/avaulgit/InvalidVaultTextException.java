package com.github.stefanbirkner.avaulgit;

/**
 * Thrown by {@link Decryptor} for a vault text that cannot be processed.
 */
class InvalidVaultTextException extends Exception {
    InvalidVaultTextException(
        String message
    ) {
        super(message);
    }
}
