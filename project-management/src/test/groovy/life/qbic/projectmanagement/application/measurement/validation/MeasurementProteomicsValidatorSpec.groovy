package life.qbic.projectmanagement.application.measurement.validation


import life.qbic.projectmanagement.application.ProjectInformationService
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificPxP
import life.qbic.projectmanagement.application.measurement.Labeling
import life.qbic.projectmanagement.application.measurement.MeasurementService
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata
import life.qbic.projectmanagement.application.ontology.OntologyClass
import life.qbic.projectmanagement.application.ontology.TerminologyService
import life.qbic.projectmanagement.application.sample.SampleInformationService
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.project.Project
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.projectmanagement.domain.model.sample.SampleCode
import spock.lang.Specification

import java.util.stream.Collectors

class MeasurementMeasurementProteomicsValidatorSpec extends Specification {

    final static ProteomicsMeasurementMetadata validMetadata = new ProteomicsMeasurementMetadata("", "", SampleCode.create("QTEST001AE"),
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

    def "A comment longer than 255 characters fails registration validation"() {
        given: "a validator whose other checks cannot pass for this sample"
        def experimentId = ExperimentId.create()
        def projectId = ProjectId.create()
        def validator = new MeasurementProteomicsValidator(
                Mock(SampleInformationService) {
                    findSampleId(_ as SampleCode) >> Optional.empty()
                    findSample(_ as ProjectId, _ as String) >> Optional.empty()
                    retrieveSamplesForExperiment(_ as ProjectId, _ as String) >> []
                },
                Mock(TerminologyService) { findByCurie(_) >> Optional.empty() },
                Mock(MeasurementService),
                Mock(ProjectInformationService) {
                    find(_ as ProjectId) >> Optional.of(Mock(Project) {
                        experiments() >> [experimentId]
                    })
                })

        and: "a registration with a 256 character comment"
        def registration = new MeasurementRegistrationInformationPxP(
                "replicate", "https://ror.org/03a1kwz48", "EFO:0004205", "pool group",
                "facility", "enzyme", "method", "enrichment", "1", "column", "lcms method",
                "labeling type",
                Map.of("QTEST001AE",
                        new MeasurementSpecificPxP("label", "fraction", "c" * 256)),
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
        def validator = new MeasurementProteomicsValidator(
                Mock(SampleInformationService) {
                    findSampleId(_ as SampleCode) >> Optional.empty()
                    findSample(_ as ProjectId, _ as String) >> Optional.empty()
                    retrieveSamplesForExperiment(_ as ProjectId, _ as String) >> []
                },
                Mock(TerminologyService) { findByCurie(_) >> Optional.empty() },
                Mock(MeasurementService),
                Mock(ProjectInformationService) {
                    find(_ as ProjectId) >> Optional.of(Mock(Project) {
                        experiments() >> [experimentId]
                    })
                })

        def registration = new MeasurementRegistrationInformationPxP(
                "replicate", "https://ror.org/03a1kwz48", "EFO:0004205", "pool group",
                "facility", "enzyme", "method", "enrichment", "1", "column", "lcms method",
                "labeling type",
                Map.of("QTEST001AE",
                        new MeasurementSpecificPxP("label", "fraction", "c" * 255)),
                "measurement name")

        when:
        def result = validator.validateRegistration(registration, experimentId.value(), projectId)

        then: "no length failure is reported"
        !result.failures().any { it.startsWith("Comment:") }
    }

}
