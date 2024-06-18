package life.qbic.datamanager.views.identicon

import spock.lang.Specification

class IdenticonGeneratorSpec extends Specification {
    def "fails for null input"() {
        when:
        IdenticonGenerator.generateIdenticonSVG(null)
        then:
        thrown(IllegalArgumentException)
    }

    def "fails for empty input"() {
        when:
        IdenticonGenerator.generateIdenticonSVG("")
        then:
        thrown(IllegalArgumentException)
    }

    def "works for other input"() {
        when:
        IdenticonGenerator.generateIdenticonSVG(input)
        then:
        notThrown(IllegalArgumentException)
        where:
        input <<
                ["test",
                 "1ca88d1f-acdb-42fe-a7ab-2d841163de41",
                 UUID.randomUUID().toString(),
                 UUID.randomUUID().toString(),
                 UUID.randomUUID().toString(),
                 UUID.randomUUID().toString(),
                 UUID.randomUUID().toString(),
                 UUID.randomUUID().toString()]
    }
}
