package life.qbic.projectmanagement.application.measurement.validation


import life.qbic.projectmanagement.application.ProjectInformationService
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificNGS
import life.qbic.projectmanagement.application.measurement.MeasurementService
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata
import life.qbic.projectmanagement.application.ontology.OntologyClass
import life.qbic.projectmanagement.application.ontology.TerminologyService
import life.qbic.projectmanagement.application.sample.SampleInformationService
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.project.Project
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.projectmanagement.domain.model.sample.SampleCode
import spock.lang.Specification

import java.util.stream.Collectors

class MeasurementNGSValidatorSpec extends Specification {

    final static NGSMeasurementMetadata validMetadata = new NGSMeasurementMetadata("", "", [SampleCode.create("QTEST001AE")],
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

    def "A comment longer than 255 characters fails registration validation"() {
        given: "a validator whose other checks cannot pass for this sample"
        def experimentId = ExperimentId.create()
        def projectId = ProjectId.create()
        def sampleInformation = Mock(SampleInformationService) {
            findSampleId(_ as SampleCode) >> Optional.empty()
            retrieveSamplesForExperiment(_ as ProjectId, _ as String) >> []
        }
        def terminology = Mock(TerminologyService) {
            findByCurie(_) >> Optional.empty()
        }
        def projectInformation = Mock(ProjectInformationService) {
            find(_ as ProjectId) >> Optional.of(Mock(Project) {
                experiments() >> [experimentId]
            })
        }
        def validator = new MeasurementNGSValidator(sampleInformation, terminology,
                projectInformation, Mock(MeasurementService))

        and: "a registration with a 256 character comment"
        def registration = new MeasurementRegistrationInformationNGS(
                "https://ror.org/03a1kwz48", "EFO:0004205", "facility",
                "read type", "library kit", "flow cell", "run protocol", "pool group",
                Map.of("QTEST001AE",
                        new MeasurementSpecificNGS("i7", "i5", "c" * 256)),
                "measurement name")

        when:
        def result = validator.validateRegistration(registration, experimentId.value(), projectId)

        then: "the length failure is reported and names the field and the sample"
        result.failures().contains(
                "Comment: exceeds the maximum length of 255 characters for 'QTEST001AE' (got 256)")
    }

    def "A comment of exactly 255 characters does not fail the length validation"() {
        given: "a registration with a 255 character comment"
        def experimentId = ExperimentId.create()
        def projectId = ProjectId.create()
        def validator = new MeasurementNGSValidator(
                Mock(SampleInformationService) {
                    findSampleId(_ as SampleCode) >> Optional.empty()
                    retrieveSamplesForExperiment(_ as ProjectId, _ as String) >> []
                },
                Mock(TerminologyService) { findByCurie(_) >> Optional.empty() },
                Mock(ProjectInformationService) {
                    find(_ as ProjectId) >> Optional.of(Mock(Project) {
                        experiments() >> [experimentId]
                    })
                },
                Mock(MeasurementService))

        def registration = new MeasurementRegistrationInformationNGS(
                "https://ror.org/03a1kwz48", "EFO:0004205", "facility",
                "read type", "library kit", "flow cell", "run protocol", "pool group",
                Map.of("QTEST001AE",
                        new MeasurementSpecificNGS("i7", "i5", "c" * 255)),
                "measurement name")

        when:
        def result = validator.validateRegistration(registration, experimentId.value(), projectId)

        then: "no length failure is reported"
        !result.failures().any { it.startsWith("Comment:") }
    }

}
