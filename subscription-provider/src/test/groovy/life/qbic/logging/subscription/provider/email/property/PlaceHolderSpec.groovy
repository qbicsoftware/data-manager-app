package life.qbic.logging.subscription.provider.email.property

import life.qbic.logging.subscription.provider.email.property.Placeholder
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class PlaceHolderSpec extends Specification {

    def "given a placeholder, return the placeholder variable name"() {
        given:
        def extractedName = Placeholder.placeholderName(testPlaceholder)

        expect:
        Placeholder.isPlaceholder(testPlaceholder)
        extractedName.equals("SUPER_PLACEHOLDER")

        where:
        testPlaceholder | _
        '${SUPER_PLACEHOLDER}' | _
        '   ${     SUPER_PLACEHOLDER}' | _
        '${     SUPER_PLACEHOLDER}' | _
        '${SUPER_PLACEHOLDER }    ' | _
        '${SUPER_PLACEHOLDER    }' | _
        '${SUPER_PLACEHOLDER }    ' | _
        '${     SUPER_PLACEHOLDER    }    ' | _
    }

    def "given an invalid placeholder, return an empty placeholder name"() {

        given:
        def extractedName = Placeholder.placeholderName(testPlaceholder as String)

        expect:
        assert !Placeholder.isPlaceholder(testPlaceholder as String)
        extractedName.isEmpty()

        where:
        testPlaceholder | _
        '{SUPER_PLACEHOLDER}' | _
        '${SUPER_PLACEHOLDER' | _
        '$SUPER_PLACEHOLDER}' | _
        'SUPER_PLACEHOLDER'   | _
    }

}
