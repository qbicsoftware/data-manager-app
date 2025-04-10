package life.qbic.projectmanagement.domain.model.experiment;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.LocalDomainEventDispatcher;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalDesign.AddExperimentalGroupResponse.ResponseCode;
import life.qbic.projectmanagement.domain.model.experiment.event.ExperimentCreatedEvent;
import life.qbic.projectmanagement.domain.model.experiment.event.ExperimentUpdatedEvent;
import life.qbic.projectmanagement.domain.model.experiment.exception.ConditionExistsException;
import life.qbic.projectmanagement.domain.model.experiment.exception.ExperimentalVariableExistsException;


/**
 * <b>Experiment</b>
 * <p>
 * An experiment tests a underlying scientific hypothesis.
 * <p>
 * An experiment has an explicit design stating {@link ExperimentalVariable}s and
 * {@link Condition}s. {@link ExperimentalVariable}s are sometimes referred to as experimental
 * factors. For more information about the design of an experiment please see
 * {@link ExperimentalDesign}.
 */
@Entity(name = "experiments_datamanager")
public class Experiment {

  @EmbeddedId
  private ExperimentId experimentId;
  @Column(name = "experimentName")
  private String name;
  @Embedded
  private ExperimentalDesign experimentalDesign;
  @Column(name = "speciesIconName", nullable = false, columnDefinition = "varchar(31) default 'default'")
  private String speciesIconName;
  @Column(name = "specimenIconName", nullable = false, columnDefinition = "varchar(31) default 'default'")
  private String specimenIconName;
  @Column(name = "analyteIconName", nullable = false, columnDefinition = "varchar(31) default 'default'")
  private String analyteIconName;
  
  @ElementCollection(targetClass = OntologyTerm.class)
  @Column(name = "analytes", columnDefinition = "longtext CHECK (json_valid(`analytes`))")
  private List<OntologyTerm> analytes = new ArrayList<>();
  @ElementCollection(targetClass = OntologyTerm.class)
  @Column(name = "species", columnDefinition = "longtext CHECK (json_valid(`species`))")
  private List<OntologyTerm> species = new ArrayList<>();
  @ElementCollection(targetClass = OntologyTerm.class)
  @Column(name = "specimens", columnDefinition = "longtext CHECK (json_valid(`specimens`))")
  private List<OntologyTerm> specimens = new ArrayList<>();
  private static final String DEFAULT_ICON_NAME = "default";

  /**
   * Please use {@link Experiment#create(String)} instead
   */
  protected Experiment() {
    // Please use the create method. This is needed for JPA
  }

  protected Experiment(String name) {
    if (name.isEmpty()) {
      throw new ApplicationException("An Experiment must have a name");
    }
    this.name = name;
    this.experimentId = ExperimentId.create();
    emitExperimentCreatedEvent();
  }

  public static Experiment create(String name) {
    Experiment experiment = new Experiment(name);
    experiment.experimentalDesign = ExperimentalDesign.create();

    experiment.speciesIconName = DEFAULT_ICON_NAME;
    experiment.specimenIconName = DEFAULT_ICON_NAME;
    experiment.analyteIconName = DEFAULT_ICON_NAME;

    return experiment;
  }

  @PostLoad
  private void loadCollections() {
    int analyteCount = analytes.size();
    int specimenCount = specimens.size();
    int speciesCount = species.size();
  }

  /**
   * Returns the name of the experiment.
   *
   * @return the name of the experiment
   */
  public String getName() {
    return name;
  }

  public String getSpeciesIconName() {
    return speciesIconName;
  }

  public String getSpecimenIconName() {
    return specimenIconName;
  }

  public String getAnalyteIconName() {
    return analyteIconName;
  }

  public void setIconNames(String speciesIconName, String specimenIconName,
      String analyteIconName) {
    this.speciesIconName = validateIconName(speciesIconName);
    this.specimenIconName = validateIconName(specimenIconName);
    this.analyteIconName = validateIconName(analyteIconName);
  }

  private String validateIconName(String iconName) {
    requireNonNull(iconName, "Icon names must not be null");
    if (iconName.isBlank()) {
      throw new ApplicationException("Icon names must not be blank");
    }
    return iconName;
  }

  /**
   * Retrieves the list of experimental variables stored within the Experiment.
   *
   * @return Provides the list of {@link ExperimentalVariable} defined within the
   * {@link ExperimentalDesign} of the Experiment
   */

  public List<ExperimentalVariable> variables() {
    return experimentalDesign.experimentalVariables();
  }

  /**
   * @return the identifier of this experiment
   */
  public ExperimentId experimentId() {
    return experimentId;
  }


  /**
   * @return the collection of species in this experiment
   */
  public Collection<OntologyTerm> getSpecies() {
    return species.stream().toList();
  }

  /**
   * @return the collection of specimens in this experiment
   */
  public Collection<OntologyTerm> getSpecimens() {
    return specimens.stream().toList();
  }

  /**
   * @return the collection of analytes in this experiment
   */
  public Collection<OntologyTerm> getAnalytes() {
    return analytes.stream().toList();
  }

  /**
   * Adds specimens to the experiment.
   *
   * @param specimens The specimens to add to the experiment
   */
  public void addSpecimens(Collection<OntologyTerm> specimens) {
    if (specimens.isEmpty()) {
      return;
    }
    List<OntologyTerm> missingSpecimens = specimens.stream()
        .filter(specimen -> !this.specimens.contains(specimen))
        .distinct()
        .toList();
    this.specimens.addAll(missingSpecimens);
    emitExperimentUpdatedEvent();
  }

  /**
   * Removes all experimental variables AND all experimental groups.
   *
   * @since 1.0.0
   */
  public void removeAllExperimentalVariables() {
    removeAllExperimentalGroups();
    experimentalDesign.removeAllExperimentalVariables();
    emitExperimentUpdatedEvent();
  }

  /**
   * Removes an experimental variable if possible.
   *
   * @param name the name of the variable
   * @return true if the variable was removed, falso if there was no need to remove it.
   */
  public boolean removeExperimentalVariable(String name) {

    Optional<ExperimentalVariable> variableOptional = experimentalDesign.getVariable(name);
    if (variableOptional.isEmpty()) {
      return false;
    }
    ExperimentalVariable variable = variableOptional.get();
    //check if any group contains this variable
    if (!experimentalDesign.getExperimentalGroups().isEmpty()) {
      throw new GroupPreventingVariableDeletionException(
          "There are experimental groups in the experimental design. Cannot remove experimental variable "
              + name);
    }
    experimentalDesign.removeExperimentalVariable(variable);
    emitExperimentUpdatedEvent();
    return true;
  }

  public static class GroupPreventingVariableDeletionException extends RuntimeException {

    public GroupPreventingVariableDeletionException(String message) {
      super(message);
    }
  }

  /**
   * Removes experimental groups with the provided ids from the experiment .
   *
   * @since 1.0.0
   */
  public void removeExperimentalGroups(List<Long> ids) {
    ids.forEach(experimentalDesign::removeExperimentalGroup);
    emitExperimentUpdatedEvent();
  }

  /**
   * Removes all experimental groups in an experiment.
   *
   * @since 1.0.0
   */
  public void removeAllExperimentalGroups() {
    for (ExperimentalGroup experimentalGroup : experimentalDesign.getExperimentalGroups()) {
      experimentalDesign.removeExperimentalGroup(experimentalGroup.id());
    }
    emitExperimentUpdatedEvent();
  }

  /**
   * Adds analytes to the experiment.
   *
   * @param analytes The analytes to add to the experiment
   */
  public void addAnalytes(Collection<OntologyTerm> analytes) {
    if (analytes.isEmpty()) {
      return;
    }

    // only add analytes that are not present already
    List<OntologyTerm> missingAnalytes = analytes.stream()
        .filter(analyte -> !this.analytes.contains(analyte))
        .distinct()
        .toList();
    this.analytes.addAll(missingAnalytes);
    emitExperimentUpdatedEvent();
  }

  /**
   * Adds species to the experiment.
   *
   * @param species The species to add to the experiment
   */
  public void addSpecies(Collection<OntologyTerm> species) {
    if (species.isEmpty()) {
      return;
    }
    // only add species that are not present already
    List<OntologyTerm> missingSpecies = species.stream()
        .filter(speci -> !this.species.contains(speci))
        .distinct()
        .toList();
    this.species.addAll(missingSpecies);
    emitExperimentUpdatedEvent();
  }

  /**
   * Creates a new experimental variable and adds it to the experimental design.
   * <p>
   * <b>Note</b>: If a variable with the provided name already exists, the creation will throw
   * an {@link ExperimentalVariableExistsException} and no variable is added to the design.
   *
   * @param variableName a declarative and unique name for the variable
   * @param levels       a list containing at least one value for the variable
   * @return the {@link ExperimentalVariable} that was added to the design
   * @since 1.10.0
   */
  public ExperimentalVariable addVariableToDesign(String variableName,
      List<ExperimentalValue> levels) {
    return experimentalDesign.addVariable(variableName, levels)
        .onValue(ignored -> emitExperimentCreatedEvent())
        .valueOrElseThrow(e -> new RuntimeException(e));
  }


  public void removeExperimentalVariables(List<String> addedNames) {
    throw new RuntimeException("Not implemented");

  }

  /**
   * Creates an experimental group consisting of one or more levels of distinct variables and the
   * sample size and adds it to the experimental design.
   * <p>
   * <ul>
   *   <li>If an experimental group with the same variable levels already exists, the creation will fail with an {@link ConditionExistsException} and no condition is added to the design.
   *   <li>If the {@link VariableLevel}s belong to variables not specified in this experiment, the creation will fail with an {@link IllegalArgumentException}
   *   <li>If the sample size is not at least 1, the creation will fail with an {@link IllegalArgumentException}
   * </ul>
   *
   * @param groupName      the name of this experimental group, which can be empty
   * @param variableLevels at least one value for a variable defined in this experiment
   * @param sampleSize     the number of samples that are expected for this experimental group
   * @return
   */
  public Result<ExperimentalGroup, ResponseCode> addExperimentalGroup(String groupName,
      Collection<VariableLevel> variableLevels,
      int sampleSize) {
    return experimentalDesign.addExperimentalGroup(groupName, variableLevels, sampleSize)
        .onValue(x -> emitExperimentUpdatedEvent());
  }

  /**
   * Updates an experimental group of to the experimental design.
   * <p>
   * <ul>
   *   <li>If an experimental group with the same variable levels already exists, the creation will fail with an {@link ConditionExistsException} and no condition is added to the design.
   *   <li>If the {@link VariableLevel}s belong to variables not specified in this experiment, the creation will fail with an {@link IllegalArgumentException}
   *   <li>If the sample size is not at least 1, the creation will fail with an {@link IllegalArgumentException}
   * </ul>
   *
   * @param id             the unique identifier of the experimental group to update
   * @param groupName      the name of this experimental group, which can be empty
   * @param variableLevels at least one value for a variable defined in this experiment
   * @param sampleSize     the number of samples that are expected for this experimental group
   * @return
   */
  public Result<ExperimentalGroup, ResponseCode> updateExperimentalGroup(long id, String groupName,
      Collection<VariableLevel> variableLevels,
      int sampleSize) {
    return experimentalDesign.updateExperimentalGroup(id, groupName, variableLevels, sampleSize)
        .onValue(ignored -> emitExperimentUpdatedEvent());
  }

  public List<ExperimentalGroup> getExperimentalGroups() {
    return experimentalDesign.getExperimentalGroups();
  }

  public void removeExperimentGroup(long groupId) {
    experimentalDesign.removeExperimentalGroup(groupId);
  }

  /**
   * Sets the name of the experiment.
   */
  public void setName(String name) {
    if (name.isEmpty()) {
      throw new ApplicationException("An Experiment must have a name");
    }
    this.name = name;
  }

  /**
   * Sets the list of species for an experiment.
   */
  public void setSpecies(
      List<OntologyTerm> species) {
    if (species == null || species.isEmpty()) {
      throw new ApplicationException(ErrorCode.NO_SPECIES_DEFINED,
          ErrorParameters.of(species));
    }
    this.species = species;
    emitExperimentUpdatedEvent();
  }

  /**
   * Sets the list of specimen for an experiment.
   */
  public void setSpecimens(
      List<OntologyTerm> specimens) {
    if (specimens == null || specimens.isEmpty()) {
      throw new ApplicationException(ErrorCode.NO_SPECIMEN_DEFINED,
          ErrorParameters.of(specimens));
    }
    this.specimens = specimens;
    emitExperimentUpdatedEvent();
  }

  /**
   * Sets the list of analytes for an experiment.
   */
  public void setAnalytes(
      List<OntologyTerm> analytes) {
    if (analytes == null || analytes.isEmpty()) {
      throw new ApplicationException(ErrorCode.NO_ANALYTE_DEFINED,
          ErrorParameters.of(analytes));
    }
    this.analytes = analytes;
    emitExperimentUpdatedEvent();
  }

  private void emitExperimentUpdatedEvent() {
    var updatedEvent = new ExperimentUpdatedEvent(this.experimentId());
    LocalDomainEventDispatcher.instance().dispatch(updatedEvent);
  }

  private void emitExperimentCreatedEvent() {
    var createdEvent = new ExperimentCreatedEvent(this.experimentId());
    LocalDomainEventDispatcher.instance().dispatch(createdEvent);
  }

}
