package life.qbic.projectmanagement.application

import life.qbic.application.commons.Result
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService.ExperimentalGroupDTO
import life.qbic.projectmanagement.application.sample.SampleInformationService
import life.qbic.projectmanagement.domain.model.OntologyTerm
import life.qbic.projectmanagement.domain.model.experiment.Experiment
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalValue
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalVariable
import life.qbic.projectmanagement.domain.model.experiment.repository.ExperimentRepository
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.projectmanagement.domain.model.sample.Sample
import life.qbic.projectmanagement.domain.repository.ProjectRepository
import spock.lang.Specification

class ExperimentInformationServiceSpec extends Specification {

    ExperimentRepository experimentRepository = Mock()
    ProjectRepository projectRepository = Mock()
    SampleInformationService sampleInformationService = Mock()
    ExperimentInformationService experimentInformationService = new ExperimentInformationService(experimentRepository, projectRepository, sampleInformationService)
    ProjectId projectId = ProjectId.create()

    def experiment = setupExperiment()

    def "Adding a specimen via the ExperimentInformationService adds the specimen to the experiment"() {
        given:
        experimentRepository.find(experiment.experimentId()) >> Optional.of(experiment)
        experimentRepository.find((ExperimentId) _) >> Optional.empty()

        when: "specimens are added to an experiment"
        OntologyTerm specimen1 = new OntologyTerm();
        OntologyTerm specimen2 = new OntologyTerm("ontology", "ontologyVersion",  "ontologyIri",
                "classLabel", "name", "description", "classIri");

        experimentInformationService.addSpecimenToExperiment("", experiment.experimentId(), specimen1, specimen2)

        then: "the experiment contains the added specimens"
        experiment.getSpecimens().containsAll(specimen1, specimen2)

        and: "the project is updated"
        1 * experimentRepository.update(experiment)
    }

    def "Adding an analyte via the ExperimentInformationService adds the analyte to the experiment"() {
        given:
        experimentRepository.find(experiment.experimentId()) >> Optional.of(experiment)
        experimentRepository.find((ExperimentId) _) >> Optional.empty()

        when: "analytes are added to an experiment"
        OntologyTerm analyte2 = new OntologyTerm();
        OntologyTerm analyte1 = new OntologyTerm("ontology", "ontologyVersion",  "ontologyIri",
                "classLabel", "name", "description", "classIri");

        experimentInformationService.addAnalyteToExperiment("", experiment.experimentId(), analyte1, analyte2)

        then: "the experiment contains the added analytes"
        experiment.getAnalytes().containsAll(analyte1, analyte2)

        and: "the experiment is updated"
        1 * experimentRepository.update(experiment)
    }

    def "Adding a species via the ExperimentInformationService adds the species to the experiment"() {
        given:
        experimentRepository.find(experiment.experimentId()) >> Optional.of(experiment)
        experimentRepository.find((ExperimentId) _) >> Optional.empty()

        when: "species are added to an experiment"
        OntologyTerm species1 = new OntologyTerm();
        OntologyTerm species2 = new OntologyTerm("ontology", "ontologyVersion",  "ontologyIri",
                "classLabel", "name", "description", "classIri");
        OntologyTerm species3 = new OntologyTerm();

        experimentInformationService.addSpeciesToExperiment("", experiment.experimentId(), species1, species2, species3)

        then: "the experiment contains the added species"
        experiment.getSpecies().containsAll(species1, species2, species3)

        and: "the experiment is updated"
        1 * experimentRepository.update(experiment)
    }

    def "Adding a variable via the ExperimentInformationService adds the variable to the experiment"() {
        given:
        experimentRepository.find(experiment.experimentId()) >> Optional.of(experiment)
        experimentRepository.find((ExperimentId) _) >> Optional.empty()

        when: "variables are added to an experiment"

        String variableName = "My awesome variable"
        String unit = "This unit exists"
        def levels = ["level 1", "level 2"]
        ExperimentalValue experimentalValue1 = ExperimentalValue.create(levels[0], unit)
        ExperimentalValue experimentalValue2 = ExperimentalValue.create(levels[1], unit)
        ExperimentalVariable experimentalVariable = ExperimentalVariable.create(variableName, experimentalValue1, experimentalValue2)

        experimentInformationService.addVariableToExperiment("", experiment.experimentId(), variableName, unit, levels)

        then: "the experiment contains the added variables"
        experiment.variables().contains(experimentalVariable)

        and: "the experiment is updated"
        1 * experimentRepository.update(experiment)
    }

    def "Adding experimental groups via the ExperimentInformationService adds the groups to the experiment"() {
        given:
        experimentRepository.find(experiment.experimentId()) >> Optional.of(experiment)
        experimentRepository.find((ExperimentId) _) >> Optional.empty()
        sampleInformationService.retrieveSamplesForExperiment((ExperimentId) _) >> Result.fromValue(new ArrayList<Sample>())

        and: "an experiment with a variable"

        String variableName = "My awesome variable"
        String unit = "This unit exists"
        ExperimentalValue experimentalValueOne = ExperimentalValue.create("level 1", unit)
        ExperimentalValue experimentalValueTwo = ExperimentalValue.create("level 2", unit)
        ExperimentalVariable experimentalVariable = ExperimentalVariable.create(variableName, experimentalValueOne, experimentalValueTwo)
        experiment.addVariableToDesign(experimentalVariable.name().value(), experimentalVariable.levels().collect { it.experimentalValue() })

        when: "experimental groups are added to an experiment"
        def group1 = new ExperimentalGroupDTO(-1, "name1", List.of(experimentalVariable.levels().get(0)), 5)
        def group2 = new ExperimentalGroupDTO(-1, "name2", List.of(experimentalVariable.levels().get(1)), 6)

        experimentInformationService.updateExperimentalGroupsOfExperiment("", experiment.experimentId(), Arrays.asList(group1))

        then: "the experiment contains the added experimental groups"
        def dtoGroups = experiment.getExperimentalGroups().stream().map(it -> new ExperimentalGroupDTO(-1, it.name(), it.condition().getVariableLevels(), it.sampleSize())).toList()
        dtoGroups.contains(group1)

        and: "the experiment is updated once for adding the experimental group"
        1 * experimentRepository.update(experiment)
    }

    private static Experiment setupExperiment() {
        Experiment experiment = Experiment.create("Dummy_Experiment")
        return experiment
    }
}
