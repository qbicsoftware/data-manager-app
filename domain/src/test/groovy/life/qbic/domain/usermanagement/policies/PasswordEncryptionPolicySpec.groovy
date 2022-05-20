package life.qbic.domain.usermanagement.policies


import spock.lang.Specification

/**
 * <b>Tests for the {@link PasswordEncryptionPolicy}</b>
 *
 * @since 1.0.0
 */
class PasswordEncryptionPolicySpec extends Specification {

    def "The password is not stored in clear text"() {
        when:
        String encryptedPassword = PasswordEncryptionPolicy.create().encrypt(password.toCharArray())

        then:
        encryptedPassword != password

        where:
        password = "12345678"
    }

    def "The password policy matches same passwords"() {
        when:
        String encryptedPassword = PasswordEncryptionPolicy.create().encrypt(password.toCharArray())
        boolean result = PasswordEncryptionPolicy.create().doPasswordsMatch(password as char[],encryptedPassword)

        then:
        result

        where:
        password = "12345678"
    }

    def "Two different passwords cannot be matched"() {
        when:
        String encryptedPassword = PasswordEncryptionPolicy.create().encrypt(password.toCharArray())
        boolean result = PasswordEncryptionPolicy.create().doPasswordsMatch(anotherPassword as char[],encryptedPassword)

        then:
        !result

        where:
        password = "12345678"
        anotherPassword = "abcdefghijkl"
    }

    def "The encrypted password starts with the number of iterations"() {
        when:
        String encryptedPassword = PasswordEncryptionPolicy.create().encrypt(password.toCharArray())
        String[] passwordElements = encryptedPassword.split(":")

        then:
        passwordElements[0].startsWith("10000") // number of iterations

        where:
        password = "abcdefghihdeo"
    }

    def "The encrypted password contains a salt"() {
        when:
        String encryptedPassword = PasswordEncryptionPolicy.create().encrypt(password.toCharArray())
        String[] passwordElements = encryptedPassword.split(":")

        then:
        passwordElements[1].length() == 40 //contains a salt with length of 20 bytes

        where:
        password = "abcdefghihdeo"
    }

    def "The encrypted password ends with the hashed password"() {
        when:
        String encryptedPassword = PasswordEncryptionPolicy.create().encrypt(password.toCharArray())
        String[] passwordElements = encryptedPassword.split(":")

        then:
        passwordElements[2] != password // contains a hash that is not the same as the clear text password

        where:
        password = "abcdefghihdeo"
    }

}
