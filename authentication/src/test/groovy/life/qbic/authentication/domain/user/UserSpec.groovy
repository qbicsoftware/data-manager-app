package life.qbic.authentication.domain.user

import life.qbic.authentication.domain.user.concept.EmailAddress
import life.qbic.authentication.domain.user.concept.EncryptedPassword
import life.qbic.authentication.domain.user.concept.FullName
import life.qbic.authentication.domain.user.concept.User
import life.qbic.authentication.domain.user.event.PasswordReset
import life.qbic.domain.concepts.DomainEventPublisher
import life.qbic.domain.concepts.DomainEventSubscriber
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>Tests for the {@link life.qbic.authentication.domain.user.concept.User}</b>
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
        DomainEventPublisher publisher = DomainEventPublisher.instance()
        publisher.subscribe(new DomainEventSubscriber<PasswordReset>() {
            @Override
            Class<PasswordReset> subscribedToEventType() {
                return PasswordReset.class
            }

            @Override
            void handleEvent(PasswordReset event) {
                domainEventPublished = true
            }
        })

        when:
        user.resetPassword()

        then:
        domainEventPublished
    }

}
