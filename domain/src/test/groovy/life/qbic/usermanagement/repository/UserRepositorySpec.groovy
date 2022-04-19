package life.qbic.usermanagement.repository

import life.qbic.usermanagement.User
import spock.lang.Specification

/**
 * <b>Tests for the {@link UserRepository}</b>
 *
 * @since 1.0.0
 */
class UserRepositorySpec extends Specification {

    def "Given a repository that contains more than one entry with the same email, throw a runtime exception"() {
        given:
        UserDataStorage storage = Mock(UserDataStorage.class)
        storage.findUsersByEmail("_") >> [createDummyUser(), createDummyUser()]

        UserRepository repository = new UserRepository(storage)

        when:
        repository.findByEmail("my.example@example.com")

        then:
        thrown(RuntimeException)
    }

    def "Given a repository already contains a user, dont add the user twice"() {
        given:
        UserDataStorage storage = Mock(UserDataStorage.class)
        storage.findUserById(_ as String) >> Optional.of(createDummyUser())
        UserRepository repository = new UserRepository(storage)

        when:
        var result = repository.findById("123")
        var user = result.get()
        boolean hasUserBeenAdded = repository.addUser(user)

        then:
        !hasUserBeenAdded
    }

    static User createDummyUser() {
        return User.create("test1234", "Sven Svenson", "myexample@example.com")
    }

}
