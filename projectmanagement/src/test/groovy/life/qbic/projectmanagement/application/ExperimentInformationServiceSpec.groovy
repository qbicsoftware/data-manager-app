package life.qbic.projectmanagement.application


import life.qbic.projectmanagement.domain.project.experiment.Experiment
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalValue
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable
import life.qbic.projectmanagement.domain.project.experiment.repository.ExperimentRepository
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen
import spock.lang.Specification

class ExperimentInformationServiceSpec extends Specification {

  ExperimentRepository experimentRepository = Mock()
  ExperimentInformationService experimentInformationService = new ExperimentInformationService(experimentRepository)

  def experiment = setupExperiment()

  def "Adding a specimen via the ExperimentInformationService adds the specimen to the experiment"() {
    given:
    experimentRepository.find(experiment.experimentId()) >> Optional.of(experiment)
    experimentRepository.find((ExperimentId) _) >> Optional.empty()

    when: "specimens are added to an experiment"
    Specimen specimen1 = Specimen.create("blood")
    Specimen specimen2 = Specimen.create("plasma")
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
    Analyte analyte1 = Analyte.create("blood")
    Analyte analyte2 = Analyte.create("plasma")
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
    Species species1 = Species.create("blood")
    Species species2 = Species.create("plasma")
    experimentInformationService.addSpeciesToExperiment(experiment.experimentId(), species1, species2)

    then: "the experiment contains the added species"
    experiment.getSpecies().containsAll(species1, species2)

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
    ExperimentalValue experimentalValueWithUnit = ExperimentalValue.create(levels[0], unit)
    ExperimentalValue experimentalValueWithoutUnit = ExperimentalValue.create(levels[1])
    ExperimentalVariable experimentalVariable = ExperimentalVariable.create(variableName, experimentalValueWithUnit, experimentalValueWithoutUnit)
    experimentInformationService.addVariableToExperiment(experiment.experimentId(), variableName, unit, levels)

    then: "the experiment contains the added variables"
    experiment.variables().contains(experimentalVariable)

    and: "the experiment is updated"
    1 * experimentRepository.update(experiment)
  }


  private static Experiment setupExperiment() {
    Experiment experiment = Experiment.create("Dummy_Experiment")
    return experiment
  }
}
