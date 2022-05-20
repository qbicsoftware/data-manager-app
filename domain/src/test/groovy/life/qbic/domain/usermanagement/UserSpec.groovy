package life.qbic.domain.usermanagement


import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>Tests for the {@link life.qbic.domain.usermanagement.User}</b>
 *
 * @since 1.0.0
 */
class UserSpec extends Specification {

    @Shared
    Set<String> generatedUserIds = new HashSet<>()

    @Unroll
    def "When a new user is created, a unique identifier is assigned to the user"() {
        when:
        User user = User.create("My Name", "my.name@example.com")
        user.setPassword("test1234".toCharArray())

        then:
        !generatedUserIds.contains(user.getId())
        generatedUserIds.add(user.getId())

        where:
        run << (1..100)
    }

    def "When a weak password is provided, throw a user exception"() {
        when:
        def user = User.create("My Name", "my.name@example.com")
        def weakPassword = "ab";
        user.setPassword(weakPassword.toCharArray())

        then:
        thrown(User.UserException)
    }


    def "When an invalid email is provided, throw a user exception"() {
        when:
        User.create("My Name", "invalid@email")

        then:
        thrown(User.UserException)
    }

}
