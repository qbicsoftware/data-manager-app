package life.qbic.domain.usermanagement.registration

import life.qbic.apps.datamanager.events.EventStore
import life.qbic.apps.datamanager.notifications.MessageBusInterface
import life.qbic.apps.datamanager.notifications.NotificationService
import life.qbic.apps.datamanager.services.UserRegistrationException
import life.qbic.apps.datamanager.services.UserRegistrationService
import life.qbic.domain.user.*
import life.qbic.domain.usermanagement.DomainRegistry
import life.qbic.domain.usermanagement.repository.UserDataStorage
import life.qbic.domain.usermanagement.repository.UserRepository
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
        userRegistrationService = new UserRegistrationService(new NotificationService(Mock(MessageBusInterface)), UserRepository.getInstance(Mock(UserDataStorage)), Mock(EventStore.class))
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
        def registration = new Registration(new UserRegistrationService(new NotificationService(Mock(MessageBusInterface)), UserRepository.getInstance(Mock(UserDataStorage)), Mock(EventStore.class)))
        registration.setOutput(useCaseOutput)

        when: "a user is registered"
        registration.register(newUser.fullName.value(), newUser.email.value(), "12345678".toCharArray())

        then:
        0 * useCaseOutput.onUserRegistrationSucceeded()
        1 * useCaseOutput.onUnexpectedFailure(_ as UserRegistrationException)
        // the user has not been added to the repository
        testStorage.findUsersByEmailAddress(newUser.email).size() == 1
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
        registration.register(newUser.fullName.value(), newUser.email.value(), "12345678".toCharArray())

        then:
        1 * useCaseOutput.onUserRegistrationSucceeded()
        0 * useCaseOutput.onUnexpectedFailure(_ as String)
        0 * useCaseOutput.onUnexpectedFailure(_ as UserRegistrationException)
        def storedUser = testStorage.findUsersByEmailAddress(newUser.getEmail()).get(0)
        storedUser.getFullName() == newUser.getFullName()
        !storedUser.getId().isBlank()

    }

    private static class TestStorage implements UserDataStorage {

        private List<User> users = []

        @Override
        List<User> findUsersByEmailAddress(EmailAddress email) {
            return users.stream()
                    .filter((User user) -> { user.getEmail().equals( email) }).collect()
        }

        @Override
        void save(User user) {
            users.add(user)
        }

        @Override
        Optional<User> findUserById(String id) {
            return users.stream().filter(user -> user.id.equalsIgnoreCase(id)).findAny()
        }
    }

}
