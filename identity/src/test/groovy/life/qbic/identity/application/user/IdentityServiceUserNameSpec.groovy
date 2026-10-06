package life.qbic.identity.application.user

import life.qbic.identity.api.UserNames
import life.qbic.identity.domain.model.EmailAddress
import life.qbic.identity.domain.model.EncryptedPassword
import life.qbic.identity.domain.model.FullName
import life.qbic.identity.domain.model.User
import life.qbic.identity.domain.model.UserId
import life.qbic.identity.domain.registry.DomainRegistry
import life.qbic.identity.domain.repository.UserDataStorage
import life.qbic.identity.domain.repository.UserRepository
import life.qbic.identity.domain.service.UserDomainService
import spock.lang.Specification

/**
 * <b>Tests for the username length rules enforced by the {@link IdentityService}</b>
 *
 * @since 1.20.0
 */
class IdentityServiceUserNameSpec extends Specification {

    private static final String VALID_PASSWORD = "0123456789012"

    private UserDataStorage storage
    private IdentityService service

    def setup() {
        storage = Mock(UserDataStorage)
        def repository = new UserRepository(storage)
        DomainRegistry.instance().registerService(new UserDomainService(repository))
        service = new IdentityService(repository)
    }

    // ── Username change ───────────────────────────────────────────

    def "changing to a username longer than the limit is rejected"() {
        given:
        def user = userWithName("jdoe")
        stubStorage(user)

        when:
        def response = service.requestUserNameChange(user.id().get(),
                "a" * (UserNames.MAX_LENGTH + 1))

        then:
        !response.isSuccess()
        response.failures().first() instanceof IdentityService.UserNameTooLongException

        and: "nothing was persisted"
        0 * storage.save(_)
    }

    def "changing to a username within the limit succeeds"() {
        given:
        def user = userWithName("jdoe")
        stubStorage(user)

        when:
        def response = service.requestUserNameChange(user.id().get(), "a" * UserNames.MAX_LENGTH)

        then:
        response.isSuccess()
        1 * storage.save(user)
        user.userName() == "a" * UserNames.MAX_LENGTH
    }

    def "a grandfathered over-long username can be resubmitted unchanged"() {
        given: "a user whose persisted username predates the length limit"
        def user = grandfatheredUserWithName("a-very-long-legacy-username")
        stubStorage(user)

        when: "the user saves their profile without changing the name"
        def response = service.requestUserNameChange(user.id().get(), user.userName())

        then: "the change is treated as a no-op success rather than a rejection"
        response.isSuccess()
        0 * storage.save(_)
    }

    def "a grandfathered over-long username may be resubmitted with surrounding whitespace"() {
        given:
        def user = grandfatheredUserWithName("a-very-long-legacy-username")
        stubStorage(user)

        when: "the trimmed value still equals the persisted one"
        def response = service.requestUserNameChange(user.id().get(), "  " + user.userName() + "  ")

        then:
        response.isSuccess()
        0 * storage.save(_)
    }

    def "a grandfathered over-long username must be shortened when changed to a different value"() {
        given:
        def user = grandfatheredUserWithName("a-very-long-legacy-username")
        stubStorage(user)

        when: "the user switches to another over-long value"
        def response = service.requestUserNameChange(user.id().get(),
                "a" * (UserNames.MAX_LENGTH + 1))

        then:
        !response.isSuccess()
        response.failures().first() instanceof IdentityService.UserNameTooLongException
        0 * storage.save(_)
    }

    def "surrounding whitespace is stripped before persisting the username"() {
        given:
        def user = userWithName("jdoe")
        stubStorage(user)

        when:
        def response = service.requestUserNameChange(user.id().get(), "  jane  ")

        then:
        response.isSuccess()
        user.userName() == "jane"
    }

    // ── Registration ──────────────────────────────────────────────

    def "registering a new user with an over-long username is rejected"() {
        given:
        stubStorage()

        when:
        def response = service.registerUser("Test User", "a" * (UserNames.MAX_LENGTH + 1),
                "new.user@example.com", VALID_PASSWORD.toCharArray())

        then:
        !response.isSuccess()
        response.failures().any { it instanceof IdentityService.UserNameTooLongException }

        and: "no user was persisted"
        0 * storage.save(_)
    }

    def "registering a new user with a username at the limit succeeds"() {
        given:
        stubStorage()

        when:
        def response = service.registerUser("Test User", "a" * UserNames.MAX_LENGTH,
                "new.user@example.com", VALID_PASSWORD.toCharArray())

        then:
        response.isSuccess()
        1 * storage.save(_)
    }

    // ── Helpers ───────────────────────────────────────────────────

    /**
     * Stubs the storage so that the given user (if any) is the only entry found, and all
     * Optional-returning lookups otherwise miss. Declared per feature because interactions
     * declared in {@code setup()} take precedence over feature-level interactions.
     */
    private void stubStorage(User existing = null) {
        storage.findUserById(_ as UserId) >>
                (existing == null ? Optional.empty() : Optional.of(existing))
        storage.findUserByUserName(_ as String) >> Optional.empty()
        storage.findUsersByEmailAddress(_ as EmailAddress) >> []
    }

    private static User userWithName(String userName) {
        return User.create(FullName.from("Test User"),
                EmailAddress.from("my.name@example.com"), userName,
                EncryptedPassword.from(VALID_PASSWORD.toCharArray()))
    }

    /**
     * Builds a user carrying a persisted, over-long username by materialising the entity the way
     * Hibernate would: no-arg constructor plus direct field assignment. The validating factory
     * method legitimately rejects such values for <em>new</em> users, but legacy rows must still
     * be loadable.
     */
    private static User grandfatheredUserWithName(String userName) {
        def constructor = User.getDeclaredConstructor()
        constructor.setAccessible(true)
        def user = constructor.newInstance()
        def userNameField = User.getDeclaredField("userName")
        userNameField.setAccessible(true)
        userNameField.set(user, userName)
        def idField = User.getDeclaredField("id")
        idField.setAccessible(true)
        idField.set(user, UserId.create())
        return user
    }
}
