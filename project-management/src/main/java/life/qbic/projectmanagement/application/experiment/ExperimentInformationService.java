package life.qbic.projectmanagement.application.experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.domain.concepts.LocalDomainEventDispatcher;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalDesign.AddExperimentalGroupResponse.ResponseCode;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalValue;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalVariable;
import life.qbic.projectmanagement.domain.model.experiment.VariableName;
import life.qbic.projectmanagement.domain.model.experiment.event.ExperimentCreatedEvent;
import life.qbic.projectmanagement.domain.model.experiment.event.ExperimentUpdatedEvent;
import life.qbic.projectmanagement.domain.model.experiment.repository.ExperimentRepository;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
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

  private static VariableLevel convertLevel(
      life.qbic.projectmanagement.domain.model.experiment.VariableLevel level) {
    return new VariableLevel(level.variableName().value(), level.experimentalValue().value(),
        level.experimentalValue().unit().orElse(null));
  }

  private static life.qbic.projectmanagement.domain.model.experiment.VariableLevel convertToDomain(
      VariableLevel level) {
    ExperimentalValue value;
    if (level.unit() == null) {
      value = ExperimentalValue.create(level.levelValue());
    } else {
      value = ExperimentalValue.create(level.levelValue(), level.unit());
    }
    return new life.qbic.projectmanagement.domain.model.experiment.VariableLevel(
        new VariableName(level.variableName()), value);
  }

  private static ExperimentalGroup convertFromDomain(
      life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup domainGroup) {
    return new ExperimentalGroup(domainGroup.id(), domainGroup.groupNumber(), domainGroup.name(),
        domainGroup.condition().getVariableLevels().stream()
            .map(ExperimentInformationService::convertFromDomain).toList(), domainGroup.sampleSize());
  }

  private static VariableLevel convertFromDomain(
      life.qbic.projectmanagement.domain.model.experiment.VariableLevel domainLevel) {
    return new VariableLevel(domainLevel.variableName().value(),
        domainLevel.experimentalValue().value(),
        domainLevel.experimentalValue().unit().orElse(null));
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Optional<Experiment> find(String projectId, ExperimentId experimentId) {
    Objects.requireNonNull(experimentId);
    log.debug("Search for experiment with id: " + experimentId.value());
    return experimentRepository.find(experimentId);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Optional<Experiment> find(String projectId, String experimentId) {
    Objects.requireNonNull(experimentId);
    Objects.requireNonNull(projectId);
    if (!ExperimentId.isValid(experimentId)) {
      return Optional.empty();
    }
    return find(projectId, ExperimentId.parse(experimentId));
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

    List<life.qbic.projectmanagement.domain.model.experiment.VariableLevel> varLevels = experimentalGroup.levels();
    if (varLevels.isEmpty()) {
      throw new ApplicationException("No experimental variable was selected",
          ErrorCode.NO_CONDITION_SELECTED,
          ErrorParameters.empty());
    }

    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));

    Experiment experiment = loadExperimentById(experimentId);
    Result<life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup, ResponseCode> result = experiment.addExperimentalGroup(
        experimentalGroup.name(), experimentalGroup.levels(), experimentalGroup.replicateCount());
    if (result.isValue()) {
      experimentRepository.update(experiment);
      dispatchLocalEvents(domainEventsCache);
    } else {
      ResponseCode responseCode = result.getError();
      if (responseCode.equals(ResponseCode.CONDITION_EXISTS)) {
        throw new ApplicationException(
            "A group with the variable levels %s already exists.".formatted(varLevels.toString()),
            ErrorCode.DUPLICATE_GROUP_SELECTED,
            ErrorParameters.empty());
      } else {
        throw new ApplicationException(
            "Could not save one or more experimental groups %s %nReason: %s".formatted(
                experimentalGroup.toString(), responseCode));
      }
    }
  }

  private life.qbic.projectmanagement.domain.model.experiment.VariableLevel convertToDomainVariableLevel(
      VariableLevel level) {
    ExperimentalValue value;
    if (level.unit() == null) {
      value = ExperimentalValue.create(level.levelValue());
    } else {
      value = ExperimentalValue.create(level.levelValue(), level.unit());
    }
    return new life.qbic.projectmanagement.domain.model.experiment.VariableLevel(
        new VariableName(level.variableName()), value);
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
  public List<ExperimentalGroupDTO> getExperimentalGroups(String projectId,
      ExperimentId experimentId) {
    Experiment experiment = loadExperimentById(experimentId);
    return experiment.getExperimentalGroups().stream()
        .map(it -> new ExperimentalGroupDTO(it.id(), it.name(), it.condition().getVariableLevels(),
            it.sampleSize()))
        .toList();
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public List<life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup> experimentalGroupsFor(
      String projectId,
      ExperimentId experimentId) {
    Experiment experiment = loadExperimentById(experimentId);
    return experiment.getExperimentalGroups().stream().toList();
  }

  /**
   * <b>ATTENTION!</b> This will remove all existing experimental variables and all defined
   * experimental groups in a give experiment!
   *
   * @param experimentId the experiment reference to delete the experimental variables from
   * @param projectId    the Id of the project that is being changed
   * @since 1.0.0
   */
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public void deleteAllExperimentalVariables(ExperimentId experimentId, ProjectId projectId) {

    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));

    Experiment experiment = loadExperimentById(experimentId);
    experiment.removeAllExperimentalGroups();
    experiment.removeAllExperimentalVariables();
    experimentRepository.update(experiment);
    dispatchLocalEvents(domainEventsCache);
  }

  /**
   * Will attemt to delete an experimental variable.
   *
   * @param projectId
   * @param experimentId
   * @return
   */
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public boolean deleteExperimentalVariable(ProjectId projectId, ExperimentId experimentId,
      String variableName) {
    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));

    Experiment experiment = loadExperimentById(experimentId);
    try {
      var wasRemoved = experiment.removeExperimentalVariable(variableName);
      experimentRepository.update(experiment);
      dispatchLocalEvents(domainEventsCache);
      return wasRemoved;
    } catch (Experiment.GroupPreventingVariableDeletionException e) {
      throw new GroupPreventingVariableDeletionException(e);
    }
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
   * @param projectId    the Id of the project that is being changed
   * @param species      the species to add
   * @see Experiment#addSpecies(Collection)
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public void addSpeciesToExperiment(String projectId, ExperimentId experimentId,
      OntologyTerm... species) {
    Arrays.stream(species).forEach(Objects::requireNonNull);
    if (species.length < 1) {
      return;
    }

    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));

    Experiment experiment = loadExperimentById(experimentId);
    experiment.addSpecies(List.of(species));
    experimentRepository.update(experiment);
    dispatchLocalEvents(domainEventsCache);
  }

  /**
   * Adds specimens to an experiment
   *
   * @param experimentId the Id of the experiment for which to add the specimen
   * @param projectId    the Id of the project that is being changed
   * @param specimens    the specimens to add
   * @see Experiment#addSpecimens(Collection)
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public void addSpecimenToExperiment(String projectId, ExperimentId experimentId,
      OntologyTerm... specimens) {
    Arrays.stream(specimens).forEach(Objects::requireNonNull);
    if (specimens.length < 1) {
      return;
    }

    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));

    Experiment experiment = loadExperimentById(experimentId);
    experiment.addSpecimens(List.of(specimens));
    experimentRepository.update(experiment);
    dispatchLocalEvents(domainEventsCache);
  }

  /**
   * Adds analytes to an experiment
   *
   * @param experimentId the Id of the experiment for which to add the analyte
   * @param projectId    the Id of the project that is being changed
   * @param analytes     the analytes to add
   * @see Experiment#addAnalytes(Collection)
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public void addAnalyteToExperiment(String projectId, ExperimentId experimentId,
      OntologyTerm... analytes) {
    Arrays.stream(analytes).forEach(Objects::requireNonNull);
    if (analytes.length < 1) {
      return;
    }

    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));

    Experiment experiment = loadExperimentById(experimentId);
    experiment.addAnalytes(List.of(analytes));
    experimentRepository.update(experiment);
    dispatchLocalEvents(domainEventsCache);
  }

  /**
   * Adds {@link ExperimentalVariable} to an {@link Experiment}
   *
   * @param experimentId the Id of the experiment
   * @param projectId    the Id of the project that is being changed
   * @param variableName the name of the variable to be added
   * @param unit         the optionally defined unit for the {@link ExperimentalValue} within the
   *                     {@link ExperimentalVariable}
   * @param levels       String based list of levels from each of which the
   *                     {@link ExperimentalValue} will be derived for the to be defined
   *                     {@link ExperimentalVariable}
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public void addVariableToExperiment(String projectId, ExperimentId experimentId,
      String variableName, String unit,
      List<String> levels) {
    Objects.requireNonNull(variableName);
    Objects.requireNonNull(levels);
    if (levels.isEmpty()) {
      return;
    }

    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));

    Experiment experiment = loadExperimentById(experimentId);
    List<ExperimentalValue> experimentalValues = new ArrayList<>();
    for (String level : levels) {
      ExperimentalValue experimentalValue = (unit.isBlank()) ? ExperimentalValue.create(level)
          : ExperimentalValue.create(level, unit);
      experimentalValues.add(experimentalValue);
    }
    experiment.addVariableToDesign(variableName, experimentalValues);
    experimentRepository.update(experiment);
    dispatchLocalEvents(domainEventsCache);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public void addVariableToExperiment(String projectId, String experimentId, List<AsyncProjectService.ExperimentalVariable> experimentalVariables) {
    Objects.requireNonNull(projectId);
    Objects.requireNonNull(experimentId);
    Objects.requireNonNull(experimentalVariables);
    if (experimentalVariables.isEmpty()) {
      return;
    }
    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));
    Experiment experiment = loadExperimentById(ExperimentId.parse(experimentId));
    for (AsyncProjectService.ExperimentalVariable experimentalVariable : experimentalVariables) {
      List<ExperimentalValue> experimentalValues = new ArrayList<>();
      for (String level : experimentalVariable.levels()) {
        ExperimentalValue experimentalValue =
            experimentalVariable.optionalUnit()
                .map(unit -> ExperimentalValue.create(level, unit))
                .orElse(ExperimentalValue.create(level));
        experimentalValues.add(experimentalValue);
      }
      experiment.addVariableToDesign(experimentalVariable.name(), experimentalValues);
    }
    experimentRepository.update(experiment);
    dispatchLocalEvents(domainEventsCache);
  }


  /**
   * Adds {@link ExperimentalVariable} to an {@link Experiment}
   *
   * @param experimentId the Id of the experiment
   * @param projectId    the Id of the project that is being changed
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public List<ExperimentalVariableInformation> addVariablesToExperiment(String projectId,
      ExperimentId experimentId,
      List<ExperimentalVariableAddition> variableAdditions) {
    if (variableAdditions.isEmpty()) {
      return List.of();
    }
    Predicate<ExperimentalVariableAddition> noNameProvided = addition ->
        addition.name() == null || addition.name().isBlank();

    if (variableAdditions.stream().anyMatch(noNameProvided)) {
      throw new IllegalArgumentException("All variables must have a name");
    }

    Predicate<ExperimentalVariableAddition> noLevelsProvided = addition ->
        addition.levels().isEmpty();

    if (variableAdditions.stream().anyMatch(noLevelsProvided)) {
      throw new IllegalArgumentException("All variables must have a levels");
    }

    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));

    Experiment experiment = loadExperimentById(experimentId);
    List<ExperimentalVariableInformation> addedVariables = new ArrayList<>();
    try {
      for (var variableAddition : variableAdditions) {
        var levels = variableAddition.levels().stream()
            .map(level -> ExperimentalValue.create(level, variableAddition.unit()))
            .toList();
        experiment.addVariableToDesign(variableAddition.name(), levels);
        addedVariables.add(
            new ExperimentalVariableInformation(experimentId.value(), variableAddition.name(),
                variableAddition.unit(), variableAddition.levels()));
      }
      experimentRepository.update(experiment);
      dispatchLocalEvents(domainEventsCache);
    } catch (RuntimeException e) {
      //remove all added variables again
      addedVariables.stream()
          .map(ExperimentalVariableInformation::name)
          .forEach(experiment::removeExperimentalVariable);
      throw e;
    }
    return addedVariables;
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
  public List<ExperimentalVariable> getVariablesOfExperiment(String projectId,
      ExperimentId experimentId) {
    Experiment experiment = loadExperimentById(experimentId);
    return experiment.variables();
  }

  /**
   * Updates an {@link ExperimentalGroup} for a given experiment.
   *
   * @param projectId    the id of the project that is being changed.
   * @param experimentId the id of the experiment for which the experimental group is going to be
   *                     updated.
   * @param group        the information of the experimental group to be updated.
   * @since 1.10.0
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public void updateExperimentalGroup(String projectId, String experimentId,
      ExperimentalGroup group) {
    Objects.requireNonNull(group);
    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));
    // Update the experimental group
    var experiment = loadExperimentById(ExperimentId.parse(experimentId));
    experiment.updateExperimentalGroup(group.id(), group.name(),
        group.levels().stream().map(ExperimentInformationService::convertToDomain).toList(),
        group.replicateCount());
    // Make the changes persistent
    experimentRepository.update(experiment);
    // Dispatch event
    dispatchLocalEvents(domainEventsCache);
  }


  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public void deleteExperimentalGroupByGroupNumber(String projectId, String experimentId,
      int experimentalGroupNumber) {
    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));

    find(projectId, experimentId).ifPresent(experiment -> {
      experiment.removeExperimentGroupByGroupNumber(experimentalGroupNumber);
      experimentRepository.update(experiment);
    });

    dispatchLocalEvents(domainEventsCache);
  }

  /**
   * Deletes all experimental groups in a given experiment.
   *
   * @param id        the experiment identifier of the experiment the experimental groups are going
   *                  to be deleted.
   * @param projectId the Id of the project that is being changed
   * @since 1.0.0
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public void deleteExperimentalGroupsWithIds(String projectId, ExperimentId id,
      List<Long> groupIds) {

    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));

    var queryResult = sampleInformationService.retrieveSamplesForExperiment(id);
    if (queryResult.isError()) {
      throw new ApplicationException("experiment (%s) converting %s to %s".formatted(id,
          queryResult.getError(), DeletionService.ResponseCode.QUERY_FAILED),
          ErrorCode.GENERAL,
          ErrorParameters.empty());
    }
    if (queryResult.isValue() && !queryResult.getValue().isEmpty()) {
      throw new ApplicationException(
          "Could not edit experimental groups because samples are already registered.",
          ErrorCode.SAMPLES_ATTACHED_TO_EXPERIMENT,
          ErrorParameters.empty());
    }
    Experiment experiment = loadExperimentById(id);
    experiment.removeExperimentalGroups(groupIds);
    experimentRepository.update(experiment);
    dispatchLocalEvents(domainEventsCache);
  }

  private void setUpDomainEventDispatcher(List<DomainEvent> domainEventsCache) {
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));
  }

  /**
   * Creates a new experimental group for a given experiment.
   *
   * @param projectId         the id of the project that is being changed.
   * @param experimentId      the id of the experiment for which the experimental group is going to
   *                          be created.
   * @param experimentalGroup the information of the experimental group to be created.
   * @return the created experimental group.
   * @since 1.10.0
   */
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public ExperimentalGroup createExperimentalGroup(String projectId, ExperimentId experimentId,
      ExperimentalGroup experimentalGroup) {
    var domainEventsCache = new ArrayList<DomainEvent>();
    setUpDomainEventDispatcher(domainEventsCache);

    var experiment = loadExperimentById(experimentId);
    var result = experiment.addExperimentalGroup(experimentalGroup.name(),
        experimentalGroup.levels().stream().map(ExperimentInformationService::convertToDomain)
            .toList(),
        experimentalGroup.replicateCount());
    if (result.isError()) {
      log.error("Could not create experimental group, response code was " + result.getError());
      throw new ApplicationException("Could not create experimental group.");
    }
    experimentRepository.update(experiment);
    dispatchLocalEvents(domainEventsCache);
    var createdGroup = result.getValue();
    return new ExperimentalGroup(createdGroup.id(), createdGroup.groupNumber(), createdGroup.name(), createdGroup.condition()
        .getVariableLevels().stream().map(ExperimentInformationService::convertLevel).toList(),
        createdGroup.sampleSize());
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
   * @param projectId the Id of the project that is being changed
   * @param experimentalGroupDTOS  the new list of experimental groups including all updates
   * @since 1.0.0
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public void updateExperimentalGroupsOfExperiment(String projectId, ExperimentId experimentId,
      List<ExperimentalGroupDTO> experimentalGroupDTOS) {

    // check for duplicates
    List<List<life.qbic.projectmanagement.domain.model.experiment.VariableLevel>> distinctLevels = experimentalGroupDTOS.stream()
        .map(ExperimentalGroupDTO::levels).distinct().toList();
    if (distinctLevels.size() < experimentalGroupDTOS.size()) {
      throw new ApplicationException("Duplicate experimental group was selected",
          ErrorCode.DUPLICATE_GROUP_SELECTED,
          ErrorParameters.empty());
    }

    List<life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup> existingGroups = experimentalGroupsFor(
        projectId, experimentId);
    List<Long> idsToDelete = getGroupIdsToDelete(existingGroups, experimentalGroupDTOS);
    if (!idsToDelete.isEmpty()) {
      deleteExperimentalGroupsWithIds(projectId, experimentId, idsToDelete);
    }

    for (ExperimentalGroupDTO group : experimentalGroupDTOS) {
      if (group.id() == -1) {
        addExperimentalGroupToExperiment(experimentId, group);
      } else {
        updateExperimentalGroupOfExperiment(experimentId, group);
      }
    }
  }

  private void updateExperimentalGroupOfExperiment(ExperimentId experimentId,
      ExperimentalGroupDTO group) {
    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));

    Experiment experiment = loadExperimentById(experimentId);
    Result<life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup, ResponseCode> result = experiment.updateExperimentalGroup(
        group.id(),
        group.name(), group.levels(), group.replicateCount());
    result.onValue(ignore -> dispatchLocalEvents(domainEventsCache));
  }

  private List<Long> getGroupIdsToDelete(
      List<life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup> existingGroups,
      List<ExperimentalGroupDTO> newGroups) {
    Set<Long> newIds = newGroups.stream().map(ExperimentalGroupDTO::id).collect(Collectors.toSet());
    return existingGroups.stream()
        .map(life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup::id)
        .filter(Predicate.not(newIds::contains))
        .toList();
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE') ")
  public void editExperimentInformation(String projectId, ExperimentId experimentId,
      String experimentName,
      List<OntologyTerm> species, List<OntologyTerm> specimens, List<OntologyTerm> analytes,
      String speciesIconName, String specimenIconName) {

    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentUpdatedDomainEventSubscriber(domainEventsCache));

    Experiment experiment = loadExperimentById(experimentId);
    experiment.setName(experimentName);
    experiment.setSpecies(species);
    experiment.setSpecimens(specimens);
    experiment.setAnalytes(analytes);
    experiment.setIconNames(speciesIconName, specimenIconName, "default");
    experimentRepository.update(experiment);

    dispatchLocalEvents(domainEventsCache);
  }

  private void dispatchLocalEvents(List<DomainEvent> domainEventsCache) {
    Set<ExperimentId> dispatchedIDs = new HashSet<>();
    for (DomainEvent event : domainEventsCache) {
      if (event instanceof ExperimentUpdatedEvent experimentUpdatedEvent) {
        ExperimentId id = experimentUpdatedEvent.experimentId();
        if (dispatchedIDs.contains(id)) {
          continue;
        }
        DomainEventDispatcher.instance().dispatch(event);
        dispatchedIDs.add(id);
      }
    }
  }

  public Optional<ProjectId> findProjectID(ExperimentId experimentId) {
    Optional<String> id = experimentRepository.findProjectId(experimentId);
    return id.map(ProjectId::parse);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public List<ExperimentalGroup> fetchGroups(String projectId, ExperimentId experimentId) {
    return experimentRepository
        .find(experimentId)
        .map(Experiment::getExperimentalGroups).orElse(Collections.emptyList())
        .stream().map(ExperimentInformationService::convertFromDomain)
        .toList();
  }

  public static class GroupPreventingVariableDeletionException extends RuntimeException {

    public GroupPreventingVariableDeletionException(Throwable cause) {
      super(cause);
    }
  }

  public record ExperimentalVariableAddition(String name, String unit, List<String> levels) {

    public ExperimentalVariableAddition {
      levels = List.copyOf(levels);
    }

    @Override
    public List<String> levels() {
      return List.copyOf(levels);
    }
  }

  public record ExperimentalVariableInformation(String experimentId, String name, String unit,
                                                List<String> levels) {

    public ExperimentalVariableInformation {
      levels = List.copyOf(levels);
    }

    @Override
    public List<String> levels() {
      return List.copyOf(levels);
    }
  }

  /**
   * Information about an experimental group
   *
   * @param id             id, -1 for new groups
   * @param name           the name of the group - can be empty
   * @param levels         the levels in the condition of the group
   * @param replicateCount the number of biological replicates
   */
  public record ExperimentalGroupDTO(long id, String name,
                                     List<life.qbic.projectmanagement.domain.model.experiment.VariableLevel> levels,
                                     int replicateCount) {

  }

  public record ExperimentalGroup(@Nullable Long id, @Nullable Integer groupNumber, String name, List<VariableLevel> levels,
                                  int replicateCount) {

  }

  public record VariableLevel(String variableName, String levelValue, @Nullable String unit) {

  }


  public record ExperimentUpdatedDomainEventSubscriber(
      List<DomainEvent> domainEventsCache) implements
      DomainEventSubscriber<DomainEvent> {

    @Override
    public Class<? extends DomainEvent> subscribedToEventType() {
      return ExperimentUpdatedEvent.class;
    }

    @Override
    public void handleEvent(DomainEvent event) {
      domainEventsCache.add(event);
    }
  }

  public record ExperimentCreatedDomainEventSubscriber(
      List<DomainEvent> domainEventsCache) implements
      DomainEventSubscriber<DomainEvent> {

    @Override
    public Class<? extends DomainEvent> subscribedToEventType() {
      return ExperimentCreatedEvent.class;
    }

    @Override
    public void handleEvent(DomainEvent event) {
      domainEventsCache.add(event);
    }
  }
}
