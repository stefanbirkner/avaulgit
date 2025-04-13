# Changelog

All notable changes to this project will be documented in this file.

This project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## 1.0.2 – 2025-04-15

Support encrypted secrets in lists

With this change, we allow encrypted secrets in lists like

    users:
      - username: Alice
        password: !vault |
          $ANSIBLE_VAULT;1.1;AES256
          363062663635....
      - username: Bob
        password: !vault |
          $ANSIBLE_VAULT;1.1;AES256
          336665396534....

Before this change, Spring didn't properly create the user objects because
the passwords and the usernames are stored in different property sources.
Either the username or the password of each user was null. It is now fixed
because the username and the decrypted password are stored in the same
property source.

## 1.0.1 – 2025-03-25

Support ciphertexts that are encoded with upper-case hexadecimal digits.
According to the
[specification of Python's unhexlify](https://docs.python.org/3/library/binascii.html#binascii.unhexlify)
both lower-case and upper-case hexadecimal digits are valid.

## 1.0.0 – 2025-03-17

Initial release that decrypts simple Ansible Vault encoded secrets. The vault
password has to be provided as a property.
