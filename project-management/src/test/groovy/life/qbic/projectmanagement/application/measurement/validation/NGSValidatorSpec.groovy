package life.qbic.projectmanagement.application.measurement.validation

import spock.lang.Specification

class NGSValidatorSpec extends Specification {

    def "Given a valid NGS measurement metadata property collection, pass the validation"() {
        given:
        def validNGSproperties = ["qbic sample id", "organisation id", "facility", "instrument", "sequencing read type", "library kit", "flow cell", "run protocol", "index i5", "index i7", "note"]

        when:
        def isNGSMetadata = NGSValidator.isNGS(validNGSproperties)

        then:
        isNGSMetadata

    }

    def "Missing properties for the NGS metadata collection must result in an unsuccessful validation"() {
        given:
        def missingProperties= ["organism id", "facility", "instrument", "sequencing read type", "library kit", "flow cell", "run protocol", "index i5", "index i7", "note"]

        when:
        def isNGSMetadata = NGSValidator.isNGS(missingProperties)

        then:
        !isNGSMetadata
    }

    def "Providing no properties for the NGS metadata collection must result in an unsuccessful validation"() {
        given:
        def missingProperties= []

        when:
        def isNGSMetadata = NGSValidator.isNGS(missingProperties)

        then:
        !isNGSMetadata
    }

    def "A complete property set must be valid no matter the letter casing style"() {
        given:
        def chaosCasing= ["QbiC SaMpLe ID", "organisation ID", "FACiLity", "Instrument", "sequencing read type", "library kit", "flow cell", "run protocol", "index i5", "index i7", "note"]

        when:
        def isNGSMetadata = NGSValidator.isNGS(chaosCasing)

        then:
        isNGSMetadata
    }

}
