package life.qbic.usermanagement

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 <b>Tests for the {@link User}</b>
 *
 * @since 1.0.0
 */
class UserSpec extends Specification {

    @Shared
    Set<String> generatedUserIds = new HashSet<>()

    @Unroll
    def "When a new user is created, a unique identifier is assigned to the user"() {
        when:
        User user = User.create("test1234", "My Name", "my.name@example.com")

        then:
        !generatedUserIds.contains(user.getId())
        generatedUserIds.add(user.getId())

        where:
        run << (1..10_000)
    }

    def "When a weak password is provided, throw a user exception"() {
        when:
        User.create("123", "My Name", "my.name@example.com")

        then:
        thrown(User.UserException)
    }

    def "When an invalid email is provided, throw a user exception"() {
        when:
        User.create("test1244", "My Name", "my.name@example")

        then:
        thrown(User.UserException)
    }

}
