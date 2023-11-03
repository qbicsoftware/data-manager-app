package life.qbic.projectmanagement.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalDesign.AddExperimentalGroupResponse.ResponseCode;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalValue;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalVariable;
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.model.experiment.repository.ExperimentRepository;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
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
  private final ProjectRepository projectRepository;

  public ExperimentInformationService(@Autowired ExperimentRepository experimentRepository,
      @Autowired ProjectRepository projectRepository) {
    this.experimentRepository = experimentRepository;
    this.projectRepository = projectRepository;
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
  public Result<ExperimentalGroup, ResponseCode> addExperimentalGroupToExperiment(
      ExperimentId experimentId, ExperimentalGroupDTO experimentalGroup) {
    Objects.requireNonNull(experimentalGroup, "experimental group must not be null");
    Objects.requireNonNull(experimentId, "experiment id must not be null");

    Experiment experiment = loadExperimentById(experimentId);
    Result<ExperimentalGroup, ResponseCode> result = experiment.addExperimentalGroup(
        experimentalGroup.levels(), experimentalGroup.replicateCount());
    if (result.isValue()) {
      experimentRepository.update(experiment);
    }
    return result;
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

  /**
   * <b>ATTENTION!</b> This will remove all existing experimental variables and all defined
   * experimental groups in a give experiment!
   *
   * @param experimentId the experiment reference to delete the experimental variables from
   * @since 1.0.0
   */
  public void deleteAllExperimentalVariables(ExperimentId experimentId) {
    Experiment experiment = loadExperimentById(experimentId);
    experiment.removeAllExperimentalGroups();
    experiment.removeAllExperimentalVariables();
    experimentRepository.update(experiment);
  }

  /**
   * Returns a list of experiment for a given project.
   *
   * @param projectId the project the experiment is linked to
   * @return a list of experiments linked to the project
   */
  public List<Experiment> findAllForProject(ProjectId projectId) {
    Project project = projectRepository.find(projectId).orElseThrow();
    List<ExperimentId> experimentIds = project.experiments();
    return experimentIds.stream()
        .map(experimentRepository::find)
        .map(Optional::orElseThrow)
        .toList();
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
    Experiment experiment = loadExperimentById(experimentId);
    experiment.addSpecies(List.of(species));
    experimentRepository.update(experiment);
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
    Experiment experiment = loadExperimentById(experimentId);
    experiment.addSpecimens(List.of(specimens));
    experimentRepository.update(experiment);
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
    Experiment experiment = loadExperimentById(experimentId);
    experiment.addAnalytes(List.of(analytes));
    experimentRepository.update(experiment);
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
    Experiment experiment = loadExperimentById(experimentId);
    List<ExperimentalValue> experimentalValues = new ArrayList<>();
    for (String level : levels) {
      ExperimentalValue experimentalValue = (unit.isBlank()) ? ExperimentalValue.create(level)
          : ExperimentalValue.create(level, unit);
      experimentalValues.add(experimentalValue);
    }
    experiment.addVariableToDesign(variableName, experimentalValues);
    experimentRepository.update(experiment);
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
    Experiment experiment = loadExperimentById(experimentId);
    return experiment.variables();
  }

  /**
   * Deletes all experimental groups in a given experiment.
   * <p>
   * This method does not check if samples are already.
   *
   * @param id the experiment identifier of the experiment the experimental groups are going to be
   *           deleted.
   * @since 1.0.0
   */
  public void deleteAllExperimentalGroups(ExperimentId id) {
    Experiment experiment = loadExperimentById(id);
    experiment.removeAllExperimentalGroups();
    experimentRepository.update(experiment);
  }

  /**
   * Adds experimental groups to an experiment
   *
   * @param experimentId          the experiment to add the groups to
   * @param experimentalGroupDTOS the group information
   * @return either the collection of added groups or an appropriate response code
   */
  public Result<Collection<ExperimentalGroup>, ResponseCode> addExperimentalGroupsToExperiment(
      ExperimentId experimentId, List<ExperimentalGroupDTO> experimentalGroupDTOS) {
    Experiment experiment = loadExperimentById(experimentId);
    List<ExperimentalGroup> addedGroups = new ArrayList<>();
    for (ExperimentalGroupDTO experimentalGroupDTO : experimentalGroupDTOS) {
      Result<ExperimentalGroup, ResponseCode> result = experiment.addExperimentalGroup(
          experimentalGroupDTO.levels(),
          experimentalGroupDTO.replicateCount());
      if (result.isError()) {
        return Result.fromError(result.getError());
      } else {
        addedGroups.add(result.getValue());
      }
    }
    experimentRepository.update(experiment);
    return Result.fromValue(addedGroups);
  }

  public void editExperimentInformation(ExperimentId experimentId, String experimentName,
      List<Species> species, List<Specimen> specimens, List<Analyte> analytes) {
    Experiment experiment = loadExperimentById(experimentId);
    experiment.setName(experimentName);
    experiment.setSpecies(species);
    experiment.setAnalytes(analytes);
    experiment.setSpecimens(specimens);
    experimentRepository.update(experiment);
  }

  /**
   * Information about an experimental group
   *
   * @param levels         the levels in the condition of the group
   * @param replicateCount the number of biological replicates
   */
  public record ExperimentalGroupDTO(Collection<VariableLevel> levels, int replicateCount) {

  }
}
