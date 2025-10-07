package life.qbic.projectmanagement.domain.model.experiment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.model.experiment.Experiment.GroupPreventingVariableDeletionException;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalDesign.AddExperimentalGroupResponse.ResponseCode;
import life.qbic.projectmanagement.domain.model.experiment.exception.ConditionExistsException;
import life.qbic.projectmanagement.domain.model.experiment.exception.ExperimentalVariableExistsException;
import life.qbic.projectmanagement.domain.model.experiment.exception.UnknownExperimentalVariableException;
import org.springframework.lang.Nullable;

/**
 * <b>Experimental Design</b>
 * <p>
 * An experimental design describes the configuration of a planned and executed experiment that is
 * designed to test a underlying scientific hypothesis.
 * <p>
 * An experiment contains of {@link ExperimentalVariable}s, also sometimes referred to as
 * experimental factors. These variables are controlled levels and dimensions of features, that the
 * researcher sets up for an experiment, in order to test the hypothesis.
 * <p>
 * The experiment is then carefully conducted in an highly controlled environment, in order to
 * minimize random effects on the measurement and get confidence.
 * <p>
 * In total you can define:
 * <ul>
 *   <li>{@link ExperimentalVariable}</li>
 *   <li>{@link ExperimentalGroup}</li>
 * </ul>
 * <p>
 * The order of creation is important to create a meaningful experimental design.
 *
 * <p>
 * <b>1. Define an Experimental Variable</b>
 * <p>
 * Describes an experimental variable with a unique and declarative name. In addition, it contains {@link ExperimentalValue}s, representing the levels
 * of the variable that are part of the experiment.
 * <p>
 * Experimental variables can be created via the {@link Experiment#addVariableToDesign(String, List)}  function.
 * <p>
 * <b>Note:</b> variables need to be unique, and its name will be checked against already defined variables in the design!
 *
 * <p>
 * <b>2. Add an ExperimentGroup</b>
 * <p>
 * {@link ExperimentalGroup}s represent conditions - different linear combinations of experimental variable level(s), as well as the sample size. For example you want to compare different genotypes of a specimen,
 * or different treatments. Let's assume you have a experimental design with two variables:
 * <ul>
 * <li>(1) the genotype (wildtype vs. mutant) </li>
 * <li>(2) a treatment a solvent with different concentrations (0 mmol/L and 150 mmol/L)</li>
 * </ul>
 * <p>
 * So in total there will be four conditions for four experimental groups:
 *
 * <ul>
 *   <li>(1) wildtype + 0 mmol/L</li>
 *   <li>(2) wildtype + 150 mmol/L</li>
 *   <li>(3) mutant + 0 mmol/L</li>
 *   <li>(4) mutant + 150 mmol/L</li>
 * </ul>
 * <p>
 * <p>
 * <b>Note:</b> The {@link ExperimentalVariable} referenced in the {@link VariableLevel} is required for defining an {@link ExperimentalGroup}.
 *
 * @since 1.0.0
 */
@Embeddable
public class ExperimentalDesign {


  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL)
  @JoinColumn(name = "experimentId")
  // @JoinColumn so no extra table is created as experimental_variables contains that column
  final List<ExperimentalVariable> variables = new ArrayList<>();

  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL)
  @JoinColumn(name = "experimentId")
  final Set<ExperimentalGroup> experimentalGroups = new HashSet<>();

  protected ExperimentalDesign() {
    // needed for JPA
  }

  static ExperimentalDesign create() {
    return new ExperimentalDesign();
  }

  @PostLoad
  private void loadCollections() {
    /*do we need the lists to be populated? Can we use @Transactional instead?
    benefits (+) and draw-backs (-)
      (+) less load on the database when designs are loaded
      (-) we cannot be sure the collections are loaded and might get LazyInvocationExceptions
      (-) we have to be careful and understand @Transactional in the context where experimental designs are loaded from the database.
    */

    int variableCount = variables.size();
    int groupCount = experimentalGroups.size();
  }

  private boolean isLevelUsed(VariableLevel variableLevel)
      throws UnknownExperimentalVariableException {
    String variableName = variableLevel.variableName().value();
    Optional<ExperimentalVariable> optionalExperimentalVariable = variableWithName(variableName);
    if (optionalExperimentalVariable.isEmpty()) {
      throw new UnknownExperimentalVariableException(
          "No variable with name " + variableName + " exists");
    }

    return definedConditions().anyMatch(it -> it.contains(variableLevel));
  }

  private Stream<Condition> definedConditions() {
    return experimentalGroups.parallelStream()
        .map(ExperimentalGroup::condition)
        .distinct();
  }

  /**
   * Returns the unit for the given variable if set.
   *
   * @param variableName the name of the variable
   * @return {@link Optional#empty()} if no unit is set for the variable, a set Optional otherwise.
   * @throws UnknownExperimentalVariableException in case the variable is not part of this design
   */
  Optional<String> unitForVariable(String variableName)
      throws UnknownExperimentalVariableException {
    return variableWithName(variableName)
        .orElseThrow(() -> new UnknownExperimentalVariableException(
            "No variable with name " + variableName + " exists"))
        .usedUnit();
  }

  /**
   * Overwrites the levels of the variable with the provided experimental variable levels
   *
   * @param variableName the name of the variable
   * @param levels       the levels to set, must be distinct
   * @return true if the variable was changes as a result of calling this method, false otherwise
   * @throws UnknownExperimentalVariableException in case the experimental variable is not part of
   *                                              this design.
   * @throws IllegalArgumentException             in case the provided levels are not distinct or
   *                                              are used {@link #isLevelUsed(VariableLevel)}
   */
  boolean setVariableLevels(String variableName, List<ExperimentalValue> levels)
      throws UnknownExperimentalVariableException, IllegalArgumentException {
    var experimentalVariable = variableWithName(variableName)
        .orElseThrow(() -> new UnknownExperimentalVariableException(
            "No variable with name " + variableName + " exists"));
    List<VariableLevel> levelsToRemove = experimentalVariable.levels()
        .stream()
        .filter(it -> !levels.contains(it.experimentalValue()))
        .toList();

    List<VariableLevel> wrongfullyDeletedLevels = levelsToRemove.stream().filter(this::isLevelUsed)
        .toList();

    if (!wrongfullyDeletedLevels.isEmpty()) {
      throw new IllegalArgumentException(
          "Variable levels " + wrongfullyDeletedLevels
              + " are already in use and cannot be deleted.");
    }

    //set levels if everything is in order
    if (levels.size() != levels.stream().distinct().count()) {
      throw new IllegalArgumentException("Duplicate levels detected. This is not allowed.");
    }
    return experimentalVariable.replaceLevels(levels);
  }

  /**
   * Adds an experimental variable to the design
   * @param experimentalVariable the experimental variable to add
   * @return true if the experimental design was modified, false if the variable already existed.
   * @throws IllegalStateException in case experimental groups are defined, preventing the addition of experimental variables.
   * @throws ExperimentalVariableExistsException in case the variable already exists but differs from the provided variable.
   */
  boolean addExperimentalVariable(ExperimentalVariable experimentalVariable) {
    if (experimentalGroups.size() > 0) {
      throw new IllegalStateException("There are already experimental groups defined");
    }
    var optionalExperimentalVariable = variableWithName(
        experimentalVariable.name().value());
    if (optionalExperimentalVariable.isPresent()) {
      ExperimentalVariable variable = optionalExperimentalVariable.orElseThrow();
      if (variable.name().equals(experimentalVariable.name()) && variable.levels()
          .equals(experimentalVariable.levels())) {
        //we have the same information already, nothing to do.
        return false;
      } else {
        throw new ExperimentalVariableExistsException(
            "A variable with name " + experimentalVariable.name() + " already exists");
      }
    }
    return variables.add(experimentalVariable);
  }

  boolean removeExperimentalVariable(String variableName) {
    if (!isVariableDefined(variableName)) {
      return false;
    }

    if (!getExperimentalGroups().isEmpty()) {
      throw new GroupPreventingVariableDeletionException(
          "There are experimental groups in the experimental design. Cannot remove experimental variable "
              + variableName);
    }
    variables.removeIf(it -> it.name().value().trim().equals(variableName.trim()));
    return true;
  }


  /**
   * Renames an existing experimental variable. The new name must not be present as an existing
   * variable. This method undoes any incomplete work in case of an exception.
   *
   * @param oldName the current name of the variable to be changed
   * @param newName the new name after renaming, must not be blank or null
   * @throws UnknownExperimentalVariableException in case no variable with the name `oldName`
   *                                              exists.
   * @throws ExperimentalVariableExistsException  in case a variable with the name `newName` already
   *                                              exists.
   * @throws IllegalArgumentException if an illegal argument was supplied
   */
  void renameExperimentalVariable(String oldName, String newName)
      throws UnknownExperimentalVariableException, ExperimentalVariableExistsException, IllegalArgumentException {
    if (newName == null || newName.isBlank()) {
      throw new IllegalArgumentException("New name cannot be null or blank");
    }
    if (variableWithName(oldName).isEmpty()) {
      throw new UnknownExperimentalVariableException(
          "No variable with name " + oldName + " exists");
    }
    if (variableWithName(newName).isPresent()) {
      throw new ExperimentalVariableExistsException(
          "Variable with the name " + newName + " already exists.");
    }

    var currentVariable = variableWithName(oldName).orElseThrow();
    List<ExperimentalGroup> deepCopyOfGroups = experimentalGroups.stream()
        .map(ExperimentalGroup::deepCopy)
        .toList();
    deepCopyOfGroups.forEach(group -> group.renameVariable(oldName, newName));
    this.experimentalGroups.clear();
    this.experimentalGroups.addAll(deepCopyOfGroups);
    currentVariable.renameTo(newName);
  }

  List<ExperimentalVariable> experimentalVariables() {
    return Collections.unmodifiableList(variables);
  }

  /**
   * Returns the experimental variable with this name. If no variable with the given name exists,
   * returns {@link Optional#empty()}
   *
   * @param variableName the name to search for, can be null
   * @return an optional containing the existing variable or {@link Optional#empty()} otherwise
   */
  private Optional<ExperimentalVariable> variableWithName(@Nullable String variableName) {
    return variables.stream().filter(it -> it.name().value().equals(variableName)).findAny();
  }

  private boolean isVariableDefined(String variableName) {
    return variables.stream().anyMatch(it -> it.name().value().trim().equals(variableName.trim()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ExperimentalDesign design = (ExperimentalDesign) o;
    return variables.equals(design.variables);
  }

  @Override
  public int hashCode() {
    int result = variables.hashCode();
    result = 31 * result + experimentalGroups.hashCode();
    return result;
  }

  /**
   * Whether a {@link Condition} is defined in this design
   *
   * @return true if there is a condition with the same levels; false otherwise
   */
  private boolean isConditionDefined(Condition condition) {
    return experimentalGroups.stream().anyMatch(it -> it.condition().equals(condition));
  }


  void removeAllExperimentalVariables() throws IllegalStateException {
    if (!experimentalGroups.isEmpty()) {
      throw new IllegalStateException(
          "Cannot delete experimental variables referenced by an experimental group.");
    }
    this.variables.clear();
  }

  /**
   * Gets a variable from the design
   *
   * @param name the name of the variable
   * @return the optional variable, {@link Optional#empty()} if no variable with that name exists.
   */
  Optional<ExperimentalVariable> getVariable(String name) {
    return variables.stream()
        .filter(it -> it.name().value().equals(name))
        .findAny();
  }

  /**
   * Change the unit of all experimental values.
   *
   * @param variableName the name of the variable
   * @param unit         the unit to use from now on
   * @throws UnknownExperimentalVariableException in case the variable is not know to this design
   */
  void changeUnit(String variableName, String unit) throws UnknownExperimentalVariableException {
    ExperimentalVariable experimentalVariable = getVariable(variableName).orElseThrow(() ->
        new UnknownExperimentalVariableException("Variable " + variableName + " does not exist."));
    if (experimentalVariable.usedUnit().map(usedUnit -> usedUnit.equals(unit)).orElse(false)) {
      return;
    }
    List<VariableLevel> newLevels = experimentalVariable.levels().stream()
        .map(level -> new VariableLevel(level.variableName(),
            new ExperimentalValue(level.experimentalValue().value(), unit)))
        .toList();
    experimentalVariable.replaceLevels(
        newLevels.stream().map(VariableLevel::experimentalValue).toList());
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
   * @param name           the name of this experimental group, which can be empty
   * @param variableLevels a list containing at least one value for a variable defined in this
   *                       experiment
   * @param sampleSize     the number of samples that are expected for this experimental group
   */
  Result<ExperimentalGroup, ResponseCode> addExperimentalGroup(String name,
      Collection<VariableLevel> variableLevels,
      int sampleSize) {
    variableLevels.forEach(Objects::requireNonNull);
    if (variableLevels.isEmpty()) {
      return Result.fromError(ResponseCode.EMPTY_VARIABLE);
    }

    for (VariableLevel level : variableLevels) {
      if (!isVariableDefined(level.variableName().value())) {
        throw new IllegalArgumentException(
            "There is no variable " + level.variableName().value() + " in this experiment.");
      }
    }

    Condition condition = Condition.create(variableLevels);
    if (isConditionDefined(condition)) {
      return Result.fromError(ResponseCode.CONDITION_EXISTS);
    }
    var newExperimentalGroup = ExperimentalGroup.create(name, condition, sampleSize, nextGroupId());
    experimentalGroups.add(newExperimentalGroup);
    return Result.fromValue(newExperimentalGroup);
  }

  private int nextGroupId() {
    return experimentalGroups.stream()
        .filter(group -> group.groupNumber() != null)
        .mapToInt(ExperimentalGroup::groupNumber).max().orElse(0) + 1;
  }

  /**
   * Updates an experimental group consisting of one or more levels of distinct variables and the
   * sample size and replaces it in the experimental design.
   * <p>
   * <ul>
   *   <li>If an experimental group with the same variable levels already exists, the creation will fail with an {@link ConditionExistsException} and no condition is added to the design.
   *   <li>If the {@link VariableLevel}s belong to variables not specified in this experiment, the creation will fail with an {@link IllegalArgumentException}
   *   <li>If the sample size is not at least 1, the creation will fail with an {@link IllegalArgumentException}
   * </ul>
   *
   * @param id             the unique identifier of the experimental group to update
   * @param name           the name of this experimental group, which can be empty
   * @param variableLevels a list containing at least one value for a variable defined in this
   *                       experiment
   * @param sampleSize     the number of samples that are expected for this experimental group
   */
  Result<ExperimentalGroup, ResponseCode> updateExperimentalGroup(long id, String name,
      Collection<VariableLevel> variableLevels, int sampleSize) {
    variableLevels.forEach(Objects::requireNonNull);
    if (variableLevels.isEmpty()) {
      return Result.fromError(ResponseCode.EMPTY_VARIABLE);
    }

    for (VariableLevel level : variableLevels) {
      if (!isVariableDefined(level.variableName().value())) {
        throw new IllegalArgumentException(
            "There is no variable " + level.variableName().value() + " in this experiment.");
      }
    }

    Condition condition = Condition.create(variableLevels);

    ExperimentalGroup groupToUpdate = experimentalGroups.stream()
        .filter(group -> id == group.id())
        .findFirst()
        .orElseThrow(() -> new ApplicationException(
            "No group with id %s exists in experimental design.".formatted(id)));
    groupToUpdate.setCondition(condition);
    groupToUpdate.setName(name);
    groupToUpdate.setSampleSize(sampleSize);

    return Result.fromValue(groupToUpdate);
  }

  List<ExperimentalGroup> getExperimentalGroups() {
    return experimentalGroups.stream().toList();
  }

  void removeExperimentalGroup(long groupId) {
    this.experimentalGroups.removeIf(experimentalGroup -> experimentalGroup.id() == groupId);
  }

  void removeExperimentalGroupByGroupNumber(int experimentalGroupNumber) {
    this.experimentalGroups.removeIf(
        experimentalGroup -> experimentalGroupNumber == experimentalGroup.groupNumber());
  }

  public record AddExperimentalGroupResponse(ResponseCode responseCode) {

    public enum ResponseCode {
      SUCCESS,
      CONDITION_EXISTS,
      EMPTY_VARIABLE
    }
  }
}
