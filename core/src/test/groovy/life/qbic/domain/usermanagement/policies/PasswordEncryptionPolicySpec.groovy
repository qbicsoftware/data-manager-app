package life.qbic.domain.usermanagement.policies

import life.qbic.identityaccess.domain.user.policy.PasswordEncryptionPolicy
import spock.lang.Shared
import spock.lang.Specification

/**
 * <b>Tests for the {@link PasswordEncryptionPolicy}</b>
 *
 * @since 1.0.0
 */
class PasswordEncryptionPolicySpec extends Specification {

  @Shared
  private final List<String> rawPasswords = ["abcdefghijklmno", "123456789", "qwerty", "password", "monkey", "This_Is_A_Real_Password123!", "Hunter1", " ", "DEFAULT"]

  def "The password is not stored in clear text"() {
    when:
    String encryptedPassword = PasswordEncryptionPolicy.instance().encrypt(password.toCharArray())

    then:
    encryptedPassword != password

    where:
    password << rawPasswords

  }

  def "The password policy matches same passwords"() {
    when:
    String encryptedPassword = PasswordEncryptionPolicy.instance().encrypt(password.toCharArray())
    boolean result = PasswordEncryptionPolicy.instance().doPasswordsMatch(password as char[], encryptedPassword)

    then:
    result

    where:
    password << rawPasswords
  }

  def "Two different passwords cannot be matched"() {
    when:
    String encryptedPassword = PasswordEncryptionPolicy.instance().encrypt(password.toCharArray())
    boolean result = PasswordEncryptionPolicy.instance().doPasswordsMatch(anotherPassword as char[], encryptedPassword)

    then:
    !result

    where:
    password << rawPasswords
    anotherPassword = "AUniquePassword123"
  }

  def "The encrypted password starts with the number of iterations"() {
    when:
    String encryptedPassword = PasswordEncryptionPolicy.instance().encrypt(password.toCharArray())
    String[] passwordElements = encryptedPassword.split(":")

    then:
    passwordElements[0].startsWith("10000") // number of iterations

    where:
    password << rawPasswords
  }

  def "The encrypted password contains a salt with a length of 20 bytes"() {
    when:
    String encryptedPassword = PasswordEncryptionPolicy.instance().encrypt(password.toCharArray())
    String[] passwordElements = encryptedPassword.split(":")

    then:
    passwordElements[1].length() == 40 //contains a salt with length of 20 bytes

    where:
    password << rawPasswords
  }

  def "The encrypted password ends with the hashed password"() {
    when:
    String encryptedPassword = PasswordEncryptionPolicy.instance().encrypt(password.toCharArray())
    String[] passwordElements = encryptedPassword.split(":")

    then:
    passwordElements[2] != password // contains a hash that is not the same as the clear text password

    where:
    password << rawPasswords
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
    passwordA << rawPasswords

    and:
    passwordB << rawPasswords
  }

  def "The encrypted password has a length of 256 bits independent of the raw password length"() {
    when:
    String encryptedPassword = PasswordEncryptionPolicy.instance().encrypt(password.toCharArray())

    then:
    encryptedPassword.length() == 111

    where:
    password << rawPasswords
  }
}
