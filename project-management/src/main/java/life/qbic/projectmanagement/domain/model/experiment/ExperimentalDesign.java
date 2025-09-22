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
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalDesign.AddExperimentalGroupResponse.ResponseCode;
import life.qbic.projectmanagement.domain.model.experiment.exception.ConditionExistsException;
import life.qbic.projectmanagement.domain.model.experiment.exception.ExperimentalVariableExistsException;
import life.qbic.projectmanagement.domain.model.experiment.exception.ExperimentalVariableNotDefinedException;

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

  private boolean isLevelUsed(VariableLevel variableLevel) {
    String variableName = variableLevel.variableName().value();
    Optional<ExperimentalVariable> optionalExperimentalVariable = variableWithName(variableName);
    if (optionalExperimentalVariable.isEmpty()) {
      throw new ExperimentalVariableNotDefinedException(
          "No variable with name " + variableName + " exists");
    }

    return definedConditions().anyMatch(it -> it.contains(variableLevel));
  }

  private Stream<Condition> definedConditions() {
    return experimentalGroups.parallelStream()
        .map(ExperimentalGroup::condition)
        .distinct();
  }

  private List<VariableLevel> removeLevels(ExperimentalVariable experimentalVariable,
      List<VariableLevel> variableLevels) {
    var removedLevels = new ArrayList<VariableLevel>();
    for (VariableLevel variableLevel : variableLevels) {
      boolean wasModified = experimentalVariable.removeLevel(variableLevel);
      if (wasModified) {
        removedLevels.add(variableLevel);
      }
    }
    return removedLevels;
  }

  private List<VariableLevel> addLevels(ExperimentalVariable experimentalVariable,
      List<ExperimentalValue> values) {
    var addedLevels = new ArrayList<VariableLevel>();
    for (ExperimentalValue value : values) {
      boolean wasModified = experimentalVariable.addLevel(value);
      if (wasModified) {
        addedLevels.add(VariableLevel.create(experimentalVariable.name(), value));
      }
    }
    return addedLevels;
  }

  boolean setVariableLevels(String variableName, List<ExperimentalValue> levels) {
    Optional<ExperimentalVariable> optionalExperimentalVariable = variableWithName(variableName);
    if (optionalExperimentalVariable.isEmpty()) {
      throw new ExperimentalVariableNotDefinedException(
          "No variable with name " + variableName + " exists");
    }
    ExperimentalVariable experimentalVariable = optionalExperimentalVariable.orElseThrow();
    List<VariableLevel> levelsToRemove = experimentalVariable.levels()
        .stream()
        .filter(it -> !levels.contains(it.experimentalValue()))
        .toList();

    List<VariableLevel> usedLevels = levelsToRemove.stream().filter(this::isLevelUsed).toList();

    if (!usedLevels.isEmpty()) {
      throw new IllegalArgumentException(
          "Variable levels " + usedLevels + " are already in use and cannot be deleted.");
    }

    var levelSnapshot = experimentalVariable.levels();

    List<VariableLevel> removedLevels;

    var levelsToAdd = levels.stream()
        .filter(it -> experimentalVariable.levels().stream().noneMatch(level ->
            level.experimentalValue().equals(it)))
        .toList();
    List<VariableLevel> addedLevels;
    try {
      addedLevels = addLevels(experimentalVariable, levelsToAdd);
      removedLevels = removeLevels(experimentalVariable, levelsToRemove);
      return !addedLevels.isEmpty() || !removedLevels.isEmpty();
    } catch (RuntimeException e) {
      experimentalVariable.replaceLevels(levelSnapshot);
      throw e;
    }
  }

  boolean addExperimentalVariable(ExperimentalVariable experimentalVariable) {
    if (!experimentalGroups.isEmpty()) {
      throw new IllegalStateException("There are already experimental groups defined");
    }
    Optional<ExperimentalVariable> optionalExperimentalVariable = variableWithName(
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
    variables.removeIf(it -> it.name().value().equals(variableName));
    for (ExperimentalGroup experimentalGroup : experimentalGroups) {
      experimentalGroup.condition().removeVariable(variableName);
    }
    return true;
  }

  void renameExperimentalVariable(String oldName, String newName) {
    RuntimeException noVariableException = new ExperimentalVariableNotDefinedException(
        "No variable with name " + oldName + " exists");
    if (variableWithName(oldName).isEmpty()) {
      throw noVariableException;
    }
    if (variableWithName(newName).isPresent()) {
      throw new ExperimentalVariableExistsException(
          "Variable with the name " + newName + " already exists.");
    }
    try {
      renameVariableInGroups(oldName, newName);
    } catch (RuntimeException e) {
      renameVariableInGroups(newName, oldName);
      throw e;
    }
    try {
      renameVariable(oldName, newName);
    } catch (RuntimeException e) {
      renameVariableInGroups(newName, oldName);
      throw e;
    }
  }


  private void renameVariable(String from, String to) {
    for (ExperimentalVariable experimentalVariable : variables) {
      if (experimentalVariable.name().value().equals(from)) {
        experimentalVariable.renameTo(to);
        return;
      }
    }
    throw new ExperimentalVariableNotDefinedException("No variable with name " + from + " exists");
  }

  private void renameVariableInGroups(String oldName, String newName) {
    for (ExperimentalGroup experimentalGroup : experimentalGroups) {
      experimentalGroup.renameVariable(oldName, newName);
    }
  }

  List<ExperimentalVariable> experimentalVariables() {
    return Collections.unmodifiableList(variables);
  }

  private Optional<ExperimentalVariable> variableWithName(String variableName) {
    return variables.stream().filter(it -> it.name().value().equals(variableName)).findAny();
  }

  private boolean isVariableDefined(String variableName) {
    return variables.stream().anyMatch(it -> it.name().value().equals(variableName));
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

  @Deprecated(forRemoval = true, since = "1.10.2")
  Result<ExperimentalVariable, Exception> addVariable(String variableName,
      List<ExperimentalValue> levels) {
    if (levels.isEmpty()) {
      return Result.fromError(new IllegalArgumentException(
          "No levels were defined for " + variableName));
    }

    if (isVariableDefined(variableName)) {
      return Result.fromError(new ExperimentalVariableExistsException(
          "A variable with the name " + variableName + " already exists."));
    }
    try {
      ExperimentalVariable variable = ExperimentalVariable.create(variableName,
          levels.toArray(ExperimentalValue[]::new));
      variables.add(variable);
      return Result.fromValue(variable);
    } catch (IllegalArgumentException e) {
      return Result.fromError(e);
    }
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
   */
  void changeUnit(String variableName, String unit) {
    ExperimentalVariable experimentalVariable = getVariable(variableName).orElseThrow(() ->
        new IllegalArgumentException("Variable " + variableName + " does not exist."));
    if (experimentalVariable.usedUnit().map(usedUnit -> usedUnit.equals(unit)).orElse(false)) {
      return;
    }
    if (experimentalVariable.levels().stream().anyMatch(this::isLevelUsed)) {
      throw new IllegalStateException("Levels are in use. Unit change not possible");
    }
    List<VariableLevel> newLevels = experimentalVariable.levels().stream()
        .map(level -> new VariableLevel(level.variableName(),
            new ExperimentalValue(level.experimentalValue().value(), unit)))
        .toList();
    experimentalVariable.replaceLevels(newLevels);
  }

  /**
   * removes an experimental variable from the design
   *
   * @param variable the variable to be removed
   */
  boolean removeExperimentalVariable(ExperimentalVariable variable) {
    return variables.remove(variable);
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
