package life.qbic.identity.domain.user

import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import life.qbic.identity.domain.event.PasswordResetRequested
import life.qbic.identity.domain.model.EmailAddress
import life.qbic.identity.domain.model.EncryptedPassword
import life.qbic.identity.domain.model.FullName
import life.qbic.identity.domain.model.User
import life.qbic.identity.api.UserNames
import life.qbic.identity.domain.model.UserNamePolicy
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>Tests for the {@link User}</b>
 *
 * @since 1.0.0
 */
class UserSpec extends Specification {

    @Shared
    Set<String> generatedUserIds = new HashSet<>()
    private String validPassword = "test123456789012"

    @Unroll
    def "When a new user is created, a unique identifier is assigned to the user"() {
        when:
        User user = User.create(FullName.from("Test User"), EmailAddress.from("my.name@example.com"), "svenipopenni", EncryptedPassword.from(validPassword.toCharArray()))

        then:
        !generatedUserIds.contains(user.id())
        generatedUserIds.add(user.id())

        where:
        run << (1..100)
    }

    def "When a password reset is requested, a password reset domain event is published"() {
        given:
        User user = User.create(FullName.from("Test User"), EmailAddress.from("my.name@example.com"), "svenipopenni", EncryptedPassword.from(validPassword.toCharArray()))

        and:
        boolean domainEventPublished = false

        and:
        DomainEventDispatcher publisher = DomainEventDispatcher.instance()
        publisher.subscribe(new DomainEventSubscriber<PasswordResetRequested>() {
            @Override
            Class<PasswordResetRequested> subscribedToEventType() {
                return PasswordResetRequested.class
            }

            @Override
            void handleEvent(PasswordResetRequested event) {
                domainEventPublished = true
            }
        })

        when:
        user.resetPassword()

        then:
        domainEventPublished
    }

    def "Given a persisted username longer than the limit, the user can still be rehydrated from storage"() {
        given: "a username that predates the length limit"
        def legacyUserName = "a-very-long-legacy-username"

        and: "a user instance as Hibernate would materialise it: via the no-arg constructor and " +
             "direct field assignment, bypassing the validating factory method"
        def constructor = User.getDeclaredConstructor()
        constructor.setAccessible(true)
        def user = constructor.newInstance()
        setField(user, "userName", legacyUserName)

        when: "the persisted value is read"
        def rehydratedUserName = user.userName()

        then: "reading must never throw — otherwise every query loading that user would fail"
        noExceptionThrown()
        rehydratedUserName == legacyUserName
    }

    static void setField(Object target, String name, Object value) {
        def field = User.getDeclaredField(name)
        field.setAccessible(true)
        field.set(target, value)
    }

    def "Given a new username longer than the limit, creating a user fails fast"() {
        when: "a user is created with a username past the limit"
        User.create(FullName.from("Test User"), EmailAddress.from("my.name@example.com"),
                "a" * (UserNames.MAX_LENGTH + 1),
                EncryptedPassword.from(validPassword.toCharArray()))

        then: "the aggregate rejects the value"
        thrown(UserNamePolicy.UserNameValidationException)
    }

    def "Given a new username longer than the limit, changing the username fails fast"() {
        given:
        def user = User.create(FullName.from("Test User"),
                EmailAddress.from("my.name@example.com"), "jdoe",
                EncryptedPassword.from(validPassword.toCharArray()))

        when:
        user.setNewUserName("a" * (UserNames.MAX_LENGTH + 1))

        then:
        thrown(UserNamePolicy.UserNameValidationException)

        and: "the previous username is left untouched"
        user.userName() == "jdoe"
    }

}
