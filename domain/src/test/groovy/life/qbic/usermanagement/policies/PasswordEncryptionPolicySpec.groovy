package life.qbic.usermanagement.policies

import spock.lang.Specification

/**
 * <b>Tests for the {@link PasswordEncryptionPolicy}</b>
 *
 * @since 1.0.0
 */
class PasswordEncryptionPolicySpec extends Specification {

    def "The same password can be matched with the correct salt and iteration"() {
        when:
        String encryptedPassword = PasswordEncryptionPolicy.create().encrypt(password)
        boolean result = PasswordEncryptionPolicy.create().doPasswordsMatch(password as char[],encryptedPassword)

        then:
        result

        where:
        password = "12345678"
    }

    def "Two different passwords cannot be matched"() {
        when:
        String encryptedPassword = PasswordEncryptionPolicy.create().encrypt(password)
        boolean result = PasswordEncryptionPolicy.create().doPasswordsMatch(anotherPassword as char[],encryptedPassword)

        then:
        !result

        where:
        password = "12345678"
        anotherPassword = "abcdefghijkl"
    }

    def "The encrypted password contains the number of iterations, a salt and the hashed password"() {
        when:
        String encryptedPassword = PasswordEncryptionPolicy.create().encrypt(password)
        String[] passwordElements = encryptedPassword.split(":")

        then:
        passwordElements[0].startsWith("4242") // number of iterations
        passwordElements[1].length() == 40 //contains a salt with length of 20 bytes
        passwordElements[2].length() == 40 // contains a hash with length of 20 bytes

        where:
        password = "abcdefghihdeo"
    }

}
