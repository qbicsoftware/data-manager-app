package life.qbic.projectmanagement.application.measurement.validation

import life.qbic.projectmanagement.application.ProjectInformationService
import life.qbic.projectmanagement.application.measurement.MeasurementService
import life.qbic.projectmanagement.application.api.NGSMeasurementMetadata
import life.qbic.projectmanagement.application.ontology.OntologyClass
import life.qbic.projectmanagement.application.ontology.TerminologyService
import life.qbic.projectmanagement.application.sample.SampleInformationService
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.projectmanagement.domain.model.sample.SampleCode
import spock.lang.Specification

import java.util.stream.Collectors

class MeasurementNGSValidatorSpec extends Specification {

    final static NGSMeasurementMetadata validMetadata = new NGSMeasurementMetadata("", [SampleCode.create("QTEST001AE")],
            "https://ror.org/03a1kwz48", //Universität Tübingen,
            "EFO:0004205", //Illumina MiSeq
            "My awesome facility",
            "Unknown sequencing read type",
            "Cool library kit",
            "Bodacious flow cell",
            "We do it by the book",
            "We need groups",
            "Index I7 never stops",
            "Index I5 always goes",
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
        findByCurie(validMetadata.instrumentCURI()) >> Optional.of(illuminaMiSeq)
    })

    final MeasurementService measurementService = Mock(MeasurementService.class)


    final ProjectInformationService projectInformationService = Mock(ProjectInformationService.class)

    final static List<String> validNGSProperties = MeasurementNGSValidator.NGS_PROPERTY.values().collect { it.label() }


    def "A complete property set must be valid no matter the letter casing style"() {

        when:
        def isNGSMetadata = MeasurementNGSValidator.isNGS(chaosCasing)

        then:
        isNGSMetadata
        where:
        chaosCasing << [
                validNGSProperties.collect { it.toUpperCase() },
                validNGSProperties.collect { it.toLowerCase() }]
    }


    def "Given a valid ngs measurement metadata property collection, pass the validation"() {
        given:

        when:
        def isNGSMetadata = MeasurementNGSValidator.isNGS(validNGSProperties)

        then:
        isNGSMetadata

    }

    def "Missing properties for the ngs metadata collection must result in an unsuccessful validation"() {
        given:
        // we just take the NGS property list
        def missingProperties = validNGSProperties.stream().collect(Collectors.toList())
        missingProperties.remove(0)

        when:
        def isNGSMetadata = MeasurementNGSValidator.isNGS(missingProperties)

        then:
        !isNGSMetadata
    }

    def "Providing no properties for the NGS metadata collection must result in an unsuccessful validation"() {
        given:
        def missingProperties = []

        when:
        def isNGSMetadata = MeasurementNGSValidator.isNGS(missingProperties)

        then:
        !isNGSMetadata
    }


    def "Valid entries in a ngs measurement metadata object must return a successful validation "() {
        given:
        def validMeasurementEntry = validMetadata

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(_ as SampleCode) >> Optional.of(_)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementNGSValidator(sampleInformationService, terminologyService, projectInformationService, measurementService)

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

    def "An unknown sample code in a ngs measurement metadata object must return a failed validation "() {
        given:
        NGSMeasurementMetadata invalidMeasurementMetadata = new NGSMeasurementMetadata("", [SampleCode.create("QNKWN001AE")],
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "My awesome facility",
                "Unknown sequencing read type",
                "Cool library kit",
                "Bodacious flow cell",
                "We do it by the book",
                "We need groups",
                "Index I7 never stops",
                "Index I5 always goes",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(_ as SampleCode) >> Optional.empty()
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementNGSValidator(sampleInformationService, terminologyService, projectInformationService, measurementService)

        when:
        def result = validator.validate(invalidMeasurementMetadata, projectId)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failures()[0] == "Unknown sample with sample id \"QNKWN001AE\""
    }

    def "An unknown sample code when several are provided in a ngs measurement metadata object must return a failed validation "() {
        given:
        def sampleToBeFound = SampleCode.create("QNWBY999AE")
        def unknownSample = SampleCode.create("QNKWN001AE")

        and:
        def invalidMeasurementMetadata = new NGSMeasurementMetadata("", [sampleToBeFound, unknownSample],
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "My awesome facility",
                "Unknown sequencing read type",
                "Cool library kit",
                "Bodacious flow cell",
                "We do it by the book",
                "We need groups",
                "Index I7 never stops",
                "Index I5 always goes",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(unknownSample) >> Optional.empty()
        sampleInformationService.findSampleId(sampleToBeFound) >> Optional.of(sampleToBeFound)
        ProjectId projectId = ProjectId.create()
        and:
        def validator = new MeasurementNGSValidator(sampleInformationService, terminologyService, projectInformationService, measurementService)

        when:
        def result = validator.validate(invalidMeasurementMetadata, projectId)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failures()[0] == "Unknown sample with sample id \"QNKWN001AE\""
    }

    def "If no sample code is provided, the validation must fail"() {
        given:
        def invalidMeasurementMetadata = new NGSMeasurementMetadata("", [],
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "My awesome facility",
                "Unknown sequencing read type",
                "Cool library kit",
                "Bodacious flow cell",
                "We do it by the book",
                "We need groups",
                "Index I7 never stops",
                "Index I5 always goes",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementNGSValidator(sampleInformationService, terminologyService, projectInformationService, measurementService)

        when:
        def result = validator.validate(invalidMeasurementMetadata, projectId)

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
        def invalidMeasurementMetadata = new NGSMeasurementMetadata("", [validSampleCode],
                invalidRorId, //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "My awesome facility",
                "Unknown sequencing read type",
                "Cool library kit",
                "Bodacious flow cell",
                "We do it by the book",
                "We need groups",
                "Index I7 never stops",
                "Index I5 always goes",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementNGSValidator(sampleInformationService, terminologyService, projectInformationService, measurementService)


        when:
        def result = validator.validate(invalidMeasurementMetadata, projectId)

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
        def invalidMeasurementMetadata = new NGSMeasurementMetadata("", [validSampleCode],
                "", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "My awesome facility",
                "Unknown sequencing read type",
                "Cool library kit",
                "Bodacious flow cell",
                "We do it by the book",
                "We need groups",
                "Index I7 never stops",
                "Index I5 always goes",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)

        and:
        def validator = new MeasurementNGSValidator(sampleInformationService, terminologyService, projectInformationService, measurementService)
        ProjectId projectId = ProjectId.create()


        when:
        def result = validator.validate(invalidMeasurementMetadata, projectId)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failures()[0] == "Organisation: missing mandatory metadata"
    }

    def "If an valid ROR ID for the organisation information is provided, the validation must pass"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        def invalidMeasurementMetadata = new NGSMeasurementMetadata("", [],
                validRorId, //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "My awesome facility",
                "Unknown sequencing read type",
                "Cool library kit",
                "Bodacious flow cell",
                "We do it by the book",
                "We need groups",
                "Index I7 never stops",
                "Index I5 always goes",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)

        and:
        def validator = new MeasurementNGSValidator(sampleInformationService, terminologyService, projectInformationService, measurementService)
        ProjectId projectId = ProjectId.create()


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

    def "If no instrument Curie for the instrument information is provided, the validation must fail"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        def invalidMeasurementMetadata = new NGSMeasurementMetadata("", [validSampleCode],
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "", //Illumina MiSeq
                "My awesome facility",
                "Unknown sequencing read type",
                "Cool library kit",
                "Bodacious flow cell",
                "We do it by the book",
                "We need groups",
                "Index I7 never stops",
                "Index I5 always goes",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)

        and:
        def validator = new MeasurementNGSValidator(sampleInformationService, terminologyService, projectInformationService, measurementService)
        ProjectId projectId = ProjectId.create()

        when:
        def result = validator.validate(invalidMeasurementMetadata, projectId)


        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failures()[0] == "Instrument: missing mandatory metadata"
    }

    def "If a valid instrument curie for the instrument information is provided, the validation must pass"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        def validMeasurementMetadata = new NGSMeasurementMetadata("", [validSampleCode],
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                validInstrumentCurie, //Illumina MiSeq
                "My awesome facility",
                "Unknown sequencing read type",
                "Cool library kit",
                "Bodacious flow cell",
                "We do it by the book",
                "We need groups",
                "Index I7 never stops",
                "Index I5 always goes",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementNGSValidator(sampleInformationService, terminologyService, projectInformationService, measurementService)


        when:
        def result = validator.validate(validMetadata, projectId)

        then:
        result.allPassed()
        !result.containsWarnings()
        !result.containsFailures()

        where:
        validInstrumentCurie << [
                "EFO:0004205", // Illumina MiSeq
        ]
    }

    def "If no value was provided for the facility information the validation will fail"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        def invalidMeasurementMetadata = new NGSMeasurementMetadata("", [validSampleCode],
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "",
                "Unknown sequencing read type",
                "Cool library kit",
                "Bodacious flow cell",
                "We do it by the book",
                "We need groups",
                "Index I7 never stops",
                "Index I5 always goes",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementNGSValidator(sampleInformationService, terminologyService, projectInformationService, measurementService)


        when:
        def result = validator.validate(invalidMeasurementMetadata, projectId)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failures()[0] == "Facility: missing mandatory metadata"
    }

    def "If no value was provided for the sequencing read type information the validation will fail"() {
        given:
        SampleCode validSampleCode = SampleCode.create("QTEST001AE")
        def invalidMeasurementMetadata = new NGSMeasurementMetadata("", [validSampleCode],
                "https://ror.org/03a1kwz48", //Universität Tübingen,
                "EFO:0004205", //Illumina MiSeq
                "My awesome facility",
                "",
                "Cool library kit",
                "Bodacious flow cell",
                "We do it by the book",
                "We need groups",
                "Index I7 never stops",
                "Index I5 always goes",
                "Don't tell anyone this is a test"
        )

        and:
        SampleInformationService sampleInformationService = Mock(SampleInformationService.class)
        sampleInformationService.findSampleId(validSampleCode) >> Optional.of(validSampleCode)
        ProjectId projectId = ProjectId.create()

        and:
        def validator = new MeasurementNGSValidator(sampleInformationService, terminologyService, projectInformationService, measurementService)


        when:
        def result = validator.validate(invalidMeasurementMetadata, projectId)

        then:
        !result.allPassed()
        !result.containsWarnings()
        result.containsFailures()
        result.failures()[0] == "Read Type: missing mandatory metadata"
    }
}
