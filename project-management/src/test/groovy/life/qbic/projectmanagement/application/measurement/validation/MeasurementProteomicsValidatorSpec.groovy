package life.qbic.projectmanagement.application.measurement.validation

import life.qbic.projectmanagement.application.ProjectInformationService
import life.qbic.projectmanagement.application.measurement.Labeling
import life.qbic.projectmanagement.application.measurement.MeasurementService
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata
import life.qbic.projectmanagement.application.ontology.OntologyClass
import life.qbic.projectmanagement.application.ontology.TerminologyService
import life.qbic.projectmanagement.application.sample.SampleInformationService
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.projectmanagement.domain.model.sample.SampleCode
import spock.lang.Specification

import java.util.stream.Collectors

class MeasurementMeasurementProteomicsValidatorSpec extends Specification {

    final static ProteomicsMeasurementMetadata validMetadata = new ProteomicsMeasurementMetadata("", SampleCode.create("QTEST001AE"),
            "",
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
            new Labeling("isotope", "N15"),
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
    final TerminologyService terminologyService = Mock(TerminologyService.class, {
        findByCurie(validMetadata.msDeviceCURIE()) >> Optional.of(illuminaMiSeq)
    })

    final MeasurementService measurementService = Mock(MeasurementService.class)

    final ProjectInformationService projectInformationService = Mock(ProjectInformationService.class)

    final static List<String> validPXPProperties = Collections.unmodifiableList(["qbic sample id", "sample name", "technical replicate", "organisation id", "facility", "ms device",
                                                    "sample pool group", "cycle/fraction name", "digestion method", "digestion enzyme",
                                                    "enrichment method", "injection volume (uL)", "lc column",
                                                    "lcms method", "labeling type", "label", "comment"])

    def "A complete property set must be valid no matter the letter casing style"() {

        when:
        def isPxPmetadata = MeasurementProteomicsValidator.isProteomics(chaosCasing)

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
        def isPXPmetadata = MeasurementProteomicsValidator.isProteomics(validPXPProperties)

        then:
        isPXPmetadata

    }

    def "Missing properties for the proteomics metadata collection must result in an unsuccessful validation"() {
        given:
        // we just take the NGS property list
        def missingProperties = validPXPProperties.stream().collect(Collectors.toList())
        missingProperties.remove(0)

        when:
        def isPXPmetadata = MeasurementProteomicsValidator.isProteomics(missingProperties)

        then:
        !isPXPmetadata
    }

    def "Providing no properties for the NGS metadata collection must result in an unsuccessful validation"() {
        given:
        def missingProperties = []

        when:
        def isPxPmetadata = MeasurementProteomicsValidator.isProteomics(missingProperties)

        then:
        !isPxPmetadata
    }


    def "Valid entries in a proteomics measurement metadata object must return a successful validation "() {
        given:
        def validMeasurementEntry = validMetadata

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(_ as SampleCode) >> Optional.of(_)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementProteomicsValidator(sampleInformationService, terminologyService, measurementService, projectInformationService)

        when:
        def result = validator.validate(validMeasurementEntry, projectId)

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
        def invalidMeasurementEntry = new ProteomicsMeasurementMetadata("", SampleCode.create("QNKWN001AE"),
                "",
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
                new Labeling("isotope", "N15"),
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(_ as SampleCode) >> Optional.empty()
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementProteomicsValidator(sampleInformationService, terminologyService, measurementService, projectInformationService)

        when:
        def result = validator.validate(invalidMeasurementEntry, projectId)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failures()[0] == "Unknown sample with sample id \"QNKWN001AE\""
    }

    def "If no sample code is provided, the validation must fail"() {
        given:
        def invalidMeasurementEntry = new ProteomicsMeasurementMetadata("", null,
                "",
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
                new Labeling("isotope", "N15"),
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementProteomicsValidator(sampleInformationService, terminologyService, measurementService, projectInformationService)

        when:
        def result = validator.validate(invalidMeasurementEntry, projectId)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failures()[0] == "Sample id: missing sample id reference"
    }

    /*
     * Organisation ID related test
     *
     */

    def "If an invalid ROR ID for the organisation information is provided, the validation must fail"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        ProteomicsMeasurementMetadata invalidMetadata = new ProteomicsMeasurementMetadata("", validSampleCode,
                "",
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
                new Labeling("isotope", "N15"),
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementProteomicsValidator(sampleInformationService, terminologyService, measurementService, projectInformationService)


        when:
        def result = validator.validate(invalidMetadata, projectId)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
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
        ProteomicsMeasurementMetadata invalidMetadata = new ProteomicsMeasurementMetadata("", validSampleCode,
                "",
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
                new Labeling("isotope", "N15"),
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementProteomicsValidator(sampleInformationService, terminologyService, measurementService, projectInformationService)


        when:
        def result = validator.validate(invalidMetadata, projectId)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failures()[0] == "Organisation: missing mandatory metadata"
    }

    def "If an valid ROR ID for the organisation information is provided, the validation must pass"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        ProteomicsMeasurementMetadata validMetadata = new ProteomicsMeasurementMetadata("", validSampleCode,
                "",
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
                new Labeling("isotope", "N15"),
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementProteomicsValidator(sampleInformationService, terminologyService, measurementService, projectInformationService)


        when:
        def result = validator.validate(validMetadata, projectId)

        then:
        result.allPassed()
        !result.containsWarnings()
        !result.containsFailures()

        where:
        validRorId << [
                "https://ror.org/03a1kwz48", // University of Tübingen
                "https://ror.org/00v34f693" // QBiC
        ]
    }


    def "If no MS device Curie for the MS device information is provided, the validation must fail"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        ProteomicsMeasurementMetadata invalidMetadata = new ProteomicsMeasurementMetadata("", validSampleCode,
                "",
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "", //Illumina MiSeq
                "1",
                "The geniuses of ITSS",
                "4 Nations lived in harmony",
                "CASPASE6",
                "in solition",
                "Enrichment Index",
                "1337",
                "12",
                "LCMS Method 1",
                new Labeling("isotope", "N15"),
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementProteomicsValidator(sampleInformationService, terminologyService, measurementService, projectInformationService)


        when:
        def result = validator.validate(invalidMetadata, projectId)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failures()[0] == "MS Device: missing mandatory metadata"
    }

    def "If a valid ms device curie for the ms device information is provided, the validation must pass"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        ProteomicsMeasurementMetadata invalidMetadata = new ProteomicsMeasurementMetadata("", validSampleCode,
                "",
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                validMsDeviceCurie, //Illumina MiSeq
                "1",
                "The geniuses of ITSS",
                "4 Nations lived in harmony",
                "CASPASE6",
                "in solition",
                "Enrichment Index",
                "1337",
                "12",
                "LCMS Method 1",
                new Labeling("isotope", "N15"),
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementProteomicsValidator(sampleInformationService, terminologyService, measurementService, projectInformationService)


        when:
        def result = validator.validate(validMetadata, projectId)

        then:
        result.allPassed()
        !result.containsWarnings()
        !result.containsFailures()

        where:
        validMsDeviceCurie << [
                "EFO:0004205", // Illumina MiSeq
        ]
    }


    def "If no value was provided for the facility information the validation will fail"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        ProteomicsMeasurementMetadata invalidMetadata = new ProteomicsMeasurementMetadata("", validSampleCode,
                "",
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "1",
                "",
                "4 Nations lived in harmony",
                "CASPASE6",
                "in solition",
                "Enrichment Index",
                "1337",
                "12",
                "LCMS Method 1",
                new Labeling("isotope", "N15"),
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementProteomicsValidator(sampleInformationService, terminologyService, measurementService, projectInformationService)


        when:
        def result = validator.validate(invalidMetadata, projectId)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failures()[0] == "Facility: missing mandatory metadata"
    }

    def "If no value was provided for the fraction name information the validation will not fail"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        ProteomicsMeasurementMetadata invalidMetadata = new ProteomicsMeasurementMetadata("", validSampleCode,
                "",
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "1",
                "The geniuses of ITSS",
                "",
                "CASPASE6",
                "in solition",
                "Enrichment Index",
                "1337",
                "12",
                "LCMS Method 1",
                new Labeling("isotope", "N15"),
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementProteomicsValidator(sampleInformationService, terminologyService, measurementService, projectInformationService)


        when:
        def result = validator.validate(invalidMetadata, projectId)

        then:
        result.allPassed()
        !result.containsWarnings()
        !result.containsFailures()
    }

    def "If no value was provided for the digestion enzyme information the validation will fail"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        ProteomicsMeasurementMetadata invalidMetadata = new ProteomicsMeasurementMetadata("", validSampleCode,
                "",
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "1",
                "The geniuses of ITSS",
                "4 Nations lived in harmony",
                "",
                "in solition",
                "Enrichment Index",
                "1337",
                "12",
                "LCMS Method 1",
                new Labeling("isotope", "N15"),
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create();

        and:
        def validator = new MeasurementProteomicsValidator(sampleInformationService, terminologyService, measurementService, projectInformationService)


        when:
        def result = validator.validate(invalidMetadata, projectId)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failures()[0] == "Digestion Enzyme: missing mandatory metadata"
    }

    def "If no value was provided for the digestion method information the validation will fail"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        ProteomicsMeasurementMetadata invalidMetadata = new ProteomicsMeasurementMetadata("", validSampleCode,
                "",
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "1",
                "The geniuses of ITSS",
                "4 Nations lived in harmony",
                "CASPASE6",
                "",
                "Enrichment Index",
                "1337",
                "12",
                "LCMS Method 1",
                new Labeling("isotope", "N15"),
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementProteomicsValidator(sampleInformationService, terminologyService, measurementService, projectInformationService)


        when:
        def result = validator.validate(invalidMetadata, projectId)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failures()[0] == "Digestion Method: missing mandatory metadata"
    }

    def "If no value was provided for the enrichment method information the validation will not fail"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        ProteomicsMeasurementMetadata invalidMetadata = new ProteomicsMeasurementMetadata("", validSampleCode,
                "",
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "1",
                "The geniuses of ITSS",
                "4 Nations lived in harmony",
                "CASPASE6",
                "in solition",
                "",
                "1337",
                "12",
                "LCMS Method 1",
                new Labeling("isotope", "N15"),
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementProteomicsValidator(sampleInformationService, terminologyService, measurementService, projectInformationService)


        when:
        def result = validator.validate(invalidMetadata, projectId)

        then:
        result.allPassed()
        !result.containsWarnings()
        !result.containsFailures()
    }


    def "If no value was provided for the LC column information the validation will fail"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        ProteomicsMeasurementMetadata invalidMetadata = new ProteomicsMeasurementMetadata("", validSampleCode,
                "",
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "1",
                "The geniuses of ITSS",
                "4 Nations lived in harmony",
                "CASPASE6",
                "in solition",
                "Enrichment Index",
                "1337",
                "",
                "LCMS Method 1",
                new Labeling("isotope", "N15"),
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementProteomicsValidator(sampleInformationService, terminologyService, measurementService, projectInformationService)


        when:
        def result = validator.validate(invalidMetadata, projectId)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failures()[0] == "LC Column: missing mandatory metadata"
    }


}
