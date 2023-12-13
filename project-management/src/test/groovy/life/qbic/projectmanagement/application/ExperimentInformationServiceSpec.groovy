package life.qbic.projectmanagement.application

import life.qbic.projectmanagement.application.ExperimentInformationService.ExperimentalGroupDTO
import life.qbic.projectmanagement.domain.model.experiment.Experiment
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalValue
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalVariable
import life.qbic.projectmanagement.domain.model.experiment.repository.ExperimentRepository
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.OntologyClassDTO
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen
import life.qbic.projectmanagement.domain.repository.ProjectRepository
import spock.lang.Specification

class ExperimentInformationServiceSpec extends Specification {

    ExperimentRepository experimentRepository = Mock()
    ProjectRepository projectRepository = Mock()
    ExperimentInformationService experimentInformationService = new ExperimentInformationService(experimentRepository, projectRepository)

    def experiment = setupExperiment()

    def "Adding a specimen via the ExperimentInformationService adds the specimen to the experiment"() {
        given:
        experimentRepository.find(experiment.experimentId()) >> Optional.of(experiment)
        experimentRepository.find((ExperimentId) _) >> Optional.empty()

        when: "specimens are added to an experiment"
        OntologyClassDTO specimen1 = new OntologyClassDTO();
        OntologyClassDTO specimen2 = new OntologyClassDTO("ontology", "ontologyVersion",  "ontologyIri",
                 "label",  "name",  "description",  "classIri");
        experimentInformationService.addSpecimenToExperiment(experiment.experimentId(), specimen1, specimen2)

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
        OntologyClassDTO analyte2 = new OntologyClassDTO();
        OntologyClassDTO analyte1 = new OntologyClassDTO("ontology", "ontologyVersion",  "ontologyIri",
                "label",  "name",  "description",  "classIri");
        experimentInformationService.addAnalyteToExperiment(experiment.experimentId(), analyte1, analyte2)

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
        OntologyClassDTO species1 = new OntologyClassDTO();
        OntologyClassDTO species2 = new OntologyClassDTO("ontology", "ontologyVersion",  "ontologyIri",
                "label",  "name",  "description",  "classIri");
        OntologyClassDTO species3 = new OntologyClassDTO();

        experimentInformationService.addSpeciesToExperiment(experiment.experimentId(), species1, species2, species3)

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
        experimentInformationService.addVariableToExperiment(experiment.experimentId(), variableName, unit, levels)

        then: "the experiment contains the added variables"
        experiment.variables().contains(experimentalVariable)

        and: "the experiment is updated"
        1 * experimentRepository.update(experiment)
    }

    def "Adding experimental groups via the ExperimentInformationService adds the groups to the experiment"() {
        given:
        experimentRepository.find(experiment.experimentId()) >> Optional.of(experiment)
        experimentRepository.find((ExperimentId) _) >> Optional.empty()

        and: "an experiment with a variable"

        String variableName = "My awesome variable"
        String unit = "This unit exists"
        ExperimentalValue experimentalValueOne = ExperimentalValue.create("level 1", unit)
        ExperimentalValue experimentalValueTwo = ExperimentalValue.create("level 2", unit)
        ExperimentalVariable experimentalVariable = ExperimentalVariable.create(variableName, experimentalValueOne, experimentalValueTwo)
        experiment.addVariableToDesign(experimentalVariable.name().value(), experimentalVariable.levels().collect { it.experimentalValue() })

        when: "experimental groups are added to an experiment"
        def group1 = new ExperimentalGroupDTO(Set.of(experimentalVariable.levels().get(0)), 5)
        def group2 = new ExperimentalGroupDTO(Set.of(experimentalVariable.levels().get(1)), 6)

        experimentInformationService.addExperimentalGroupToExperiment(experiment.experimentId(), group1)


        then: "the experiment contains the added experimental groups"
        def dtoGroups = experiment.getExperimentalGroups().stream().map(it -> new ExperimentalGroupDTO(it.condition().getVariableLevels(), it.sampleSize())).toList()
        dtoGroups.contains(group1)

        and: "the experiment is updated once for adding the variable and once for adding the experimental groups"
        1 * experimentRepository.update(experiment)
    }

    private static Experiment setupExperiment() {
        Experiment experiment = Experiment.create("Dummy_Experiment")
        return experiment
    }
}
