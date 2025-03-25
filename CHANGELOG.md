# Changelog

All notable changes to this project will be documented in this file.

This project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## 1.0.1 – 2025-03-25

Support ciphertexts that are encoded with upper-case hexadecimal digits.
According to the
[specification of Python's unhexlify](https://docs.python.org/3/library/binascii.html#binascii.unhexlify)
both lower-case and upper-case hexadecimal digits are valid.

## 1.0.0 – 2025-03-17

Initial release that decrypts simple Ansible Vault encoded secrets. The vault
password has to be provided as a property.
