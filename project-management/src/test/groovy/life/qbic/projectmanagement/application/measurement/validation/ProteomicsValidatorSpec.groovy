package life.qbic.projectmanagement.application.measurement.validation

import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata
import life.qbic.projectmanagement.application.sample.SampleInformationService
import life.qbic.projectmanagement.domain.model.sample.SampleCode
import spock.lang.Specification

class ProteomicsValidatorSpec extends Specification {

    def "Given a valid proteomics measurement metadata property collection, pass the validation"() {
        given:
        def validPxPproperties = ["qbic sample id", "organisation id", "facility", "instrument",
                                  "pooled sample label", "cycle/fraction name","digestion method",
                                  "enrichment method", "injection volume (uL)", "lc column",
                                  "lcms method", "sample preparation", "sample cleanup (protein)",
                                  "sample cleanup (peptide)", "note"]

        when:
        def isPXPmetadata = ProteomicsValidator.isProteomics(validPxPproperties)

        then:
        isPXPmetadata

    }

    def "Missing properties for the proteomics metadata collection must result in an unsuccessful validation"() {
        given:
        // we just take the NGS property list
        def missingProperties = ["organism id", "facility", "instrument", "sequencing read type", "library kit", "flow cell", "run protocol", "index i5", "index i7", "note"]

        when:
        def isPXPmetadata = ProteomicsValidator.isProteomics(missingProperties)

        then:
        !isPXPmetadata
    }

    def "Providing no properties for the NGS metadata collection must result in an unsuccessful validation"() {
        given:
        def missingProperties = []

        when:
        def isPxPmetadata = ProteomicsValidator.isProteomics(missingProperties)

        then:
        !isPxPmetadata
    }

    def "A complete property set must be valid no matter the letter casing style"() {
        given:
        def chaosCasing = ["QBIC sampLE id", "ORganisation ID", "facility", "instrument",
                           "pooled sample label", "cycle/fraction name", "digestion method",
                           "enrichment method", "injection volume (uL)", "LC COlumn",
                           "lcms method", "sample preparation", "sample cleanup (protein)",
                           "sample cleanup (peptide)", "note"]

        when:
        def isPxPmetadata = ProteomicsValidator.isProteomics(chaosCasing)

        then:
        isPxPmetadata
    }

    def "Valid entries in a proteomics measurement metadata object must return a successful validation "() {
        given:
        def validMeasurementEntry = new ProteomicsMeasurementMetadata([SampleCode.create("QTEST001AE")])

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(_ as SampleCode) >> Optional.of(_)

        and:
        def validator = new ProteomicsValidator(sampleInformationService)

        when:
        def result = validator.validate(validMeasurementEntry)

        then:
        result.allPassed()
        !result.containsWarnings()
        !result.containsFailures()
    }


    def "An unknown sample code in a proteomics measurement metadata object must return a failed validation "() {
        given:
        def validMeasurementEntry = new ProteomicsMeasurementMetadata([SampleCode.create("QNKWN001AE")])

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(_ as SampleCode) >> Optional.empty()

        and:
        def validator = new ProteomicsValidator(sampleInformationService)

        when:
        def result = validator.validate(validMeasurementEntry)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failedEntries() == 1
        result.failures()[0] == "Unknown sample with sample id \"QNKWN001AE\""
    }

    def "An unknown sample code when several are provided in a proteomics measurement metadata object must return a failed validation "() {
        given:
        def sampleToBeFound = SampleCode.create("QNWBY999AE")
        def unknownSample = SampleCode.create("QNKWN001AE")

        and:
        def validMeasurementEntry = new ProteomicsMeasurementMetadata([unknownSample, sampleToBeFound])

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(unknownSample) >> Optional.empty()
        sampleInformationService.findSampleId(sampleToBeFound) >> Optional.of(sampleToBeFound)

        and:
        def validator = new ProteomicsValidator(sampleInformationService)

        when:
        def result = validator.validate(validMeasurementEntry)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failedEntries() == 1
        result.failures()[0] == "Unknown sample with sample id \"QNKWN001AE\""
    }

    def "If no sample code is provided, the validation must fail"() {
        given:
        def validMeasurementEntry = new ProteomicsMeasurementMetadata([])

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)

        and:
        def validator = new ProteomicsValidator(sampleInformationService)

        when:
        def result = validator.validate(validMeasurementEntry)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failedEntries() == 1
        result.failures()[0] == "A measurement must contain at least one sample reference. Provided: none"
    }
}
