package life.qbic.usermanagement.registration

import life.qbic.usermanagement.User
import life.qbic.usermanagement.repository.UserDataStorage
import life.qbic.usermanagement.repository.UserRepository
import spock.lang.Specification

/**
 * Tests for the registration use case
 *
 * @since 1.0.0
 */
class RegistrationSpec extends Specification {

    def "When a user is already registered with a given email address, abort the registration and communicate the failure"() {
        given: "A repository with one user entry"
        def userDataStorage = new TestStorage()
        def testUser = User.create("12345678", "Mr Somebody", "some@body.com")
        userDataStorage.save(testUser)
        def userRepo = UserRepository.getInstance(userDataStorage)

        and:
        def useCaseOutput = Mock(RegisterUserOutput.class)

        and: "a new user to register"
        def newUser = User.create("12345678", "Mr Nobody", "some@body.com")

        and: "a the use case with output"
        def registration = new Registration(userRepo)
        registration.setOutput(useCaseOutput)

        when: "a user is registered"
        registration.register(newUser)

        then:
        0 * useCaseOutput.onSuccess()
        1 * useCaseOutput.onFailure(_ as String)
        // the user has not been added to the repository
        userDataStorage.findUserById(newUser.getId()).isEmpty()
    }

    def "When a user is not yet registered with a given email address, register the user"() {
        given: "A repository with one user entry"
        def userDataStorage = new TestStorage()
        def testUser = User.create("12345678", "Mr Somebody", "some@body.com")
        userDataStorage.save(testUser)
        def userRepo = UserRepository.getInstance(userDataStorage)

        and:
        def useCaseOutput = Mock(RegisterUserOutput.class)

        and: "a new user to register"
        def newUser = User.create("12345678", "Mr Nobody", "no@body.com")

        and: "a the use case with output"
        def registration = new Registration(userRepo)
        registration.setOutput(useCaseOutput)

        when: "a user is registered"
        registration.register(newUser)

        then:
        1 * useCaseOutput.onSuccess()
        0 * useCaseOutput.onFailure(_ as String)
        userDataStorage.findUserById(newUser.getId()).isPresent()

    }

    private static class TestStorage implements UserDataStorage {

        private List<User> users = []

        @Override
        List<User> findUsersByEmail(String email) {
            return users.stream()
                    .filter((User user) -> { user.getEmail().equals(email) }).collect()
        }

        @Override
        void save(User user) {
            users.add(user)
        }

        @Override
        Optional<User> findUserById(String id) {
            return users.stream().filter( user -> user.id.equalsIgnoreCase(id)).findAny()
        }
    }

}
