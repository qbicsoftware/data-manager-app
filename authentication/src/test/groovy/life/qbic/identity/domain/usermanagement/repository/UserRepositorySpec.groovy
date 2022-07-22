package life.qbic.identity.domain.usermanagement.repository


import life.qbic.identity.domain.user.concept.EmailAddress
import life.qbic.identity.domain.user.concept.EncryptedPassword
import life.qbic.identity.domain.user.concept.FullName
import life.qbic.identity.domain.user.concept.User
import life.qbic.identity.domain.user.concept.UserId
import life.qbic.identity.domain.user.repository.UserDataStorage
import life.qbic.identity.domain.user.repository.UserRepository
import spock.lang.Specification

/**
 * <b>Tests for the {@link life.qbic.identity.domain.user.repository.UserRepository}</b>
 *
 * @since 1.0.0
 */
class UserRepositorySpec extends Specification {

    def "Given a repository that contains more than one entry with the same email, throw a runtime exception"() {
        given:
        UserDataStorage storage = Mock(UserDataStorage.class)
        storage.findUsersByEmailAddress(_ as EmailAddress) >> [createDummyUser(), createDummyUser()]

        UserRepository repository = new UserRepository(storage)

        when:
        repository.findByEmail(EmailAddress.from("my.example@example.com"))

        then:
        thrown(RuntimeException)
    }

    def "Given a repository that contains a user with a given email, return the user"() {
        given:
        UserDataStorage storage = Mock(UserDataStorage.class)
        def user = createDummyUser()
        storage.findUsersByEmailAddress(_ as EmailAddress) >> [user]
        UserRepository repository = new UserRepository(storage)

        when:
        def matchingUser = repository.findByEmail(EmailAddress.from("my.example@example.com"))

        then:
        matchingUser.get().id() == user.id()
    }

    def "Given a repository that contains no user with a given email, return an empty result"() {
        given:
        UserDataStorage storage = Mock(UserDataStorage.class)
        def user = createDummyUser()
        storage.findUsersByEmailAddress(_ as EmailAddress) >> []
        UserRepository repository = new UserRepository(storage)

        when:
        def matchingUser = repository.findByEmail(EmailAddress.from("not.existing@example.com"))

        then:
        matchingUser.isEmpty()
    }

    def "Given a repository already contains a user, dont add the user twice"() {
        given:
        UserDataStorage storage = Mock(UserDataStorage.class)
        storage.findUserById(_ as UserId) >> Optional.of(createDummyUser())
        UserRepository repository = new UserRepository(storage)

        when:
        var result = repository.findById(UserId.create())
        var user = result.get()
        repository.addUser(user)

        then:
        thrown(UserRepository.UserStorageException)
    }


    static User createDummyUser() {
        def user = User.create(FullName.from("Test User"), EmailAddress.from("my.name@example.com"), EncryptedPassword.from("test1234".toCharArray()))
        return user
    }

}
