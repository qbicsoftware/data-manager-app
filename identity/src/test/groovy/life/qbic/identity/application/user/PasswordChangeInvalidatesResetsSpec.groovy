package life.qbic.identity.application.user

import life.qbic.identity.domain.model.EmailAddress
import life.qbic.identity.domain.model.EncryptedPassword
import life.qbic.identity.domain.model.FullName
import life.qbic.identity.domain.model.User
import life.qbic.identity.domain.repository.UserDataStorage
import life.qbic.identity.domain.repository.UserRepository
import spock.lang.Specification

/**
 * <b>A completed password change invalidates outstanding reset links (#1674, AUTH-R-05).</b>
 *
 * <p>Without this, a user who requests two resets and uses the second leaves the first still valid,
 * so an attacker holding the older link can still take the account over. The token store is
 * superseded on every successful change.</p>
 */
class PasswordChangeInvalidatesResetsSpec extends Specification {

    def "a successful password change supersedes the account's pending reset tokens"() {
        given:
        def user = User.create(FullName.from("Test User"), EmailAddress.from("test@example.com"),
                "testuser", EncryptedPassword.from("anOldPassw0rd!".toCharArray()))
        // UserRepository is a concrete class over a UserDataStorage port; stub the port, as
        // IdentityServiceUserNameSpec does.
        def storage = Stub(UserDataStorage)
        storage.findUserById(_) >> Optional.of(user)
        def repository = new UserRepository(storage)
        def invalidated = []
        def service = new IdentityService(repository, { String userId -> invalidated << userId })

        when:
        def result = service.newUserPassword(user.id().get(), "aNewPassw0rd!".toCharArray())

        then: "the change succeeded"
        result.isValue()

        and: "every outstanding reset token for that account was superseded"
        invalidated == [user.id().get()]
    }

    def "a rejected password change does not invalidate anything"() {
        given:
        def user = User.create(FullName.from("Test User"), EmailAddress.from("test@example.com"),
                "testuser", EncryptedPassword.from("anOldPassw0rd!".toCharArray()))
        // UserRepository is a concrete class over a UserDataStorage port; stub the port, as
        // IdentityServiceUserNameSpec does.
        def storage = Stub(UserDataStorage)
        storage.findUserById(_) >> Optional.of(user)
        def repository = new UserRepository(storage)
        def invalidated = []
        def service = new IdentityService(repository, { String userId -> invalidated << userId })

        when: "the new password is too weak"
        def result = service.newUserPassword(user.id().get(), "short".toCharArray())

        then:
        result.isError()

        and: "no link is burned, so the user can still use the one they hold"
        invalidated.isEmpty()
    }
}
