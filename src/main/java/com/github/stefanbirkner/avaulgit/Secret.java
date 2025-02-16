package com.github.stefanbirkner.avaulgit;

/**
 * An encrypted secret that is stored in a YAML file.
 */
record Secret(
    String value
) {
}
