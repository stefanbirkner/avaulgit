# Avaulgit

![Build Status](https://github.com/stefanbirkner/avaulgit/actions/workflows/ci.yml/badge.svg?branch=main)

Avaulgit brings encrypted passwords/secrets to
[Spring Boot](https://docs.spring.io/spring-boot/index.html). With Avaulgit you can store
secrets encrypted in your configuration files.
[Ansible Vault](https://docs.ansible.com/ansible/latest/vault_guide/index.html)
is used for encrypting the secrets. There are plugins for IntelliJ IDEA
([Ansible Vault Editor](https://plugins.jetbrains.com/plugin/14278-ansible-vault-editor)) and Visual Code
([plugin list](https://marketplace.visualstudio.com/search?term=%22Ansible%20Vault%22&target=VSCode))
that allow you to modify the secrets in the configuration files directly without
Ansible Vault.
With Avaulgit you finally can get rid of plaintext secrets in source code
repositories and/or container images, which are one of the top 10 security risks
(see
[OWASP CICD-SEC-6: Insufficient Credential Hygiene](https://owasp.org/www-project-top-10-ci-cd-security-risks/CICD-SEC-06-Insufficient-Credential-Hygiene)).

Avaulgit is published under the
[Apache License, Version 2.0](https://opensource.org/license/apache-2-0). It
requires at least Java 17 and Spring Boot 3.

## Usage

There are three steps for using encrypted secrets with Spring Boot and Avaulgit.

1. Encrypt secrets
2. Add Avaulgit to your dependencies
3. Provide the vault password when you start the Spring Boot application

### 1. Encrypt Secrets

Avaulgit only works with YAML configuration files, e.g. `application.yml`. Let's
assume that you have a project with database access and your `application.yml`
look like:

    spring:
      datasource:
        url: jdbc:h2:mem:mydb
        username: sa
        password: original secret

Choose a vault password and encrypt the database password with
[Ansible Vault](https://docs.ansible.com/ansible/latest/vault_guide/index.html)
or a plugin of your IDE. In the example below the vault password
`the-secret-vault-password` is used for encryption (you get a different output
since the timestamp is encoded in the ciphertext):

    spring:
      datasource:
        url: jdbc:h2:mem:mydb
        username: sa
        password: !vault |
          $ANSIBLE_VAULT;1.1;AES256
          36306266363535333031316134333331323830393336663830373536663338393664623733663739
          3362643935363430626331363532646665613431636230660a383166306238616637333161653832
          33666539653464373737616161646434353962653862306564323639666639393538346132363339
          3732636234626437610a336537316365663264366131363762666235666530336664366365623335
          6538

### 2. Add Avaulgit as Dependency

As a last step you add Avaulgit to you project's dependencies. If you're using
Maven, please add

    <dependency>
      <groupId>com.github.stefanbirkner</groupId>
      <artifactId>avaulgit</artifactId>
      <version>1.0.0</version>
    </dependency>

to your `pom.xml`. If you're using Gradle, please add

    implementation 'com.github.stefanbirkner:avaulgit:1.0.0'

to your `build.gradle` file.

### 3. Provide the Vault Password at Runtime

When starting the Spring Boot application you need to provide the vault password
that you used for the encryption. Avaulgit reads it from the property
`vault.password`. You can use all
property sources that are supported by Spring Boot, e.g. environment variables
or system properties.

    env vault.password='the-secret-vault-password' java -jar your-application.jar

The property `spring.datasource.password` has the value `original secret` while
running your application. E.g. the simple application

    @SpringBootApplication
    public class DemoApplication {

      public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
      }

      DemoApplication(@Value("${spring.datasource.password}") String password) {
        System.out.println("Plaintext password: " + password);
      }
    }

has the output

    Plaintext password: original secret

and Spring uses the plaintext password for the database connection.


## Contributing

You have three options if you have a feature request, found a bug or
simply have a question about Avaulgit.

* [Write an issue.](https://github.com/stefanbirkner/avaulgit/issues/new)
* Create a pull request. (See [Understanding the GitHub Flow](https://guides.github.com/introduction/flow/index.html))
* [Write a mail to mail@stefan-birkner.de](mailto:mail@stefan-birkner.de)


## Development Guide

Avaulgit is built with [Maven](http://maven.apache.org/) and must be compiled
with JDK 17. If you want to contribute code then

* Please write a test for your change.
* Ensure that you didn't break the build by running `mvnw clean verify -Dgpg.skip`.
* Fork the repo and create a pull request. (See [Understanding the GitHub Flow](https://guides.github.com/introduction/flow/index.html))

The basic coding style is described in the
[EditorConfig](http://editorconfig.org/) file `.editorconfig`. You don't have to
care about formatting, Avaulgit's maintainer will adjust the code format to his
needs.

Avaulgit supports [GitHub Actions](https://help.github.com/en/actions). Each
pull request is automatically built and tested.


### Project Decisions

There are decision records for some decisions that had been made for Avaulgit.
They are stored in the folder [doc/Decision Records](doc/Decision%20Records).


## Release Guide

* Select a new version according to the
  [Semantic Versioning 2.0.0 Standard](http://semver.org/).
* Update `Changelog.md`.
* Set the new version in `pom.xml` and in the `Usage` section of this readme.
* Commit the modified `Changelog.md`, `pom.xml` and `README.md`.
* Run `mvn clean deploy` with JDK 17.
* Add a tag for the release: `git tag avaulgit-X.X.X`
