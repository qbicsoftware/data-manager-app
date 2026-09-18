package life.qbic.projectmanagement.application.sample

import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService
import life.qbic.projectmanagement.application.ontology.OntologyClass
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService
import life.qbic.projectmanagement.application.ontology.TerminologyService
import life.qbic.projectmanagement.domain.model.OntologyTerm
import life.qbic.projectmanagement.domain.model.experiment.Experiment
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalValue
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel
import life.qbic.projectmanagement.domain.model.experiment.VariableName
import life.qbic.projectmanagement.domain.model.project.ProjectId
import spock.lang.Specification

class SampleValidationSpec extends Specification {

    SampleInformationService sampleInformationService = Stub()
    ExperimentInformationService experimentInformationService = Stub()
    TerminologyService terminologyService = Stub()
    SpeciesLookupService speciesLookupService = Stub()
    ConfoundingVariableService confoundingVariableService = Stub()

    SampleValidation sampleValidation = new SampleValidation(
            sampleInformationService,
            experimentInformationService,
            terminologyService,
            speciesLookupService,
            confoundingVariableService)

    ProjectId projectId = ProjectId.create()

    def experiment = setupExperiment()

    def setup() {
        experimentInformationService.find(_ as String, _ as ExperimentId) >> Optional.of(experiment)
        terminologyService.findByCurie(_) >> Optional.of(ontologyTerm())
        speciesLookupService.findByCURI(_) >> Optional.of(ontologyClass())
        confoundingVariableService.listConfoundingVariablesForExperiment(_, _) >> []
    }

    def "A blank batch in validateNewSample produces a Missing batch failure"() {
        when: "a new sample is validated with a blank batch"
        def result = sampleValidation.validateNewSample(
                "sampleName", "1", "Color: red", "Homo sapiens [NCBITaxon:9606]",
                "specimen [UBERON:000006]",
                "analyte [CHEBI:1234]", "WGS", "comment",
                [:], "  ", projectId.value(), experiment.experimentId().value())

        then: "the validation fails and reports a missing batch"
        result.validationResult().containsFailures()
        result.validationResult().failures().contains("Missing batch")
    }

    def "A null batch in validateNewSample produces a Missing batch failure"() {
        when: "a new sample is validated with a null batch"
        def result = sampleValidation.validateNewSample(
                "sampleName", "1", "Color: red", "Homo sapiens [NCBITaxon:9606]",
                "specimen [UBERON:000006]",
                "analyte [CHEBI:1234]", "WGS", "comment",
                [:], null, projectId.value(), experiment.experimentId().value())

        then: "the validation fails and reports a missing batch"
        result.validationResult().containsFailures()
        result.validationResult().failures().contains("Missing batch")
    }

    def "A non-blank batch in validateNewSample does not produce a Missing batch failure"() {
        when: "a new sample is validated with a non-blank batch"
        def result = sampleValidation.validateNewSample(
                "sampleName", "1", "Color: red", "Homo sapiens [NCBITaxon:9606]",
                "specimen [UBERON:000006]",
                "analyte [CHEBI:1234]", "WGS", "comment",
                [:], "batch-1", projectId.value(), experiment.experimentId().value())

        then: "the validation does not report a missing batch"
        !result.validationResult().failures().contains("Missing batch")
    }

    private static Experiment setupExperiment() {
        Experiment experiment = Experiment.create("Dummy_Experiment")
        def variableName = VariableName.create("Color")
        experiment.addVariableToDesign(variableName.value(),
                List.of(ExperimentalValue.create("red")))
        def variableLevel = VariableLevel.create(variableName, ExperimentalValue.create("red"))
        def addGroupResult = experiment.addExperimentalGroup("group-1", List.of(variableLevel), 5)
        if (addGroupResult.isError()) {
            throw new IllegalStateException("Could not add experimental group")
        }
        experiment.getExperimentalGroups().forEach { it.experimentalGroupId = 1L }
        return experiment
    }

    private static OntologyTerm ontologyTerm() {
        new OntologyTerm("UBERON", "1.0", "http://example.org", "specimen",
                "UBERON:000006", "a specimen", "http://example.org/UBERON_000006")
    }

    private static OntologyClass ontologyClass() {
        new OntologyClass("NCBITaxon", "1.0", "http://example.org", "Homo sapiens",
                "NCBITaxon:9606", "human", "http://example.org/NCBITaxon_9606")
    }
}