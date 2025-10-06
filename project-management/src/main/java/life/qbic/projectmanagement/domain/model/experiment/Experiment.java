package life.qbic.projectmanagement.domain.model.experiment;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

  @Version
  private int version;
  
  @ElementCollection(targetClass = OntologyTerm.class)
  @Column(name = "analytes", columnDefinition = "longtext CHECK (json_valid(`analytes`))")
  private List<OntologyTerm> analytes = new ArrayList<>();
  @ElementCollection(targetClass = OntologyTerm.class)
  @Column(name = "species", columnDefinition = "longtext CHECK (json_valid(`species`))")
  private List<OntologyTerm> species = new ArrayList<>();
  @ElementCollection(targetClass = OntologyTerm.class)
  @Column(name = "specimens", columnDefinition = "longtext CHECK (json_valid(`specimens`))")
  private List<OntologyTerm> specimens = new ArrayList<>();

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

    return experiment;
  }

  @PostLoad
  private void loadCollections() {
    int analyteCount = analytes.size();
    int specimenCount = specimens.size();
    int speciesCount = species.size();
    if (experimentalDesign.experimentalGroups.stream().anyMatch(
        group -> group.groupNumber() == null)) {
      assignExperimentalGroupNumbers();
    }
  }

  /*
  Temporary helper method to assign group numbers for existing experimental groups.

  In the future, no experimental group without an assigned number should exist. Until the transition
  is complete, this method shall be called when the aggregate is checked out from the database.
   */
  private void assignExperimentalGroupNumbers() {
    var maxId = experimentalDesign.experimentalGroups.stream().filter(group -> group.groupNumber() != null).mapToInt(ExperimentalGroup::groupNumber).max().orElse(1);
    var currentId =maxId;
    for (ExperimentalGroup group : experimentalDesign.experimentalGroups) {
      if (group.groupNumber() == null) {
        group.setGroupNumber(currentId);
        currentId++;
      }
    }
  }

  /**
   * Returns the name of the experiment.
   *
   * @return the name of the experiment
   */
  public String getName() {
    return name;
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
    //check if any group contains this variable
    if (!experimentalDesign.getExperimentalGroups().isEmpty()) {
      throw new GroupPreventingVariableDeletionException(
          "There are experimental groups in the experimental design. Cannot remove experimental variable "
              + name);
    }
    experimentalDesign.removeAllExperimentalVariables();
    emitExperimentUpdatedEvent();
  }

  /**
   * Removes an experimental variable if possible. Emits an experiment update domain event.
   *
   * @param name the name of the variable
   * @return true if the variable was removed, falso if there was no need to remove it.
   */
  public boolean removeExperimentalVariable(String name) {
    var changed = experimentalDesign.removeExperimentalVariable(name);
    if (changed) {
      emitExperimentUpdatedEvent();
    }
    return changed;
  }

  public void removeExperimentGroupByGroupNumber(int experimentalGroupNumber) {
    experimentalDesign.removeExperimentalGroupByGroupNumber(experimentalGroupNumber);
  }

  /**
   * Renames an experimental variable
   *
   * @param currentName the name of the variable now
   * @param futureName  the name of the variable after renaming
   */
  public void renameExperimentalVariable(String currentName, String futureName) {
    experimentalDesign.renameExperimentalVariable(currentName, futureName);
  }

  public void setVariableUnit(String variableName, String unit) {
    experimentalDesign.changeUnit(variableName, unit);
  }

  /**
   * Overwrites the levels of a variable by the provided levels values. The unit is unchanged.
   * @param variable the variable for which to set the level values
   * @param levels the values of the experimental variable levels
   */
  public void setVariableLevels(String variable, List<String> levels) {
    String unit = experimentalDesign.unitForVariable(variable).orElse(null);
    List<ExperimentalValue> mappedLevels = levels.stream().map(l -> new ExperimentalValue(l, unit))
        .toList();
    experimentalDesign.setVariableLevels(variable, mappedLevels);
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
    ExperimentalVariable experimentalVariable = ExperimentalVariable.create(variableName,
        levels.toArray(new ExperimentalValue[0]));
    boolean created = experimentalDesign.addExperimentalVariable(experimentalVariable);
    if (created) {
      emitExperimentUpdatedEvent();
    }
    return experimentalVariable;
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
