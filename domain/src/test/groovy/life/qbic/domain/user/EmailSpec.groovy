package life.qbic.domain.user

import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class EmailSpec extends Specification {

    def "When a invalid email address is provided, an InvalidEmailExcpetion is thrown"() {
        when:
        EmailAddress email = EmailAddress.from("invalid@emailAddress")

        then:
        thrown(EmailAddress.EmailValidationException)
    }

    def "When a valid email address is provided, create an instance of the object"() {
        when:
        EmailAddress email = EmailAddress.from("valid@emailAddress.com")

        then:
        noExceptionThrown()
        email.address().equals("valid@emailAddress.com")
    }
}
