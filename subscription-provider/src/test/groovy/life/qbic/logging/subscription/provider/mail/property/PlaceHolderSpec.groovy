package life.qbic.logging.subscription.provider.mail.property


import spock.lang.Specification

class PlaceHolderSpec extends Specification {

    def "given a placeholder, return the placeholder variable name"() {
        when:
        def placeholder = Placeholder.create(testPlaceholder)

        then:
        placeholder.name().equals("SUPER_PLACEHOLDER")

        where:
        testPlaceholder                     | _
        '${SUPER_PLACEHOLDER}'              | _
        '   ${     SUPER_PLACEHOLDER}'      | _
        '${     SUPER_PLACEHOLDER}'         | _
        '${SUPER_PLACEHOLDER }    '         | _
        '${SUPER_PLACEHOLDER    }'          | _
        '${SUPER_PLACEHOLDER }    '         | _
        '${     SUPER_PLACEHOLDER    }    ' | _
    }

    def "given an invalid placeholder, return an empty placeholder name"() {
        when:
        Placeholder.create(testPlaceholder)

        then:
        thrown(IllegalArgumentException)

        where:
        testPlaceholder       | _
        '{SUPER_PLACEHOLDER}' | _
        '${SUPER_PLACEHOLDER' | _
        '$SUPER_PLACEHOLDER}' | _
        'SUPER_PLACEHOLDER'   | _
    }

}
