package life.qbic.datamanager.signposting.http.validation

import life.qbic.datamanager.signposting.http.Validator
import life.qbic.datamanager.signposting.http.WebLink
import life.qbic.datamanager.signposting.http.parsing.RawLink
import life.qbic.datamanager.signposting.http.parsing.RawLinkHeader
import life.qbic.datamanager.signposting.http.parsing.RawParam
import spock.lang.Specification

/**
 * Specification for {@link Rfc8288Validator}.
 *
 * Covers basic RFC 8288 semantics:
 * <ul>
 *   <li>Valid URIs create {@link WebLink} instances without issues.</li>
 *   <li>Invalid URIs create error {@link Validator.Issue}s and no WebLink for that entry.</li>
 *   <li>Multiple links are all validated; one invalid URI does not stop validation.</li>
 *   <li>Unknown / extension parameters are preserved and do not cause issues.</li>
 * </ul>
 *
 * @since <version tag>
 */
class Rfc8288ValidatorSpec extends Specification {

    /**
     * Valid single link with a syntactically correct absolute URI
     * should yield one WebLink and no issues.
     */
    def "single valid link produces one WebLink and no issues"() {
        given:
        def rawHeader = new RawLinkHeader([
                new RawLink("https://example.org/resource", [])
        ])

        and:
        def validator = new Rfc8288Validator()

        when:
        Validator.ValidationResult result = validator.validate(rawHeader)

        then: "no issues are reported"
        !result.containsIssues()
        !result.report().hasErrors()
        !result.report().hasWarnings()

        and: "exactly one WebLink is produced with the expected URI and empty params"
        result.weblinks().size() == 1
        WebLink link = result.weblinks().first()
        link.reference().toString() == "https://example.org/resource"
        link.params().isEmpty()
    }

    /**
     * A link with an invalid URI string should not yield a WebLink instance,
     * but should record at least one error Issue.
     */
    def "single invalid URI produces error issue and no WebLinks"() {
        given:
        // 'not a uri' will fail URI.create(...)
        def rawHeader = new RawLinkHeader([
                new RawLink("not a uri", [])
        ])

        and:
        def validator = new Rfc8288Validator()

        when:
        Validator.ValidationResult result = validator.validate(rawHeader)

        then: "an error is reported"
        result.containsIssues()
        result.report().hasErrors()

        and: "no WebLinks are produced for invalid URIs"
        result.weblinks().isEmpty()
    }

    /**
     * When there are multiple links and one has an invalid URI,
     * the validator should still validate all links and produce
     * WebLinks for the valid ones.
     */
    def "multiple links - one invalid URI does not prevent valid WebLinks"() {
        given:
        def rawHeader = new RawLinkHeader([
                new RawLink("not a uri", []),
                new RawLink("https://example.org/valid", [])
        ])

        and:
        def validator = new Rfc8288Validator()

        when:
        Validator.ValidationResult result = validator.validate(rawHeader)

        then: "at least one error is reported for the invalid entry"
        result.containsIssues()
        result.report().hasErrors()

        and: "the valid URI still yields a WebLink"
        result.weblinks().size() == 1
        result.weblinks().first().reference().toString() == "https://example.org/valid"
    }

    /**
     * Unknown / extension parameters should be preserved on the WebLink
     * and must not trigger errors at RFC 8288 level.
     *
     * Example: Link: <https://example.org>; foo="bar"
     */
    def "unknown extension parameters are preserved and do not cause issues"() {
        given:
        def params = [new RawParam("x-custom", "value")]  // arbitrary extension parameter
        def rawHeader = new RawLinkHeader([
                new RawLink("https://example.org/with-param", params)
        ])

        and:
        def validator = new Rfc8288Validator()

        when:
        Validator.ValidationResult result = validator.validate(rawHeader)

        then: "no errors are reported for unknown parameters"
        !result.report().hasErrors()

        and: "at RFC level, we do not warn about extension parameters either (optional; adjust if you decide to warn)"
        !result.report().hasWarnings()

        and: "the parameter is preserved on the resulting WebLink"
        result.weblinks().size() == 1
        def link = result.weblinks().first()
        link.params().get("x-custom").get() == "value"
    }

    /**
     * A parameter without a value (e.g. 'rel' without '=...') is structurally
     * allowed in RFC 8288. At the RFC semantic level we accept it and leave any
     * deeper interpretation to profile-specific validators (e.g. Signposting).
     *
     * How you map "no value" into your RawLink/WebLink model is up to your
     * implementation; here we assume null or empty string is used to represent it.
     */
    def "parameter without value is accepted at RFC level"() {
        given:
        // Example representation: parameter present with null value.
        // Adapt this to your actual RawLink model.
        def params = [new RawParam("rel", null)]
        def rawHeader = new RawLinkHeader([
                new RawLink("https://example.org/no-value-param", params)
        ])

        and:
        def validator = new Rfc8288Validator()

        when:
        Validator.ValidationResult result = validator.validate(rawHeader)

        then: "URI is valid, so we get a WebLink back"
        result.weblinks().size() == 1

        and: "parameter without value does not cause an error at RFC-level"
        !result.report().hasErrors()

        // You may or may not decide to warn here; if you later choose to warn, adjust this:
        // !result.report().hasWarnings()
    }
}
