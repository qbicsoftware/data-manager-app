package life.qbic.domain.usermanagement.policies

import life.qbic.identityaccess.domain.user.policy.PasswordEncryptionPolicy
import spock.lang.Specification

/**
 * <b>Tests for the {@link PasswordEncryptionPolicy}</b>
 *
 * @since 1.0.0
 */
class PasswordEncryptionPolicySpec extends Specification {

  def "The password is not stored in clear text"() {
    when:
    String encryptedPassword = PasswordEncryptionPolicy.instance().encrypt(password.toCharArray())

    then:
    encryptedPassword != password

    where:
    password = "12345678"
  }

  def "The password policy matches same passwords"() {
    when:
    String encryptedPassword = PasswordEncryptionPolicy.instance().encrypt(password.toCharArray())
    boolean result = PasswordEncryptionPolicy.instance().doPasswordsMatch(password as char[], encryptedPassword)

    then:
    result

    where:
    password = "12345678"
  }

  def "Two different passwords cannot be matched"() {
    when:
    String encryptedPassword = PasswordEncryptionPolicy.instance().encrypt(password.toCharArray())
    boolean result = PasswordEncryptionPolicy.instance().doPasswordsMatch(anotherPassword as char[], encryptedPassword)

    then:
    !result

    where:
    password = "12345678"
    anotherPassword = "abcdefghijkl"
  }

  def "The encrypted password starts with the number of iterations"() {
    when:
    String encryptedPassword = PasswordEncryptionPolicy.instance().encrypt(password.toCharArray())
    String[] passwordElements = encryptedPassword.split(":")

    then:
    passwordElements[0].startsWith("10000") // number of iterations

    where:
    password = "abcdefghihdeo"
  }

  def "The encrypted password contains a salt with a length of 20 bytes"() {
    when:
    String encryptedPassword = PasswordEncryptionPolicy.instance().encrypt(password.toCharArray())
    String[] passwordElements = encryptedPassword.split(":")

    then:
    passwordElements[1].length() == 40 //contains a salt with length of 20 bytes

    where:
    password = "abcdefghihdeo"
  }

  def "The encrypted password ends with the hashed password"() {
    when:
    String encryptedPassword = PasswordEncryptionPolicy.instance().encrypt(password.toCharArray())
    String[] passwordElements = encryptedPassword.split(":")

    then:
    passwordElements[2] != password // contains a hash that is not the same as the clear text password

    where:
    password = "abcdefghihdeo"
  }

  def "Equal raw user passwords must not create the same secret"() {
    when:
    String encryptedPasswordA = PasswordEncryptionPolicy.instance().encrypt(passwordA.toCharArray())
    String encryptedPasswordB = PasswordEncryptionPolicy.instance().encrypt(passwordB.toCharArray())
    String secretA = encryptedPasswordA.split(":")[2]
    String secretB = encryptedPasswordB.split(":")[2]

    then:
    secretA != secretB

    where:
    passwordA = "helloworld"

    and:
    passwordB = "helloworld"
  }

  def "The encrypted password has a length of 256 bits independent of the raw password length"() {
    when:
    String encryptedPassword = PasswordEncryptionPolicy.instance().encrypt(password.toCharArray())

    then:
    encryptedPassword.length() == 111

    where:
    password                      | _
    "abcdefghijklmno"             | _
    "123456789"                   | _
    "qwerty"                      | _
    "password"                    | _
    "monkey"                      | _
    "This_Is_A_Real_Password123!" | _
    "Hunter1"                     | _
    " "                           | _
    "DEFAULT"                     | _
  }
}
