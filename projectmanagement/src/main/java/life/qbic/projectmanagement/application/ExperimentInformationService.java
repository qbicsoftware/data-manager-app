package life.qbic.projectmanagement.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import life.qbic.application.commons.ApplicationException;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign.AddExperimentalGroupResponse;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign.AddExperimentalGroupResponse.ResponseCode;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalValue;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.project.experiment.repository.ExperimentRepository;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that provides an API to query basic experiment information
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

  public Optional<Experiment> find(ExperimentId experimentId) {
    Objects.requireNonNull(experimentId);
    log.debug("Search for experiment with id: " + experimentId.value());
    return experimentRepository.find(experimentId);
  }

  private Experiment loadExperimentById(ExperimentId experimentId) {
    Objects.requireNonNull(experimentId);
    return experimentRepository.find(experimentId).orElseThrow(
        () -> new ApplicationException(
            "Experiment with id" + experimentId.value() + "does not exit anymore")
        // should never happen; indicates dirty removal of experiment from db
    );
  }

  /**
   * Add sample groups to the experiment
   *
   * @param experimentId      the Id of the experiment for which to add the species
   * @param experimentalGroup the experimental groups to add
   */
  public AddExperimentalGroupResponse addExperimentalGroupToExperiment(
      ExperimentId experimentId, ExperimentalGroupDTO experimentalGroup) {
    Objects.requireNonNull(experimentalGroup, "experimental group must not be null");
    Objects.requireNonNull(experimentId, "experiment id must not be null");

    Experiment activeExperiment = loadExperimentById(experimentId);
    AddExperimentalGroupResponse response = activeExperiment.addExperimentalGroup(
        experimentalGroup.levels(), experimentalGroup.sampleSize());
    if (response.responseCode() == ResponseCode.SUCCESS) {
      experimentRepository.update(activeExperiment);
    }
    return response;
  }

  /**
   * Retrieve all analytes of an experiment.
   *
   * @param experimentId the Id of the experiment for which the experimental groups should be
   *                     retrieved
   * @return the list of experimental groups in the active experiment.
   */
  public List<ExperimentalGroupDTO> getExperimentalGroups(ExperimentId experimentId) {
    Experiment experiment = loadExperimentById(experimentId);
    return experiment.getExperimentalGroups().stream()
        .map(it -> new ExperimentalGroupDTO(it.condition().getVariableLevels(), it.sampleSize()))
        .toList();
  }

  public List<ExperimentalGroup> experimentalGroupsFor(ExperimentId experimentId) {
    Experiment experiment = loadExperimentById(experimentId);
    return experiment.getExperimentalGroups().stream().toList();
  }

  public void deleteExperimentGroup(ExperimentId experimentId, long groupId) {
    Experiment experiment = loadExperimentById(experimentId);
    experiment.removeExperimentGroup(groupId);
    experimentRepository.update(experiment);
  }


  public record ExperimentalGroupDTO(Set<VariableLevel> levels, int sampleSize) {

  }

  /**
   * Adds species to an experiment.
   *
   * @param experimentId the Id of the experiment for which to add the species
   * @param species      the species to add
   * @see Experiment#addSpecies(Collection)
   */
  public void addSpeciesToExperiment(ExperimentId experimentId, Species... species) {
    Arrays.stream(species).forEach(Objects::requireNonNull);
    if (species.length < 1) {
      return;
    }
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
    Arrays.stream(specimens).forEach(Objects::requireNonNull);
    if (specimens.length < 1) {
      return;
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
    Arrays.stream(analytes).forEach(Objects::requireNonNull);
    if (analytes.length < 1) {
      return;
    }
    Experiment activeExperiment = loadExperimentById(experimentId);
    activeExperiment.addAnalytes(List.of(analytes));
    experimentRepository.update(activeExperiment);
  }

  /**
   * Adds {@link ExperimentalVariable} to an {@link Experiment}
   *
   * @param experimentId the Id of the experiment
   * @param variableName the name of the variable to be added
   * @param unit         the optionally defined unit for the {@link ExperimentalValue} within the
   *                     {@link ExperimentalVariable}
   * @param levels       String based list of levels from each of which the
   *                     {@link ExperimentalValue} will be derived for the to be defined
   *                     {@link ExperimentalVariable}
   */
  public void addVariableToExperiment(ExperimentId experimentId, String variableName, String unit,
      List<String> levels) {
    Objects.requireNonNull(variableName);
    Objects.requireNonNull(levels);
    if (levels.isEmpty()) {
      return;
    }
    Experiment activeExperiment = loadExperimentById(experimentId);
    List<ExperimentalValue> experimentalValues = new ArrayList<>();
    for (String level : levels) {
      ExperimentalValue experimentalValue = (unit.isBlank()) ? ExperimentalValue.create(level)
          : ExperimentalValue.create(level, unit);
      experimentalValues.add(experimentalValue);
    }
    activeExperiment.addVariableToDesign(variableName, experimentalValues);
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

  /**
   * Retrieve all {@link ExperimentalVariable} defined for an experiment.
   *
   * @param experimentId the {@link ExperimentId} of the {@link Experiment} for which the
   *                     {@link ExperimentalVariable} should be retrieved
   * @return a list of {@link ExperimentalVariable} associated with the {@link Experiment} with the
   * {@link ExperimentId}
   */
  public List<ExperimentalVariable> getVariablesOfExperiment(ExperimentId experimentId) {
    Experiment activeExperiment = loadExperimentById(experimentId);
    return activeExperiment.variables();
  }

}
