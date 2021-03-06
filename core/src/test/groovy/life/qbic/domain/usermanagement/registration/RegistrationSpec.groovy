package life.qbic.domain.usermanagement.registration

import life.qbic.identityaccess.application.user.RegisterUserOutput
import life.qbic.identityaccess.application.user.Registration
import life.qbic.identityaccess.application.user.UserRegistrationException
import life.qbic.identityaccess.application.user.UserRegistrationService
import life.qbic.identityaccess.domain.DomainRegistry
import life.qbic.identityaccess.domain.user.*
import life.qbic.shared.application.notification.EventStore
import life.qbic.shared.application.notification.MessageBusInterface
import life.qbic.shared.application.notification.NotificationService
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
        userRegistrationService = new UserRegistrationService(new NotificationService(Mock(MessageBusInterface)), UserRepository.getInstance(testStorage), Mock(EventStore.class))
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
        def registration = new Registration(new UserRegistrationService(new NotificationService(Mock(MessageBusInterface)), UserRepository.getInstance(testStorage), Mock(EventStore.class)))
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
        def registration = new Registration(new UserRegistrationService(new NotificationService(Mock(MessageBusInterface)), UserRepository.getInstance(Mock(UserDataStorage)), Mock(EventStore.class)))
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
                    .filter((User user) -> { user.emailAddress().equals( email) }).collect()
        }

        @Override
        void save(User user) {
            users.add(user)
        }

        @Override
        Optional<User> findUserById(UserId id) {
            return users.stream().filter(user -> user.id() == id).findAny()
        }
    }

}
