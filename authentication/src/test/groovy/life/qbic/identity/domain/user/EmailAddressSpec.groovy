package life.qbic.identity.domain.user


import spock.lang.Specification

/**
 * <b>Tests for the {@link EmailAddress}</b>
 *
 * @since 1.0.0
 */
class EmailAddressSpec extends Specification {

    def "When a invalid email address is provided, an InvalidEmailException is thrown"() {
        when:
        EmailAddress emailAddress = EmailAddress.from("invalid@emailAddress")

        then:
        thrown(EmailAddress.EmailValidationException)
    }

    def "When a valid email address is provided, create an instance of the object"() {
        when:
        EmailAddress emailAddress = EmailAddress.from("valid@emailAddress.com")

        then:
        noExceptionThrown()
        emailAddress.get().equals("valid@emailAddress.com")
    }
}
