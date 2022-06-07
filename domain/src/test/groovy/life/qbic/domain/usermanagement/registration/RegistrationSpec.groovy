package life.qbic.domain.usermanagement.registration

import life.qbic.apps.datamanager.events.EventStore
import life.qbic.apps.datamanager.notifications.MessageBusInterface
import life.qbic.apps.datamanager.notifications.NotificationService
import life.qbic.apps.datamanager.services.UserRegistrationService
import life.qbic.domain.user.Email
import life.qbic.domain.user.EncryptedPassword
import life.qbic.domain.user.FullName
import life.qbic.domain.usermanagement.DomainRegistry
import life.qbic.domain.user.User
import life.qbic.domain.user.UserDomainService
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
        userRegistrationService = new UserRegistrationService(new NotificationService(Mock(MessageBusInterface)), Mock(EventStore.class))
    }

    def "When a user is already registered with a given email address, abort the registration and communicate the failure"() {
        given: "A repository with one user entry"
        def testUser = User.create(FullName.from("Mr Somebody"), Email.from("some@body.com"), EncryptedPassword.from("test1234".toCharArray()))
        testStorage.save(testUser)

        and:
        def useCaseOutput = Mock(RegisterUserOutput.class)

        and: "a new user to register"
        def newUser = User.create(FullName.from("Mr Somebody"), Email.from("some@body.com"), EncryptedPassword.from("test1234".toCharArray()))

        and: "a the use case with output"
        def registration = new Registration(new UserRegistrationService(new NotificationService(Mock(MessageBusInterface)), Mock(EventStore.class)))
        registration.setOutput(useCaseOutput)

        when: "a user is registered"
        registration.register(testUser.fullName.name(), testUser.email.address(), "12345678".toCharArray())

        then:
        0 * useCaseOutput.onSuccess()
        1 * useCaseOutput.onFailure(_ as String)
        // the user has not been added to the repository
        testStorage.findUsersByEmail(newUser.email.address()).size() == 1
    }

    def "When a user is not yet registered with a given email address, register the user"() {
        given: "A repository with one user entry"
        def testUser = User.create(FullName.from("Mr Somebody"), Email.from("some@body.com"), EncryptedPassword.from("test1234".toCharArray()))
        testStorage.save(testUser)

        and:
        def useCaseOutput = Mock(RegisterUserOutput.class)

        and: "a new user to register"
        def newUser = User.create(FullName.from("Mr Nobody"), Email.from("no@body.com"), EncryptedPassword.from("test1234".toCharArray()))

        and: "a the use case with output"
        def registration = new Registration(new UserRegistrationService(new NotificationService(Mock(MessageBusInterface)), Mock(EventStore.class)))
        registration.setOutput(useCaseOutput)

        when: "a user is registered"
        registration.register(newUser.fullName.name(), newUser.email.address(), "12345678".toCharArray())

        then:
        1 * useCaseOutput.onSuccess()
        0 * useCaseOutput.onFailure(_ as String)
        testStorage.findUsersByEmail(newUser.email.address()).get(0).fullName == newUser.fullName

    }

    private static class TestStorage implements UserDataStorage {

        private List<User> users = []

        @Override
        List<User> findUsersByEmail(String email) {
            return users.stream()
                    .filter((User user) -> { user.getEmail().address().equals(email) }).collect()
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
