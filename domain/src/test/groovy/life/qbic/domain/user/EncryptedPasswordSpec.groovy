package life.qbic.domain.user

import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class EncryptedPasswordSpec extends Specification {

    def "Given a weak password, throw a PasswordValidationException" () {
        when:
        EncryptedPassword.from(rawPassword.toCharArray())

        then:
        thrown(EncryptedPassword.PasswordValidationException)

        where:
        rawPassword << ["test", "1234", "", "toofew1"]
    }

    def "Given a password, that fulfills the minimal criteria, create an instance" () {
        when:
        def encryptedPassword = EncryptedPassword.from(rawPassword.toCharArray())

        then:
        encryptedPassword != null

        where:
        rawPassword << ["test1234", "1234!#234", "megastrongpassword"]
    }
}
