package life.qbic.controlling.domain.model.experiment;

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
import life.qbic.application.commons.Result;
import life.qbic.controlling.domain.model.experiment.ExperimentalDesign.AddExperimentalGroupResponse.ResponseCode;
import life.qbic.controlling.domain.model.experiment.exception.ConditionExistsException;
import life.qbic.controlling.domain.model.experiment.exception.ExperimentalVariableExistsException;
import life.qbic.controlling.domain.model.experiment.exception.ExperimentalVariableNotDefinedException;

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

  public static ExperimentalDesign create() {
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

  /**
   * Adds a level to an experimental variable with the given name. A successful operation is
   * indicated in the result, which can be verified via {@link Result#isValue()}.
   * <p>
   * <b>Note</b>: If a variable with the provided name is not defined in the design, the creation
   * will fail with an {@link ExperimentalVariableNotDefinedException}. You can check via
   * {@link Result#isError()} if this is the case.
   *
   * @param variableName a declarative and unique name for the variable
   * @param level        the value to be added to the levels of that variable
   * @return a {@link Result} object containing the added level value or declarative exceptions. The
   * result will contain an {@link ExperimentalVariableNotDefinedException} if no variable with the
   * provided name is defined in this design.
   * @since 1.0.0
   */
  Result<VariableLevel, Exception> addLevelToVariable(String variableName,
      ExperimentalValue level) {
    Optional<ExperimentalVariable> experimentalVariableOptional = variableWithName(variableName);
    if (experimentalVariableOptional.isEmpty()) {
      return Result.fromError(
          new ExperimentalVariableNotDefinedException(
              "There is no variable with name " + variableName));
    }
    ExperimentalVariable experimentalVariable = experimentalVariableOptional.get();
    return experimentalVariable.addLevel(level);
  }

  List<ExperimentalVariable> variables() {
    return Collections.unmodifiableList(variables);
  }

  private Optional<ExperimentalVariable> variableWithName(String variableName) {
    return variables.stream().filter(it -> it.name().value().equals(variableName)).findAny();
  }

  boolean isVariableDefined(String variableName) {
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
  boolean isConditionDefined(Condition condition) {
    return experimentalGroups.stream().anyMatch(it -> it.condition().equals(condition));
  }

  Result<VariableName, Exception> addVariable(String variableName, List<ExperimentalValue> levels) {
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
      return Result.fromValue(variable.name());
    } catch (IllegalArgumentException e) {
      return Result.fromError(e);
    }
  }

    public void removeAllExperimentalVariables() throws IllegalStateException {
      if (!experimentalGroups.isEmpty()) {
        throw new IllegalStateException("Cannot delete experimental variables referenced by an experimental group.");
      }
      this.variables.clear();
    }

    public record AddExperimentalGroupResponse(ResponseCode responseCode) {

    public enum ResponseCode {
      SUCCESS,
      CONDITION_EXISTS
    }
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
   * @param variableLevels a list containing at least one value for a variable defined in this
   *                       experiment
   * @param sampleSize     the number of samples that are expected for this experimental group
   */
  public Result<ExperimentalGroup, ResponseCode> addExperimentalGroup(Collection<VariableLevel> variableLevels,
      int sampleSize) {
    variableLevels.forEach(Objects::requireNonNull);
    if (variableLevels.isEmpty()) {
      throw new IllegalArgumentException("at least one variable level is required");
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
    var newExperimentalGroup = ExperimentalGroup.create(condition, sampleSize);
    experimentalGroups.add(newExperimentalGroup);
    return Result.fromValue(newExperimentalGroup);
  }

  public List<ExperimentalGroup> getExperimentalGroups() {
    return experimentalGroups.stream().toList();
  }

  public void removeExperimentalGroup(long groupId) {
    this.experimentalGroups.removeIf(experimentalGroup -> experimentalGroup.id() == groupId);
  }
}
