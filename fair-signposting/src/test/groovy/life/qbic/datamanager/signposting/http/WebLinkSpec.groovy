package life.qbic.datamanager.signposting.http

import spock.lang.Specification

class WebLinkSpec extends Specification {

    def "An empty parameter key must throw an FormatException"() {
        given:
        var someURI = URI.create("myuri")

        and:
        var someParameters = new HashMap<String, List<String>>()
        someParameters.put("someKey", "someValue")
        someParameters.put("", "anotherValue")

        when:
        WebLink.create(someURI, someParameters)

        then:
        thrown(FormatException.class)
    }

}
