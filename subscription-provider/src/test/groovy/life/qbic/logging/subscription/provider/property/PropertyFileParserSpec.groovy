package life.qbic.logging.subscription.provider.property

import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class PropertyFileParserSpec extends Specification {

    def "Given a property file with placeholders, throw an IllegalArgumentException when the placeholder cannot be resolved"() {
        given:
        def input = this.getClass().getResource("/mail-failing.properties").file

        and:

        when:
        def properties = PropertyFileParser.parse(new File(input))

        then:
        thrown(IllegalArgumentException)

    }

    def "Given a property file without placeholders, parse them accordingly"() {
        given:
        def input = this.getClass().getResource("/mail-valid.properties").file

        and:

        when:
        def properties = PropertyFileParser.parse(new File(input))

        then:
        properties.get("smtp-server").equals("smtp.example.com")
        properties.get("submitter-address").equals("no-reply@qbic.life")

    }

}
