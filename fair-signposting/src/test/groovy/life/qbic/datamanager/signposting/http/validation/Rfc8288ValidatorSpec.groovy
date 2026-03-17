package life.qbic.datamanager.signposting.http.validation

import life.qbic.datamanager.signposting.http.WebLinkValidator
import life.qbic.datamanager.signposting.http.WebLink
import life.qbic.datamanager.signposting.http.parsing.RawLink
import life.qbic.datamanager.signposting.http.parsing.RawLinkHeader
import life.qbic.datamanager.signposting.http.parsing.RawParam
import spock.lang.Specification

/**
 * Specification for {@link Rfc8288WebLinkValidator}.
 *
 * Covers basic RFC 8288 semantics:
 * <ul>
 *   <li>Valid URIs create {@link WebLink} instances without issues.</li>
 *   <li>Invalid URIs create error {@link WebLinkValidator.Issue}s and no WebLink for that entry.</li>
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
        def validator = new Rfc8288WebLinkValidator()

        when:
        WebLinkValidator.ValidationResult result = validator.validate(rawHeader)

        then: "no issues are reported"
        !result.containsIssues()
        !result.report().hasErrors()
        !result.report().hasWarnings()

        and: "exactly one WebLink is produced with the expected URI and withoutValue params"
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
        def validator = new Rfc8288WebLinkValidator()

        when:
        WebLinkValidator.ValidationResult result = validator.validate(rawHeader)

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
        def validator = new Rfc8288WebLinkValidator()

        when:
        WebLinkValidator.ValidationResult result = validator.validate(rawHeader)

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
        def validator = new Rfc8288WebLinkValidator()

        when:
        WebLinkValidator.ValidationResult result = validator.validate(rawHeader)

        then: "no errors are reported for unknown parameters"
        !result.report().hasErrors()

        and: "at RFC level, we do not warn about extension parameters either (optional; adjust if you decide to warn)"
        !result.report().hasWarnings()

        and: "the parameter is preserved on the resulting WebLink"
        result.weblinks().size() == 1
        def link = result.weblinks().first()
        link.extensionAttribute("x-custom")[0] == "value"
    }

    /**
     * A parameter without a value (e.g. 'rel' without '=...') is structurally
     * allowed in RFC 8288. At the RFC semantic level we accept it and leave any
     * deeper interpretation to profile-specific validators (e.g. Signposting).
     *
     * How you map "no value" into your RawLink/WebLink model is up to your
     * implementation; here we assume null or withoutValue string is used to represent it.
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
        def validator = new Rfc8288WebLinkValidator()

        when:
        WebLinkValidator.ValidationResult result = validator.validate(rawHeader)

        then: "URI is valid, so we get a WebLink back"
        result.weblinks().size() == 1

        and: "parameter without value does not cause an error at RFC-level"
        !result.report().hasErrors()
    }

    def "parameter anchor with one occurrence is allowed"() {
        given:
        // Example representation: parameter present with null value.
        // Adapt this to your actual RawLink model.
        def params = [new RawParam("anchor", "https://example.org/one-anchor-only")]
        def rawHeader = new RawLinkHeader([
                new RawLink("https://example.org/one-anchor-only", params)
        ])

        and:
        def validator = new Rfc8288WebLinkValidator()

        when:
        WebLinkValidator.ValidationResult result = validator.validate(rawHeader)

        then: "URI is valid, so we get a WebLink back"
        result.weblinks().size() == 1

        and: "parameter anchor with only one occurrence does not cause an error at RFC-level"
        !result.report().hasErrors()
    }

    def "a parameter with allowed multiplicity of 1 must be only processed on the first occurrence"() {
        given:
        // Example representation: parameter present with null value.
        // Adapt this to your actual RawLink model.
        def firstParam = new RawParam("rel", "https://example.org/first-occurrence")
        def secondParam = new RawParam("rel", "https://example.org/next-occurrence")
        def params = [firstParam, secondParam]
        def rawHeader = new RawLinkHeader([
                new RawLink("https://example.org/one-anchor-only", params)
        ])

        and:
        def validator = new Rfc8288WebLinkValidator()

        when:
        WebLinkValidator.ValidationResult result = validator.validate(rawHeader)

        then: "URI is valid, so we get a WebLink back"
        result.weblinks().size() == 1

        and: "parameter rel with only one occurrence does not cause an error at RFC-level"
        !result.report().hasErrors()

        and: "but results in a warning, since the second occurrence is skipped"
        result.report().hasWarnings()

        and: "uses only the value of the first occurrence"
        var relations = result.weblinks().get(0).rel()
        relations.size() == 1
        relations.get(0).equals(firstParam.value())
    }

    def "the rel parameter can contain multiple relations as whitespace-separated list"() {
        given:
        // Example representation: parameter present with null value.
        // Adapt this to your actual RawLink model.
        def firstParam = new RawParam("rel", "self describedby     another")
        def params = [firstParam]
        def rawHeader = new RawLinkHeader([
                new RawLink("https://example.org/one-anchor-only", params)
        ])

        and:
        def validator = new Rfc8288WebLinkValidator()

        when:
        WebLinkValidator.ValidationResult result = validator.validate(rawHeader)

        then: "URI is valid, so we get a WebLink back"
        result.weblinks().size() == 1

        and: "parameter rel with only one occurrence does not cause an error at RFC-level"
        !result.report().hasErrors()

        and: "results in no warnings"
        !result.report().hasWarnings()

        and: "splits the relations into three values"
        var relations = result.weblinks().get(0).rel()
        relations.size() == 3
    }


    def "parameter anchor must not have multiple occurrences"() {
        given:
        // Example representation: parameter present with null value.
        // Adapt this to your actual RawLink model.
        def params = [new RawParam("anchor", "https://example.org/one-anchor-only"),
                      new RawParam("anchor", "https://example.org/another-anchor")]
        def rawHeader = new RawLinkHeader([
                new RawLink("https://example.org/one-anchor-only", params)
        ])

        and:
        def validator = new Rfc8288WebLinkValidator()

        when:
        WebLinkValidator.ValidationResult result = validator.validate(rawHeader)

        then: "URI is valid, so we get a WebLink back"
        result.weblinks().size() == 1

        and: "parameter anchor with only one occurrence does not cause an error at RFC-level"
        result.report().hasWarnings()
        result.report().issues().size() == 1
    }
}
