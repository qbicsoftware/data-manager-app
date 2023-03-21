package life.qbic.projectmanagement.domain.project.experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.experiment.exception.ConditionExistsException;
import life.qbic.projectmanagement.domain.project.experiment.exception.ExperimentalVariableNotDefinedException;
import life.qbic.projectmanagement.domain.project.experiment.exception.VariableLevelExistsException;

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
 *   <li>{@link Condition}</li>
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
 * <b>Note:</b> variables need to be unique, and its name will be checked against already defined variables in the design! If a variable with the same
 * name is already part of the design, the method will return a {@link Result#failure(Exception)}.
 *
 * <p>
 * <b>2. Define a Condition</b>
 * <p>
 * {@link Condition}s represent different linear combinations of experimental variable level(s). For example you want to compare different genotypes of a specimen,
 * or different treatments. Let's assume you have a experimental design with two variables:
 * <ul>
 * <li>(1) the genotype (wildtype vs. mutant) </li>
 * <li>(2) a treatment a solvent with different concentrations (0 mmol/L and 150 mmol/L)</li>
 * </ul>
 * <p>
 * So in total there will be four conditions:
 *
 * <ul>
 *   <li>(1) wildtype + 0 mmol/L</li>
 *   <li>(2) wildtype + 150 mmol/L</li>
 *   <li>(3) mutant + 0 mmol/L</li>
 *   <li>(4) mutant + 150 mmol/L</li>
 * </ul>
 * <p>
 * Conditions can be defined via {@link #defineCondition(String, VariableLevel[])}.
 * <p>
 * <b>Note:</b> The {@link ExperimentalVariable} referenced in the {@link VariableLevel} is required for defining a {@link Condition}.
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
  // @JoinColumn so no extra table is created as conditions contains that column
  final Collection<Condition> conditions = new ArrayList<>();

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
    int conditionCount = conditions.size();
  }


  /**
   * Adds a level to an experimental variable with the given name. A successful operation is
   * indicated in the result, which can be verified via {@link Result#isSuccess()}.
   * <p>
   * <b>Note</b>: If a variable with the provided name is not defined in the design, the creation
   * will fail with an {@link ExperimentalVariableNotDefinedException}. You can check via
   * {@link Result#isFailure()} if this is the case.
   *
   * @param variableName a declarative and unique name for the variable
   * @param level        the value to be added to the levels of that variable
   * @return a {@link Result} object containing the added level value or declarative exceptions. The
   * result will contain an {@link ExperimentalVariableNotDefinedException} if no variable with the
   * provided name is defined in this design.
   * @since 1.0.0
   */
  Result<ExperimentalValue, Exception> addLevelToVariable(String variableName,
      ExperimentalValue level) {
    Optional<ExperimentalVariable> experimentalVariableOptional = variableWithName(variableName);
    if (experimentalVariableOptional.isEmpty()) {
      return Result.failure(
          new ExperimentalVariableNotDefinedException(
              "There is no variable with name " + variableName));
    }
    ExperimentalVariable experimentalVariable = experimentalVariableOptional.get();
    Result<ExperimentalValue, RuntimeException> addLevelResult = experimentalVariable.addLevel(
        level);
    if (addLevelResult.isSuccess()) {
      return Result.success(level);
    } else if (addLevelResult.isFailure()
        && addLevelResult.exception() instanceof VariableLevelExistsException) {
      // we don't care that the level is present already as this is what we wanted to achieve.
      return Result.success(level);
    } else {
      // we could not add the level
      return Result.failure(addLevelResult.exception());
    }
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
    result = 31 * result + conditions.hashCode();
    return result;
  }

  /**
   * Whether a {@link Condition} is defined in this experimental design named with the same label.
   *
   * @param conditionLabel the label of the condition
   * @return true if there is a condition with label <code>conditionLabel</code>; false otherwise
   */
  boolean isConditionDefined(String conditionLabel) {
    return conditions.stream()
        .map(Condition::label)
        .anyMatch(label -> label.value().equals(conditionLabel));
  }

  /**
   * Whether this design contains a condition with the same {@link VariableLevel}s
   *
   * @param condition the condition to check for
   * @return true if there is a condition with the same variable levels; false otherwise.
   */
  boolean isConditionWithSameLevelsDefined(Condition condition) {
    return conditions.stream().anyMatch(c -> c.hasIdenticalContent(condition));
  }

  /**
   * Provides a {@link VariableLevel} of the <code>value</code> for the variable
   * <code>variableName</code>. If the variable does not exist, or level creation failed, an
   * {@link Optional#empty()} is returned.
   *
   * @param value        the value of the variable
   * @param variableName the name of the variable
   */
  Optional<VariableLevel> getLevel(String variableName, ExperimentalValue value) {
    Objects.requireNonNull(variableName);
    Objects.requireNonNull(value);
    Optional<ExperimentalVariable> variableOptional = variables.stream()
        .filter(it -> it.name().value().equals(variableName))
        .findAny();

    return variableOptional.map(variable -> {
      try {
        return new VariableLevel(variable, value);
      } catch (RuntimeException e) {
        return null;
      }
    });
  }

  /**
   * Creates a new condition and adds it to the experimental design. A successful operation is
   * indicated in the result, which can be verified via {@link Result#isSuccess()}.
   * <p>
   * <b>Note</b>: {@link Result#isFailure()} indicates a failed operation.
   * {@link Result#exception()} can be used to determine the cause of the failure.
   * <ul>
   *   <li>If a condition with the provided label or the same variable levels already exists, the creation will fail with an {@link ConditionExistsException} and no condition is added to the design.
   *   <li>If the {@link VariableLevel}s belong to variables not specified in this experiment, the creation will fail with an {@link IllegalArgumentException}
   * </ul>
   *
   * @param conditionLabel a declarative and unique name for the condition in scope of this
   *                       experiment.
   * @param levels         at least one value for the variable
   * @return a {@link Result} object containing the {@link ConditionLabel} or containing a
   * declarative exceptions.
   */
  public Result<ConditionLabel, Exception> defineCondition(String conditionLabel,
      VariableLevel[] levels) {
    Arrays.stream(levels).forEach(Objects::requireNonNull);

    for (VariableLevel level : levels) {
      if (!isVariableDefined(level.variableName().value())) {
        return Result.failure(new IllegalArgumentException(
            "There is no variable " + level.variableName().value() + " in this experiment."));
      }
    }

    if (isConditionDefined(conditionLabel)) {
      return Result.failure(new ConditionExistsException(
          "please provide a different condition label. A condition with the label "
              + conditionLabel + " exists."));
    }

    try {
      Condition condition = Condition.create(conditionLabel, levels);
      if (isConditionWithSameLevelsDefined(condition)) {
        return Result.failure(new ConditionExistsException(
            "A condition containing the provided levels exists."));
      }
      conditions.add(condition);
      return Result.success(condition.label());
    } catch (IllegalArgumentException e) {
      return Result.failure(e);
    }
  }
}
