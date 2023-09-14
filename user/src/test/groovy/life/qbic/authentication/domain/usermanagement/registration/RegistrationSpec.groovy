package life.qbic.authentication.domain.usermanagement.registration


import life.qbic.authentication.application.user.registration.RegisterUserOutput
import life.qbic.authentication.application.user.registration.Registration
import life.qbic.authentication.application.user.registration.UserRegistrationException
import life.qbic.authentication.application.user.registration.UserRegistrationService
import life.qbic.authentication.domain.registry.DomainRegistry
import life.qbic.authentication.domain.user.concept.*
import life.qbic.authentication.domain.user.repository.UserDataStorage
import life.qbic.authentication.domain.user.repository.UserDomainService
import life.qbic.authentication.domain.user.repository.UserRepository
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests for the registration use case
 *
 * @since 1.0.0
 */
class RegistrationSpec extends Specification {

    @Shared
    public UserRegistrationService userRegistrationService

    @Shared
    public TestStorage testStorage

    def setupSpec() {
        testStorage = new TestStorage()
        DomainRegistry domainRegistry = DomainRegistry.instance()
        domainRegistry.registerService(new UserDomainService(UserRepository.getInstance(testStorage)))
        userRegistrationService = new UserRegistrationService(UserRepository.getInstance(testStorage))
    }

    def "When a user is already registered with a given email address, abort the registration and communicate the failure"() {
        given: "A repository with one user entry"
        def testUser = User.create(FullName.from("Mr Somebody"), EmailAddress.from("some@body.com"), EncryptedPassword.from("test1234".toCharArray()))
        testStorage.save(testUser)

        and:
        def useCaseOutput = Mock(RegisterUserOutput.class)

        and: "a new user to register"
        def newUser = User.create(FullName.from("Mr Somebody"), EmailAddress.from("some@body.com"), EncryptedPassword.from("test1234".toCharArray()))

        and: "a the use case with output"
        def registration = new Registration(new UserRegistrationService(UserRepository.getInstance(testStorage)))
        registration.setOutput(useCaseOutput)

        when: "a user is registered"
        registration.register(newUser.fullName().get(), newUser.emailAddress().get(), "12345678".toCharArray())

        then:
        0 * useCaseOutput.onUserRegistrationSucceeded()
        1 * useCaseOutput.onUnexpectedFailure(_ as UserRegistrationException)
        // the user has not been added to the repository
        testStorage.findUsersByEmailAddress(testUser.emailAddress()).size() == 1
    }

    def "When a user is not yet registered with a given email address, register the user"() {
        given: "A repository with one user entry"
        def testUser = User.create(FullName.from("Mr Somebody"), EmailAddress.from("some@body.com"), EncryptedPassword.from("test1234".toCharArray()))
        testStorage.save(testUser)

        and:
        def useCaseOutput = Mock(RegisterUserOutput.class)

        and: "a new user to register"
        def newUser = User.create(FullName.from("Mr Nobody"), EmailAddress.from("no@body.com"), EncryptedPassword.from("test1234".toCharArray()))

        and: "a the use case with output"
        def registration = new Registration(new UserRegistrationService(UserRepository.getInstance(Mock(UserDataStorage))))
        registration.setOutput(useCaseOutput)

        when: "a user is registered"
        registration.register(newUser.fullName().get(), newUser.emailAddress().get(), "12345678".toCharArray())

        then:
        1 * useCaseOutput.onUserRegistrationSucceeded()
        0 * useCaseOutput.onUnexpectedFailure(_ as String)
        0 * useCaseOutput.onUnexpectedFailure(_ as UserRegistrationException)
        def storedUser = testStorage.findUsersByEmailAddress(newUser.emailAddress()).get(0)
        storedUser.fullName() == newUser.fullName()
        !storedUser.id().get().isBlank()

    }

    private static class TestStorage implements UserDataStorage {

        private List<User> users = []

        @Override
        List<User> findUsersByEmailAddress(EmailAddress email) {
            return users.stream()
                    .filter((User user) -> { user.emailAddress().equals(email) }).collect()
        }

        @Override
        void save(User user) {
            users.add(user)
        }

        @Override
        Optional<User> findUserById(UserId id) {
            return users.stream().filter(user -> user.id() == id).findAny()
        }

        @Override
        List<User> findAllActiveUsers() {
            return users.findAll { it.active }
        }
    }

}
