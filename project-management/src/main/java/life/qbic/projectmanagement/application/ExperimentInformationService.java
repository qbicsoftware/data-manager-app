package life.qbic.projectmanagement.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalDesign.AddExperimentalGroupResponse.ResponseCode;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalValue;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalVariable;
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.model.experiment.repository.ExperimentRepository;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final SampleInformationService sampleInformationService;

  public ExperimentInformationService(@Autowired ExperimentRepository experimentRepository,
      @Autowired ProjectRepository projectRepository,
      @Autowired SampleInformationService sampleInformationService) {
    this.experimentRepository = experimentRepository;
    this.projectRepository = projectRepository;
    this.sampleInformationService = sampleInformationService;
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
  private void addExperimentalGroupToExperiment(
      ExperimentId experimentId, ExperimentalGroupDTO experimentalGroup) {
    Objects.requireNonNull(experimentalGroup, "experimental group must not be null");
    Objects.requireNonNull(experimentId, "experiment id must not be null");

    List<VariableLevel> varLevels = experimentalGroup.levels;
    if (varLevels.isEmpty()) {
      throw new ApplicationException("No experimental variable was selected",
          ErrorCode.NO_CONDITION_SELECTED,
          ErrorParameters.empty());
    }

      Experiment experiment = loadExperimentById(experimentId);
      Result<ExperimentalGroup, ResponseCode> result = experiment.addExperimentalGroup(
          experimentalGroup.name(), experimentalGroup.levels(), experimentalGroup.replicateCount());
      if (result.isValue()) {
        experimentRepository.update(experiment);
      } else {
        ResponseCode responseCode = result.getError();
        if (responseCode.equals(ResponseCode.CONDITION_EXISTS)) {
          throw new ApplicationException("A group with the variable levels %s already exists.".formatted(varLevels.toString()),
              ErrorCode.DUPLICATE_GROUP_SELECTED,
              ErrorParameters.empty());
        } else {
          throw new ApplicationException(
              "Could not save one or more experimental groups %s %nReason: %s".formatted(
                  experimentalGroup.toString(), responseCode));
        }
      }
  }

  /**
   * Retrieve all analytes of an experiment.
   *
   * @param experimentId the Id of the experiment for which the experimental groups should be
   *                     retrieved
   * @return the list of experimental groups in the active experiment.
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public List<ExperimentalGroupDTO> getExperimentalGroups(String projectId, ExperimentId experimentId) {
    Experiment experiment = loadExperimentById(experimentId);
    return experiment.getExperimentalGroups().stream()
        .map(it -> new ExperimentalGroupDTO(it.id(), it.name(), it.condition().getVariableLevels(), it.sampleSize()))
        .toList();
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public List<ExperimentalGroup> experimentalGroupsFor(String projectId, ExperimentId experimentId) {
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
  @PostAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
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
  public void addSpeciesToExperiment(ExperimentId experimentId, OntologyTerm... species) {
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
  public void addSpecimenToExperiment(ExperimentId experimentId, OntologyTerm... specimens) {
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
  public void addAnalyteToExperiment(ExperimentId experimentId, OntologyTerm... analytes) {
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
  public Collection<OntologyTerm> getAnalytesOfExperiment(ExperimentId experimentId) {
    Experiment experiment = loadExperimentById(experimentId);
    return experiment.getAnalytes();
  }

  /**
   * Retrieve all species of an experiment.
   *
   * @param experimentId the Id of the experiment for which the species should be retrieved
   * @return a collection of species in the active experiment.
   */
  public Collection<OntologyTerm> getSpeciesOfExperiment(ExperimentId experimentId) {
    Experiment experiment = loadExperimentById(experimentId);
    return experiment.getSpecies();
  }

  /**
   * Retrieve all specimen of an experiment.
   *
   * @param experimentId the Id of the experiment for which the specimen should be retrieved
   * @return a collection of specimen in the active experiment.
   */
  public Collection<OntologyTerm> getSpecimensOfExperiment(ExperimentId experimentId) {
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
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public List<ExperimentalVariable> getVariablesOfExperiment(String projectId, ExperimentId experimentId) {
    Experiment experiment = loadExperimentById(experimentId);
    return experiment.variables();
  }

  /**
   * Deletes all experimental groups in a given experiment.
   *
   * @param id the experiment identifier of the experiment the experimental groups are going to be
   *           deleted.
   * @since 1.0.0
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public void deleteExperimentalGroupsWithIds(String projectId, ExperimentId id, List<Long> groupIds) {
    var queryResult = sampleInformationService.retrieveSamplesForExperiment(id);
    if (queryResult.isError()) {
      throw new ApplicationException("experiment (%s) converting %s to %s".formatted(id,
          queryResult.getError(), DeletionService.ResponseCode.QUERY_FAILED),
          ErrorCode.GENERAL,
          ErrorParameters.empty());
    }
    if (queryResult.isValue() && !queryResult.getValue().isEmpty()) {
      throw new ApplicationException("Could not edit experimental groups because samples are already registered.",
          ErrorCode.SAMPLES_ATTACHED_TO_EXPERIMENT,
          ErrorParameters.empty());
    }
    Experiment experiment = loadExperimentById(id);
    experiment.removeExperimentalGroups(groupIds);
    experimentRepository.update(experiment);
  }

  @Transactional
  /**
   * Updates experimental groups in a given experiment.
   *
   * Compares the provided list of experimental groups of an experiment with the persistent state.
   * Removes groups from the experiment that are not in the new list, adds groups that are not in
   * the experiment yet and updates the other groups of the experiment.
   *
   * @param id                     the experiment identifier of the experiment whose groups should be updated
   * @param experimentalGroupDTOS  the new list of experimental groups including all updates
   * @since 1.0.0
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public void updateExperimentalGroupsOfExperiment(String projectId, ExperimentId experimentId,
      List<ExperimentalGroupDTO> experimentalGroupDTOS) {

    // check for duplicates
    List<List<VariableLevel>> distinctLevels = experimentalGroupDTOS.stream()
        .map(ExperimentalGroupDTO::levels).distinct().toList();
    if (distinctLevels.size() < experimentalGroupDTOS.size()) {
      throw new ApplicationException("Duplicate experimental group was selected",
          ErrorCode.DUPLICATE_GROUP_SELECTED,
          ErrorParameters.empty());
    }

    List<ExperimentalGroup> existingGroups = experimentalGroupsFor(projectId, experimentId);
    List<Long> idsToDelete = getGroupIdsToDelete(existingGroups, experimentalGroupDTOS);
    if(!idsToDelete.isEmpty()) {
      deleteExperimentalGroupsWithIds(projectId, experimentId, idsToDelete);
    }

    for(ExperimentalGroupDTO group : experimentalGroupDTOS) {
      if(group.id() == -1) {
        addExperimentalGroupToExperiment(experimentId, group);
      } else {
        updateExperimentalGroupOfExperiment(experimentId, group);
      }
    }
  }

  private void updateExperimentalGroupOfExperiment(ExperimentId experimentId, ExperimentalGroupDTO group) {
    Experiment experiment = loadExperimentById(experimentId);
    experiment.updateExperimentalGroup(group.id(), group.name(), group.levels(), group.replicateCount());
  }

  private List<Long> getGroupIdsToDelete(List<ExperimentalGroup> existingGroups,
      List<ExperimentalGroupDTO> newGroups) {
    Set<Long> newIds = newGroups.stream().map(ExperimentalGroupDTO::id).collect(Collectors.toSet());
    return existingGroups.stream()
        .map(ExperimentalGroup::id)
        .filter(Predicate.not(newIds::contains))
        .toList();
  }

  public void editExperimentInformation(ExperimentId experimentId, String experimentName,
      List<OntologyTerm> species, List<OntologyTerm> specimens, List<OntologyTerm> analytes) {
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
   * @param id             id, -1 for new groups
   * @param name           the name of the group - can be empty
   * @param levels         the levels in the condition of the group
   * @param replicateCount the number of biological replicates
   */
  public record ExperimentalGroupDTO(long id, String name, List<VariableLevel> levels, int replicateCount) {

  }
}
