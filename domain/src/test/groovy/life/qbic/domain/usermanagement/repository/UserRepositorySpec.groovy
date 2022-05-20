package life.qbic.usermanagement.repository

import life.qbic.domain.usermanagement.User
import spock.lang.Specification

/**
 * <b>Tests for the {@link life.qbic.domain.usermanagement.repository.UserRepository}</b>
 *
 * @since 1.0.0
 */
class UserRepositorySpec extends Specification {

    def "Given a repository that contains more than one entry with the same email, throw a runtime exception"() {
        given:
        UserDataStorage storage = Mock(UserDataStorage.class)
        storage.findUsersByEmail(_ as String) >> [createDummyUser(), createDummyUser()]

        UserRepository repository = new UserRepository(storage)

        when:
        repository.findByEmail("my.example@example.com")

        then:
        thrown(RuntimeException)
    }

    def "Given a repository that contains a user with a given email, return the user"() {
        given:
        UserDataStorage storage = Mock(UserDataStorage.class)
        def user = createDummyUser()
        storage.findUsersByEmail(_ as String) >> [user]
        UserRepository repository = new UserRepository(storage)

        when:
        def matchingUser = repository.findByEmail("my.example@example.com")

        then:
        matchingUser.get().getId().equalsIgnoreCase(user.getId())
    }

    def "Given a repository that contains no user with a given email, return an empty result"() {
        given:
        UserDataStorage storage = Mock(UserDataStorage.class)
        def user = createDummyUser()
        storage.findUsersByEmail(_ as String) >> []
        UserRepository repository = new UserRepository(storage)

        when:
        def matchingUser = repository.findByEmail("not.existing@example.com")

        then:
        matchingUser.isEmpty()
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
        def user = User.create("test1234", "Sven Svenson", "myexample@example.com")
        user.setId(new Random().nextInt().toString())
        return user
    }

}
