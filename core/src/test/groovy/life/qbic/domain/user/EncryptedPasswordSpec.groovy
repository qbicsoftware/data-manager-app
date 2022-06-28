package life.qbic.domain.user

import life.qbic.identityaccess.domain.user.EncryptedPassword
import spock.lang.Specification

/**
 * <b>Tests for the {@link life.qbic.identityaccess.domain.user.EncryptedPassword}</b>
 *
 * @since 1.0.0
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
