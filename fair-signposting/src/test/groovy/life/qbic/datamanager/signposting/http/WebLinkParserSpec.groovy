package life.qbic.datamanager.signposting.http

import life.qbic.datamanager.signposting.http.lexer.SimpleWebLinkLexer
import life.qbic.datamanager.signposting.http.parser.SimpleWebLinkParser
import spock.lang.Specification

class WebLinkParserSpec extends Specification {

    /**
     * Why valid: link-value is < URI-Reference > with zero link-params.
     * Spec: RFC 8288 Section 3 (“Link Serialisation in HTTP Headers”), ABNF link-value = "<" URI-Reference ">" *(...); * allows zero params.
     */
    def "Minimal working serialized link, no parameters"() {
        given:
        var validSerialisation = "<https://example.org>"

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * 	Why valid: link-param is token BWS [ "=" BWS token ]; both rel and self are tokens.
     * 	Spec: RFC 8288 Section 3; RFC 7230 section  3.2.6 defines token.
     */
    def "Single parameter, token value"() {
        given:
        var validSerialisation = "<https://example.org/resource>; rel=self"

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * 	Why valid: link-param value may be token / quoted-string; both forms equivalent.
     * 	Spec: RFC 8288 section 3 (note on token vs quoted-string equivalence); RFC 7230 section 3.2.6 for quoted-string.
     */
    def "Single parameter, quoted-string value"() {
        given:
        var validSerialisation = '<https://example.org/resource>; rel="self"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * Why valid: ABNF allows zero or more ";" link-param after URI.
     * Spec: RFC 8288 section 3, *( OWS ";" OWS link-param ).
     */
    def "Multiple parameters"() {
        given:
        var validSerialisation = '<https://example.org/resource>; rel="self"; type="application/json"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * Why valid: OWS and BWS allow optional whitespace around separators and =.
     * Spec: RFC 8288 section 3 (uses OWS/BWS); RFC 7230 section 3.2.3 (OWS), section 3.2.4 (BWS concept).
     */
    def "Whitespace around semi-colon and ="() {
        given:
        var validSerialisation = '<https://example.org/resource>  ;  rel = "self"  ;  type = application/json'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * Why valid: link-param = token BWS [ "=" BWS ( token / quoted-string ) ]; the [ ... ] part is optional, so no = is allowed.
     * Spec: RFC 8288 section 3, link-param ABNF (optional value).
     */
    def "Parameter without value"() {
        given:
        var validSerialisation = "<https://example.org/resource>; rel"

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * Why valid: Empty string is a valid quoted-string.
     * Spec: RFC 7230 section 3.2.6 (quoted-string can contain zero or more qdtext).
     */
    def "Parameter with empty quoted string"() {
        given:
        var validSerialisation = '<https://example.org/resource>; title=""'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * Why valid: rel value is defined as a space-separated list of link relation types.
     * Spec: RFC 8288 section 3.3 (“Relation Types”), which describes rel as a list of relation types.
     */
    def "Multiple rel values in one parameter"() {
        given:
        var validSerialisation = '<https://example.org/resource>; rel="self describedby item"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * Why valid: URI-Reference may be relative, resolved against base URI.
     * Spec: RFC 8288 section 3 (uses URI-Reference); RFC 3986 section 4.1 (“URI Reference”).
     */
    def "Relative URI"() {
        given:
        var validSerialisation = '</relative/path>; rel="item"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * Why valid: At the header level, field-content is opaque to RFC 8288; title is a defined target attribute and its value is a quoted-string.
     * Spec: RFC 8288 section 3 (defines title as a target attribute); RFC 7230 section 3.2 (header fields treat value as opaque except for defined syntax).
     */
    def "Non-ASCII in quoted-string title"() {
        given:
        var validSerialisation = '<https://example.org/resource>; title="Données de recherche"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * Why valid: link-value uses standard link-param names; rel="linkset" and type="application/linkset+json" are ordinary parameters.
     * Spec: RFC 8288 section 3 (general link-param usage); linkset relation and media type from the Linkset draft (compatible with RFC 8288).
     */
    def "Linkset type example"() {
        given:
        var validSerialisation = '<https://example.org/resource.linkset.json>; rel="linkset"; type="application/linkset+json"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * Why valid: Link = #link-value; #rule allows 1+ link-values separated by commas in a single header field.
     * Spec: RFC 8288 section 3 (Link = #link-value); RFC 7230 section 7 (“ABNF list extension: #rule”).
     */
    def "Multiple link-values in one header"() {
        given:
        var validSerialisation = '<https://example.org/a>; rel="self", <https://example.org/b>; rel="next"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    def "Multiple links without parameters"() {
        given:
        var validSerialisation = '<https://example.com/1>, <https://example.com/2>'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * Why valid: type parameter carries a media-type; application/ld+json fits token syntax and media-type grammar.
     * Spec: RFC 8288 section 3 (defines type parameter); RFC 7231 section 3.1.1.1 (media-type grammar uses tokens).
     */
    def "Parameter value as token with slash"() {
        given:
        var validSerialisation = '<https://example.org/resource>; type=application/ld+json'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * Why valid: anchor is a registered link-parameter giving the context URI; its value is a quoted-string.
     * Spec: RFC 8288 section 3.2 (“Target Attributes”) defines anchor; RFC 7230 section 3.2.6 for quoted-string.
     */
    def "Anchor parameter"() {
        given:
        var validSerialisation = '<https://example.org/api/records/123>; rel="self"; anchor="https://example.org/records/123"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * Why valid: link-param value may be token or quoted-string; mixing quoted and unquoted values is allowed.
     * Spec: RFC 8288 section 3 (token / quoted-string equivalence for link-param values); RFC 7230 section 3.2.6.
     */
    def "Mixed quoting styles in parameters"() {
        given:
        var validSerialisation = '<https://example.org/resource>; rel=self; type="application/json"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        var result = weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        noExceptionThrown()
        result != null
    }

    /**
     * Why invalid: A trailing comma indicates an empty link value, which is invalid.
     * Spec: RFC 8288 Section 3, link-value = "<" URI-Reference ">" *( OWS ";" OWS link-param )”
     */
    def "No trailing comma allowed for multiple link values"() {
        given:
        var validSerialisation = '<https://example.org>,'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        thrown(WebLinkParser.StructureException.class)
    }

    def "No trailing semicolon allowed for multiple link values"() {

        given:
        var validSerialisation = '<https://example.org>;'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        weblinkParser.parse(lexer.lex(validSerialisation))

        then:
        thrown(WebLinkParser.StructureException.class)
    }


    /**
     * Why invalid: link-value must start with "<" URI-Reference ">"; a bare URI with params does not match link-value syntax.
     * Spec: RFC 8288 Section 3, link-value = "<" URI-Reference ">" *( ... ).
     */
    def "Invalid: Missing angle brackets around URI"() {
        given:
        var invalidSerialisation = 'https://example.org/resource; rel="self"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        weblinkParser.parse(lexer.lex(invalidSerialisation))

        then:
        thrown(WebLinkParser.StructureException.class)
    }

    /**
     * Why invalid: link-value requires a "<URI-Reference>" prefix; parameters alone do not form a valid link-value.
     * Spec: RFC 8288 Section 3, link-value ABNF.
     */
    def "Invalid: Parameters without URI"() {
        given:
        var invalidSerialisation = 'rel="self"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        weblinkParser.parse(lexer.lex(invalidSerialisation))

        then:
        thrown(WebLinkParser.StructureException.class)

    }

    /**
     * Why invalid: link-param must start with token; an empty name before equal sign violates token = 1*tchar.
     * Spec: RFC 8288 section 3, link-param = token ...; RFC 7230 section 3.2.6 (token = 1*tchar).
     */
    def "Invalid: Empty parameter name"() {
        given:
        var invalidSerialisation = '<https://example.org/resource>; =self'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        weblinkParser.parse(lexer.lex(invalidSerialisation))

        then:
        thrown(WebLinkParser.StructureException.class)
    }

    /**
     * Why invalid: Each ";" must be followed by a link-param; ";;" introduces an empty parameter without a token.
     * Spec: RFC 8288 section 3, *( OWS ";" OWS link-param ) requires a link-param after each ";".
     */
    def "Invalid: Double semicolon introduces empty parameter"() {
        given:
        var invalidSerialisation = '<https://example.org/resource>;; rel="self"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        weblinkParser.parse(lexer.lex(invalidSerialisation))

        then:
        thrown(WebLinkParser.StructureException.class)
    }

    /**
     * Why invalid: Comma is not allowed in token; parameter name containing "," violates token = 1*tchar.
     * Spec: RFC 7230 section 3.2.6 (tchar set does not include ",").
     */
    def "Invalid: Parameter name with illegal character"() {
        given:
        var invalidSerialisation = '<https://example.org/resource>; re,l="self"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        weblinkParser.parse(lexer.lex(invalidSerialisation))

        then:
        thrown(WebLinkParser.StructureException.class)
    }


    /**
     * Why invalid: link-param requires a token before "="; "=" without a parameter name violates link-param syntax.
     * Spec: RFC 8288 section 3, link-param = token BWS [ "=" ... ]; RFC 7230 section 3.2.6 (token required).
     */
    def "Invalid: Parameter with only equals sign and no name"() {
        given:
        var invalidSerialisation = '<https://example.org/resource>; = "self"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        weblinkParser.parse(lexer.lex(invalidSerialisation))

        then:
        thrown(WebLinkParser.StructureException.class)
    }

    /**
     * Why invalid: link-value must start with "<URI-Reference>"; placing parameters before the URI does not match the ABNF.
     * Spec: RFC 8288 section 3, link-value = "<" URI-Reference ">" *( ... ).
     */
    def "Invalid: Parameters before URI"() {
        given:
        var invalidSerialisation = 'rel="self"; <https://example.org/resource>'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        weblinkParser.parse(lexer.lex(invalidSerialisation))

        then:
        thrown(WebLinkParser.StructureException.class)
    }

    /**
     * Why invalid: URI must be enclosed in "<" and ">"; bare URI with parameters is not a valid link-value.
     * Spec: RFC 8288 section 3, "<" URI-Reference ">" is mandatory in link-value.
     */
    def "Invalid: URI not enclosed in angle brackets"() {
        given:
        var invalidSerialisation = 'https://example.org/resource; rel="self"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        weblinkParser.parse(lexer.lex(invalidSerialisation))

        then:
        thrown(WebLinkParser.StructureException.class)
    }


    /**
     * Why invalid: After ">" only OWS ";" OWS link-param is allowed; arbitrary token "foo" between ">" and ";" violates link-value syntax.
     * Spec: RFC 8288 section 3, link-value = "<" URI-Reference ">" *( OWS ";" OWS link-param ).
     */
    def "Invalid: Garbage between URI and first parameter"() {
        given:
        var invalidSerialisation = '<https://example.org/resource> foo ; rel="self"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        weblinkParser.parse(lexer.lex(invalidSerialisation))

        then:
        thrown(WebLinkParser.StructureException.class)
    }

    /**
     * Why invalid: #link-value requires 1+ elements separated by commas; a leading comma introduces an empty element.
     * Spec: RFC 8288 section 3 (Link = #link-value); RFC 7230 section 7 (#rule does not allow empty list elements).
     */
    def "Invalid: Leading comma in Link header list"() {
        given:
        var invalidSerialisation = ', <https://example.org/resource>; rel="self"'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        weblinkParser.parse(lexer.lex(invalidSerialisation))

        then:
        thrown(WebLinkParser.StructureException.class)
    }

    /**
     * Why invalid: #link-value requires 1+ elements separated by commas; a trailing comma implies an empty last element.
     * Spec: RFC 8288 section 3 (Link = #link-value); RFC 7230 section 7 (#rule does not allow empty list elements).
     */
    def "Invalid: Trailing comma in Link header list"() {
        given:
        var invalidSerialisation = '<https://example.org/resource>; rel="self",'

        and:
        var weblinkParser = SimpleWebLinkParser.create()

        and:
        var lexer = new SimpleWebLinkLexer()

        when:
        weblinkParser.parse(lexer.lex(invalidSerialisation))

        then:
        thrown(WebLinkParser.StructureException.class)
    }

}
