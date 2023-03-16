package life.qbic.projectmanagement.application;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.repository.ExperimentRepository;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that provides an API to query basic project information
 *
 * @since 1.0.0
 */
@Service
public class ExperimentInformationService {

  private static final Logger log = LoggerFactory.logger(ExperimentInformationService.class);
  private final ExperimentRepository experimentRepository;

  public ExperimentInformationService(@Autowired ExperimentRepository experimentRepository) {
    this.experimentRepository = experimentRepository;
  }

  public Experiment loadExperimentById(ExperimentId experimentId) {
    Objects.requireNonNull(experimentId);
    return experimentRepository.find(experimentId).orElseThrow(
        () -> new ProjectManagementException("The active experiment does not exist anymore.")
        // should never happen; indicates dirty removal of experiment from db
    );
  }

  /**
   * Adds species to an experiment.
   *
   * @param experimentId the Id of the experiment for which to add the species
   * @param species      the species to add
   * @see Experiment#addSpecies(Collection)
   */
  public void addSpeciesToExperiment(ExperimentId experimentId, Species... species) {
    if (species.length < 1) {
      return;
    }
    Arrays.stream(species).forEach(Objects::requireNonNull);
    Experiment activeExperiment = loadExperimentById(experimentId);
    activeExperiment.addSpecies(List.of(species));
    experimentRepository.update(activeExperiment);
  }

  /**
   * Adds specimens to an experiment
   *
   * @param experimentId the Id of the experiment for which to add the specimen
   * @param specimens    the specimens to add
   * @see Experiment#addSpecimens(Collection)
   */
  public void addSpecimenToExperiment(ExperimentId experimentId, Specimen... specimens) {
    if (specimens.length < 1) {
      return;
    }
    for (Specimen specimen : specimens) {
      Objects.requireNonNull(specimen);
    }
    Experiment activeExperiment = loadExperimentById(experimentId);
    activeExperiment.addSpecimens(List.of(specimens));
    experimentRepository.update(activeExperiment);
  }

  /**
   * Adds analytes to an experiment
   *
   * @param experimentId the Id of the experiment for which to add the analyte
   * @param analytes     the analytes to add
   * @see Experiment#addAnalytes(Collection)
   */
  public void addAnalyteToExperiment(ExperimentId experimentId, Analyte... analytes) {
    if (analytes.length < 1) {
      return;
    }
    Arrays.stream(analytes).forEach(Objects::requireNonNull);
    Experiment activeExperiment = loadExperimentById(experimentId);
    activeExperiment.addAnalytes(List.of(analytes));
    experimentRepository.update(activeExperiment);
  }

  /**
   * Retrieve all analytes of an experiment.
   *
   * @param experimentId the Id of the experiment for which the analytes should be retrieved
   * @return a collection of analytes in the active experiment.
   */
  public Collection<Analyte> getAnalytesOfExperiment(ExperimentId experimentId) {
    Experiment experiment = loadExperimentById(experimentId);
    return experiment.getAnalytes();
  }


  /**
   * Retrieve all species of an experiment.
   *
   * @param experimentId the Id of the experiment for which the species should be retrieved
   * @return a collection of species in the active experiment.
   */
  public Collection<Species> getSpeciesOfExperiment(ExperimentId experimentId) {
    Experiment experiment = loadExperimentById(experimentId);
    return experiment.getSpecies();
  }

  /**
   * Retrieve all specimen of an experiment.
   *
   * @param experimentId the Id of the experiment for which the specimen should be retrieved
   * @return a collection of specimen in the active experiment.
   */
  public Collection<Specimen> getSpecimensOfExperiment(ExperimentId experimentId) {
    Experiment experiment = loadExperimentById(experimentId);
    return experiment.getSpecimens();
  }

}
