package life.qbic.logging.subscription.provider.mail.property


import spock.lang.Specification

class PropertyFileParserSpec extends Specification {

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
