package life.qbic.identity.domain.usermanagement.registration


import life.qbic.identity.application.user.registration.RegisterUserOutput
import life.qbic.identity.application.user.registration.Registration
import life.qbic.identity.application.user.registration.UserRegistrationException
import life.qbic.identity.application.user.IdentityService
import life.qbic.identity.domain.registry.DomainRegistry
import life.qbic.identity.domain.model.EmailAddress
import life.qbic.identity.domain.model.EncryptedPassword
import life.qbic.identity.domain.model.FullName
import life.qbic.identity.domain.model.User
import life.qbic.identity.domain.model.UserId
import life.qbic.identity.domain.repository.UserDataStorage
import life.qbic.identity.domain.service.UserDomainService
import life.qbic.identity.domain.repository.UserRepository
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests for the registration use case
 *
 * @since 1.0.0
 */
class RegistrationSpec extends Specification {

    @Shared
    public IdentityService userRegistrationService

    @Shared
    public TestStorage testStorage

    def setupSpec() {
        testStorage = new TestStorage()
        DomainRegistry domainRegistry = DomainRegistry.instance()
        domainRegistry.registerService(new UserDomainService(UserRepository.getInstance(testStorage)))
        userRegistrationService = new IdentityService(UserRepository.getInstance(testStorage))
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
        def registration = new Registration(new IdentityService(UserRepository.getInstance(testStorage)))
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
        def registration = new Registration(new IdentityService(UserRepository.getInstance(Mock(UserDataStorage))))
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
