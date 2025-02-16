# Avaulgit

**WORK IN PROGRESS: INITIAL RELEASE IS IN THE MAKING**

Avaulgit is a library for
[Spring Boot](https://docs.spring.io/spring-boot/index.html)
that allows you to store encrypted secrets in your configuration files. It is a
simple and leightweight method for avoiding plaintext credentials in source code
repositories and/or container images (see
[OWASP CICD-SEC-6: Insufficient Credential Hygiene](https://owasp.org/www-project-top-10-ci-cd-security-risks/CICD-SEC-06-Insufficient-Credential-Hygiene)).

Avaulgit is published under the
[MIT license](http://opensource.org/licenses/MIT). It requires at least Java 17
and Spring Boot 3.

## Usage

You need to use YAML for your configuration files, e.g. `application.yml`. Let's
assume that you have a project with database access and the following
`application.yml`:

    spring:
      datasource:
        url: jdbc:h2:mem:mydb
        username: sa
        password: password

Use [Ansible Vault]() to encrypt the password in the file. The file's content is
replaced by

    spring:
      datasource:
        url: jdbc:h2:mem:mydb
        username: sa
        password: !vault |
          $ANSIBLE_VAULT;1.1;AES256
          32303666343964656137356266613665663266653261646438626133626134353863636630643931
          6263623332343066323439323635623365356332626461370a373834313835396334663562363833
          37396434376433613863373563313737653566366431613938393132633763623137663462626561
          6232353530306430620a353863306563663138303731376363623762636530656633306635653463
          3036

Instead of Ansible Vault itself you may use Ansible Vault support in you IDE for
encrypting the password, e.g.
[Ansible Vault Editor](https://plugins.jetbrains.com/plugin/14278-ansible-vault-editor)
for [IntelliJ IDEA](https://www.jetbrains.com/idea/)

As a last step you need to add Avaulgit to you project's dependencies. If you're
using Maven, please add

    <dependency>
      <groupId>com.github.stefanbirkner</groupId>
      <artifactId>avaulgit</artifactId>
      <version>1.0.0</version>
    </dependency>

If you're using Gradle, please add

    implementation 'com.github.stefanbirkner:avaulgit:1.0.0'
