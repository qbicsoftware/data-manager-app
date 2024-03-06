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
            "EFO:0004205", //Illumina MiSeq
            "1",
            "The geniuses of ITSS",
            "4 Nations lived in harmony",
            "CASPASE6",
            "in solition",
            "Enrichment Index",
            "1337",
            "12",
            "LCMS Method 1",
            "Sample Preparation 1",
            "Cleanup Protein",
            "Cleanup Peptide",
            "Don't tell anyone this is a test"
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

    final static List<String> validPXPProperties = ["qbic sample ids", "sample label", "organisation id", "facility", "instrument",
                                                    "sample pool group", "cycle/fraction name", "digestion method", "digestion enzyme",
                                                    "enrichment method", "injection volume (uL)", "lc column",
                                                    "lcms method", "sample preparation", "sample cleanup (protein)",
                                                    "sample cleanup (peptide)", "note"]

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
        def invalidMeasurementEntry = new ProteomicsMeasurementMetadata([SampleCode.create("QNKWN001AE")],
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "1",
                "The geniuses of ITSS",
                "4 Nations lived in harmony",
                "CASPASE6",
                "in solition",
                "Enrichment Index",
                "1337",
                "12",
                "LCMS Method 1",
                "Sample Preparation 1",
                "Cleanup Protein",
                "Cleanup Peptide",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(_ as SampleCode) >> Optional.empty()

        and:
        def validator = new ProteomicsValidator(sampleInformationService, ontologyLookupService)

        when:
        def result = validator.validate(invalidMeasurementEntry)

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
        def invalidMeasurementEntry = new ProteomicsMeasurementMetadata([sampleToBeFound, unknownSample],
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "1",
                "The geniuses of ITSS",
                "4 Nations lived in harmony",
                "CASPASE6",
                "in solition",
                "Enrichment Index",
                "1337",
                "12",
                "LCMS Method 1",
                "Sample Preparation 1",
                "Cleanup Protein",
                "Cleanup Peptide",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(unknownSample) >> Optional.empty()
        sampleInformationService.findSampleId(sampleToBeFound) >> Optional.of(sampleToBeFound)

        and:
        def validator = new ProteomicsValidator(sampleInformationService, ontologyLookupService)

        when:
        def result = validator.validate(invalidMeasurementEntry)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failedEntries() == 1
        result.failures()[0] == "Unknown sample with sample id \"QNKWN001AE\""
    }

    def "If no sample code is provided, the validation must fail"() {
        given:
        def invalidMeasurementEntry = new ProteomicsMeasurementMetadata([],
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "1",
                "The geniuses of ITSS",
                "4 Nations lived in harmony",
                "CASPASE6",
                "in solition",
                "Enrichment Index",
                "1337",
                "12",
                "LCMS Method 1",
                "Sample Preparation 1",
                "Cleanup Protein",
                "Cleanup Peptide",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)

        and:
        def validator = new ProteomicsValidator(sampleInformationService, ontologyLookupService)

        when:
        def result = validator.validate(invalidMeasurementEntry)

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
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        ProteomicsMeasurementMetadata invalidMetadata = new ProteomicsMeasurementMetadata([validSampleCode],
                 invalidRorId, //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "1",
                "The geniuses of ITSS",
                "4 Nations lived in harmony",
                "CASPASE6",
                "in solition",
                "Enrichment Index",
                "1337",
                "12",
                "LCMS Method 1",
                "Sample Preparation 1",
                "Cleanup Protein",
                "Cleanup Peptide",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)

        and:
        def validator = new ProteomicsValidator(sampleInformationService, ontologyLookupService)


        when:
        def result = validator.validate(invalidMetadata)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.validatedEntries() == 4
        result.failedEntries() == 1
        result.failures()[0] == "The organisation ID does not seem to be a ROR ID: \"${invalidRorId}\""

        where:
        invalidRorId << [
                "https://ror.org/1234", // invalid unique id pattern
                "https://ror/1243", // missing domain
                "03a1kwz48", // missing url part
                "https://ror.org" // missing unique id part
        ]
    }

    def "If no RoRId was provided for the organisation information the validation will fail"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        ProteomicsMeasurementMetadata invalidMetadata = new ProteomicsMeasurementMetadata([validSampleCode],
                "", // missing entry
                "EFO:0004205", //Illumina MiSeq
                "1",
                "The geniuses of ITSS",
                "4 Nations lived in harmony",
                "CASPASE6",
                "in solition",
                "Enrichment Index",
                "1337",
                "12",
                "LCMS Method 1",
                "Sample Preparation 1",
                "Cleanup Protein",
                "Cleanup Peptide",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)

        and:
        def validator = new ProteomicsValidator(sampleInformationService, ontologyLookupService)


        when:
        def result = validator.validate(invalidMetadata)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failedEntries() == 1
        result.failures()[0] == "Organisation: missing mandatory metadata"
    }

    def "If an valid ROR ID for the organisation information is provided, the validation must pass"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        ProteomicsMeasurementMetadata validMetadata = new ProteomicsMeasurementMetadata([validSampleCode],
                validRorId, //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "1",
                "The geniuses of ITSS",
                "4 Nations lived in harmony",
                "CASPASE6",
                "in solition",
                "Enrichment Index",
                "1337",
                "12",
                "LCMS Method 1",
                "Sample Preparation 1",
                "Cleanup Protein",
                "Cleanup Peptide",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)

        and:
        def validator = new ProteomicsValidator(sampleInformationService, ontologyLookupService)


        when:
        def result = validator.validate(validMetadata)

        then:
        result.allPassed()
        !result.containsWarnings()
        result.validatedEntries() == 4
        !result.containsFailures()

        where:
        validRorId << [
                "https://ror.org/03a1kwz48", // University of Tübingen
                "https://ror.org/00v34f693" // QBiC
        ]
    }

    //Todo Extend test for the other mandatory metadata
}
