package life.qbic.datamanager.signposting.http

import life.qbic.datamanager.signposting.http.WebLinkLexer.LexingException
import life.qbic.datamanager.signposting.http.lexing.WebLinkToken
import life.qbic.datamanager.signposting.http.parsing.RawLinkHeader
import life.qbic.datamanager.signposting.http.WebLinkValidator.Issue
import life.qbic.datamanager.signposting.http.WebLinkValidator.IssueReport
import life.qbic.datamanager.signposting.http.WebLinkValidator.ValidationResult
import spock.lang.Specification
import spock.lang.Unroll

class WebLinkProcessorSpec extends Specification {

    // ---------------------------------------------------------------------------
    // Helpers – ADAPT CONSTRUCTORS HERE
    // ---------------------------------------------------------------------------

    /**
     * Create a minimal but real WebLinkToken list.
     *
     */
    static List<WebLinkToken> dummyTokens() {
        return List.of(
                new WebLinkToken(WebLinkTokenType.URI, "https://example.org", 0)
        )
    }

    /**
     * Create a minimal but real RawLinkHeader.
     * Adjust constructor to your actual RawLinkHeader definition.
     *
     * Example assumption:
     *   public record RawLinkHeader(List<RawLink> rawLinks) { }
     */
    static RawLinkHeader dummyParsedHeader() {
        return new RawLinkHeader(List.of())
    }

    /**
     * Create a minimal but real WebLink instance.
     * Adjust constructor to your actual WebLink record/class.
     *
     * Example assumption:
     *   public record WebLink(URI reference, Map<String, ?> parameters) { }
     */
    static WebLink dummyWebLink(String id) {
        return new WebLink(
                URI.create("https://example.org/" + id),
                List.of()
        )
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    def "default processor can process minimal valid link header"() {
        given:
        def processor = new WebLinkProcessor.Builder().build()
        def input = "<https://example.org>"

        when:
        def result = processor.process(input)

        then:
        result != null
        result.weblinks() != null
        result.report() != null
    }

    /**
     * When a custom lexer is provided, it must be used instead of the default one.
     */
    def "processor uses configured lexer instead of default"() {
        given:
        def lexer = Mock(WebLinkLexer)
        def parser = Mock(WebLinkParser)
        def validator = Mock(WebLinkValidator)

        def tokens = dummyTokens()
        def parsedHeader = dummyParsedHeader()
        def validationResult = new ValidationResult(List.of(), new IssueReport(List.of()))

        and:
        def processor = new WebLinkProcessor.Builder()
                .withLexer(lexer)
                .withParser(parser)
                .withValidator(validator)
                .build()

        when:
        def result = processor.process("<ignored>")

        then:
        1 * lexer.lex("<ignored>") >> tokens
        1 * parser.parse(tokens) >> parsedHeader
        1 * validator.validate(parsedHeader) >> validationResult

        and:
        result.weblinks().isEmpty()
        !result.report().hasErrors()
    }

    /**
     * When a custom parser is provided, it must be used instead of the default one.
     */
    def "processor uses configured parser instead of default"() {
        given:
        def lexer = Mock(WebLinkLexer)
        def parser = Mock(WebLinkParser)
        def validator = Mock(WebLinkValidator)

        def tokens = dummyTokens()
        def parsedHeader = dummyParsedHeader()
        def validationResult = new ValidationResult(List.of(), new IssueReport(List.of()))

        and:
        def processor = new WebLinkProcessor.Builder()
                .withLexer(lexer)
                .withParser(parser)
                .withValidator(validator)
                .build()

        when:
        def result = processor.process("<something>")

        then:
        1 * lexer.lex("<something>") >> tokens
        1 * parser.parse(tokens) >> parsedHeader
        1 * validator.validate(parsedHeader) >> validationResult

        and:
        result != null
    }

    def "builder injects default validator when none configured"() {
        given:
        def processor = new WebLinkProcessor.Builder().build()
        def input = "<https://example.org>"

        when:
        def result = processor.process(input)

        then:
        result != null
        result.weblinks() != null
        result.report() != null
    }

    def "aggregates issues from multiple validators and uses last validator's weblinks"() {
        given:
        def lexer = Mock(WebLinkLexer)
        def parser = Mock(WebLinkParser)
        def validator1 = Mock(WebLinkValidator)
        def validator2 = Mock(WebLinkValidator)

        def tokens = dummyTokens()
        def parsedHeader = dummyParsedHeader()

        def link1 = dummyWebLink("v1")
        def link2 = dummyWebLink("v2")

        def issue1 = Issue.error("first")
        def issue2 = Issue.warning("second")

        def result1 = new ValidationResult(List.of(link1), new IssueReport(List.of(issue1)))
        def result2 = new ValidationResult(List.of(link2), new IssueReport(List.of(issue2)))

        and:
        def processor = new WebLinkProcessor.Builder()
                .withLexer(lexer)
                .withParser(parser)
                .withValidator(validator1)
                .withValidator(validator2)
                .build()

        when:
        def result = processor.process("<x>")

        then:
        1 * lexer.lex("<x>") >> tokens
        1 * parser.parse(tokens) >> parsedHeader
        1 * validator1.validate(parsedHeader) >> result1
        1 * validator2.validate(parsedHeader) >> result2

        and:
        result.weblinks() == List.of(link2)
        result.report().issues().containsAll(List.of(issue1, issue2))
        result.report().issues().size() == 2
    }

    @Unroll
    def "process throws NullPointerException for null input (#caseName)"() {
        given:
        def processor = new WebLinkProcessor.Builder().build()

        when:
        processor.process(input)

        then:
        thrown(NullPointerException)

        where:
        caseName      | input
        "null header" | null
    }

    def "lexer exception is propagated and prevents parser and validators from running"() {
        given:
        def lexer = Mock(WebLinkLexer)
        def parser = Mock(WebLinkParser)
        def validator = Mock(WebLinkValidator)

        def processor = new WebLinkProcessor.Builder()
                .withLexer(lexer)
                .withParser(parser)
                .withValidator(validator)
                .build()

        when:
        processor.process("<bad")

        then:
        1 * lexer.lex("<bad") >> { throw new LexingException("boom") }
        0 * parser._
        0 * validator._

        and:
        thrown(LexingException)
    }

    def "parser exception is propagated and prevents validators from running"() {
        given:
        def lexer = Mock(WebLinkLexer)
        def parser = Mock(WebLinkParser)
        def validator = Mock(WebLinkValidator)

        def tokens = dummyTokens()
        def processor = new WebLinkProcessor.Builder()
                .withLexer(lexer)
                .withParser(parser)
                .withValidator(validator)
                .build()

        when:
        processor.process("<something>")

        then:
        1 * lexer.lex("<something>") >> tokens
        1 * parser.parse(tokens) >> { throw new RuntimeException("parse error") }
        0 * validator._

        and:
        thrown(RuntimeException)
    }

    def "validator exception is propagated and stops further validators"() {
        given:
        def lexer = Mock(WebLinkLexer)
        def parser = Mock(WebLinkParser)
        def validator1 = Mock(WebLinkValidator)
        def validator2 = Mock(WebLinkValidator)

        def tokens = dummyTokens()
        def parsedHeader = dummyParsedHeader()

        def processor = new WebLinkProcessor.Builder()
                .withLexer(lexer)
                .withParser(parser)
                .withValidator(validator1)
                .withValidator(validator2)
                .build()

        when:
        processor.process("<header>")

        then:
        1 * lexer.lex("<header>") >> tokens
        1 * parser.parse(tokens) >> parsedHeader
        1 * validator1.validate(parsedHeader) >> { throw new RuntimeException("validator boom") }
        0 * validator2._

        and:
        thrown(RuntimeException)
    }

    def "throws IllegalStateException when no validator produces a result (defensive branch)"() {
        given:
        def lexer = Mock(WebLinkLexer)
        def parser = Mock(WebLinkParser)

        def tokens = dummyTokens()
        def parsedHeader = dummyParsedHeader()

        def processor = new WebLinkProcessor.Builder()
                .withLexer(lexer)
                .withParser(parser)
                .build()

        and:
        def validatorsField = WebLinkProcessor.getDeclaredField("validators")
        validatorsField.accessible = true
        validatorsField.set(processor, List.of()) // simulate broken internal state

        when:
        processor.process("<x>")

        then:
        1 * lexer.lex("<x>") >> tokens
        1 * parser.parse(tokens) >> parsedHeader

        and:
        def ex = thrown(IllegalStateException)
        ex.message.contains("No validation result was found")
    }

    def "external mutation of issue list from validator does not break aggregated result"() {
        given:
        def lexer = Mock(WebLinkLexer)
        def parser = Mock(WebLinkParser)
        def validator = Mock(WebLinkValidator)

        def tokens = dummyTokens()
        def parsedHeader = dummyParsedHeader()

        def mutableIssues = new ArrayList<Issue>()
        mutableIssues.add(Issue.error("original"))

        def validationResult = new ValidationResult(
                List.of(dummyWebLink("l1")),
                new IssueReport(mutableIssues)
        )

        def processor = new WebLinkProcessor.Builder()
                .withLexer(lexer)
                .withParser(parser)
                .withValidator(validator)
                .build()

        when:
        def result = processor.process("<h>")

        then:
        1 * lexer.lex("<h>") >> tokens
        1 * parser.parse(tokens) >> parsedHeader
        1 * validator.validate(parsedHeader) >> validationResult

        and:
        result.report().issues().size() == 1

        when:
        mutableIssues.clear()

        then:
        result.report().issues().size() == 1
    }

    def "external mutation of weblink list from validator does not alter processor result"() {
        given:
        def lexer = Mock(WebLinkLexer)
        def parser = Mock(WebLinkParser)
        def validator = Mock(WebLinkValidator)

        def tokens = dummyTokens()
        def parsedHeader = dummyParsedHeader()

        def mutableWebLinks = new ArrayList<WebLink>()
        def link = dummyWebLink("foo")
        mutableWebLinks.add(link)

        def validationResult = new ValidationResult(
                mutableWebLinks,
                new IssueReport(List.of())
        )

        def processor = new WebLinkProcessor.Builder()
                .withLexer(lexer)
                .withParser(parser)
                .withValidator(validator)
                .build()

        when:
        def result = processor.process("<h>")

        then:
        1 * lexer.lex("<h>") >> tokens
        1 * parser.parse(tokens) >> parsedHeader
        1 * validator.validate(parsedHeader) >> validationResult

        and:
        result.weblinks().size() == 1
        result.weblinks().first() == link

        when:
        mutableWebLinks.clear()

        then:
        result.weblinks().size() == 1
    }
}
