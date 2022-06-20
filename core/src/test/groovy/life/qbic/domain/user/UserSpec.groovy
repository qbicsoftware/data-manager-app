package life.qbic.domain.user

import life.qbic.identityaccess.domain.user.EmailAddress
import life.qbic.identityaccess.domain.user.EncryptedPassword
import life.qbic.identityaccess.domain.user.FullName
import life.qbic.identityaccess.domain.user.User
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>Tests for the {@link life.qbic.identityaccess.domain.user.User}</b>
 *
 * @since 1.0.0
 */
class UserSpec extends Specification {

    @Shared
    Set<String> generatedUserIds = new HashSet<>()

    @Unroll
    def "When a new user is created, a unique identifier is assigned to the user"() {
        when:
        User user = User.create(FullName.from("Test User"), EmailAddress.from("my.name@example.com"), EncryptedPassword.from("test1234".toCharArray()))

        then:
        !generatedUserIds.contains(user.id())
        generatedUserIds.add(user.id())

        where:
        run << (1..100)
    }

}
