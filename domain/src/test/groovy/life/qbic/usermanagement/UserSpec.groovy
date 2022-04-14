package life.qbic.usermanagement

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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
