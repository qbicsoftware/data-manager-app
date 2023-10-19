package life.qbic.identity.domain.user


import life.qbic.identity.domain.model.EncryptedPassword
import life.qbic.identity.domain.event.PasswordResetRequested
import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import life.qbic.identity.domain.model.EmailAddress
import life.qbic.identity.domain.model.FullName
import life.qbic.identity.domain.model.User
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

    @Unroll
    def "When a new user is created, a unique identifier is assigned to the user"() {
        when:
        User user = User.create(FullName.from("Test User"), EmailAddress.from("my.name@example.com"), EncryptedPassword.from("test1234".toCharArray()))

        then:
        !generatedUserIds.contains(user.id())
        generatedUserIds.add(user.id())

        where:
        run << (1..100)
    }

    def "When a password reset is requested, a password reset domain event is published"() {
        given:
        User user = User.create(FullName.from("Test User"), EmailAddress.from("my.name@example.com"), EncryptedPassword.from("test1234".toCharArray()))

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

}
