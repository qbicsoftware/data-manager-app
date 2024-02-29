package life.qbic.projectmanagement.application.measurement.validation

import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata
import life.qbic.projectmanagement.application.ontology.OntologyClass
import life.qbic.projectmanagement.application.ontology.OntologyLookupService
import life.qbic.projectmanagement.application.sample.SampleInformationService
import life.qbic.projectmanagement.domain.model.sample.SampleCode
import spock.lang.Specification

class ProteomicsValidatorSpec extends Specification {

    final static ProteomicsMeasurementMetadata validMetadata = new ProteomicsMeasurementMetadata([SampleCode.create("QTEST001AE")],
            "https://ror.org/03a1kwz48", //Universität Tübingen,
            "EFO:0004205" //Illumina MiSeq
    )
    final static OntologyClass illuminaMiSeq = new OntologyClass(
            "efo",
            "3.62.0",
            "http://www.ebi.ac.uk/efo/efo.owl#",
            "Illumina MiSeq",
            "EFO:0004205",
            "The Illumina MiSeq is a high-throughput sequencing machine developed by Illumina. Its primary applications include small whole-genome sequencing, targeted sequencing of a set of genes or gene regions and 16S metagenomic sequencing.",
            "http://www.ebi.ac.uk/efo/EFO_0004205"
    )
    final OntologyLookupService ontologyLookupService = Mock(OntologyLookupService.class, {
        findByCURI(validMetadata.instrumentCURI()) >> Optional.of(illuminaMiSeq)
    })

    final static List<String> validPXPProperties = ["qbic sample ids", "organisation id", "facility", "instrument",
                                                    "pooled sample label", "cycle/fraction name", "digestion method", "digestion enzyme",
                                                    "enrichment method", "injection volume (uL)", "lc column",
                                                    "lcms method", "sample preparation", "sample cleanup (protein)",
                                                    "sample cleanup (peptide)", "note"]

    def "Given a valid proteomics measurement metadata property collection, pass the validation"() {
        given:

        when:
        def isPXPmetadata = ProteomicsValidator.isProteomics(validPXPProperties)

        then:
        isPXPmetadata

    }

    def "Missing properties for the proteomics metadata collection must result in an unsuccessful validation"() {
        given:
        // we just take the NGS property list
        def missingProperties = validPXPProperties
        missingProperties.remove(0)

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

        when:
        def isPxPmetadata = ProteomicsValidator.isProteomics(chaosCasing)

        then:
        isPxPmetadata
        where:
        chaosCasing << [
                validPXPProperties.collect { it.toUpperCase() },
                validPXPProperties.collect { it.toLowerCase() }]


    }

    def "Valid entries in a proteomics measurement metadata object must return a successful validation "() {
        given:
        def validMeasurementEntry = validMetadata

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(_ as SampleCode) >> Optional.of(_)

        and:
        def validator = new ProteomicsValidator(sampleInformationService, ontologyLookupService)

        when:
        def result = validator.validate(validMeasurementEntry)

        then:
        result.allPassed()
        !result.containsWarnings()
        !result.containsFailures()
    }

    /*
    * QBiC Sample ID related test
    *
    */

    def "An unknown sample code in a proteomics measurement metadata object must return a failed validation "() {
        given:
        def validMeasurementEntry = new ProteomicsMeasurementMetadata([SampleCode.create("QNKWN001AE")], validMetadata.organisationId(), validMetadata.instrumentCURI())

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(_ as SampleCode) >> Optional.empty()

        and:
        def validator = new ProteomicsValidator(sampleInformationService, ontologyLookupService)

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
        def validMeasurementEntry = new ProteomicsMeasurementMetadata([unknownSample, sampleToBeFound], validMetadata.organisationId(), validMetadata.instrumentCURI())

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(unknownSample) >> Optional.empty()
        sampleInformationService.findSampleId(sampleToBeFound) >> Optional.of(sampleToBeFound)

        and:
        def validator = new ProteomicsValidator(sampleInformationService, ontologyLookupService)

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
        def validMeasurementEntry = new ProteomicsMeasurementMetadata([], validMetadata.organisationId(), validMetadata.instrumentCURI())

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)

        and:
        def validator = new ProteomicsValidator(sampleInformationService, ontologyLookupService)

        when:
        def result = validator.validate(validMeasurementEntry)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failedEntries() == 1
        result.failures()[0] == "A measurement must contain at least one sample reference. Provided: none"
    }

    /*
     * Organisation ID related test
     *
     */

    def "If an invalid ROR ID for the organisation information is provided, the validation must fail"() {
        given:
        def sampleCode = SampleCode.create("QTEST001AE")
        def pxpMetadata = new ProteomicsMeasurementMetadata(List.of(sampleCode), invalidRorId, validMetadata.instrumentCURI())

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(sampleCode) >> Optional.of(sampleCode)

        and:
        def validator = new ProteomicsValidator(sampleInformationService, ontologyLookupService)


        when:
        def result = validator.validate(pxpMetadata)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.validatedEntries() == 3
        result.containsFailures()
        result.failedEntries() == 1
        result.failures()[0] == "The organisation ID does not seem to be a ROR ID: \"${invalidRorId}\""

        where:
        invalidRorId << [
                "", // missing entry
                "https://ror.org/1234", // invalid unique id pattern
                "https://ror/1243", // missing domain
                "03a1kwz48", // missing url part
                "https://ror.org" // missing unique id part
        ]
    }

    def "If an valid ROR ID for the organisation information is provided, the validation must pass"() {
        given:
        def sampleCode = SampleCode.create("QTEST001AE")
        def pxpMetadata = new ProteomicsMeasurementMetadata(validMetadata.sampleCodes(), validRorId, validMetadata.instrumentCURI())

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(sampleCode) >> Optional.of(sampleCode)

        and:
        def validator = new ProteomicsValidator(sampleInformationService, ontologyLookupService)


        when:
        def result = validator.validate(pxpMetadata)

        then:
        result.allPassed()
        !result.containsWarnings()
        result.validatedEntries() == 3
        !result.containsFailures()

        where:
        validRorId << [
                "https://ror.org/03a1kwz48", // University of Tübingen
                "https://ror.org/00v34f693" // QBiC
        ]
    }
}
