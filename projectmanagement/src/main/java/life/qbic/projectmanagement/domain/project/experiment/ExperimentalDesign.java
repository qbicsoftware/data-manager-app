package life.qbic.projectmanagement.domain.project.experiment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import life.qbic.application.commons.Result;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ExperimentalDesign {

  private final List<ExperimentalVariable<ExperimentalValue>> experimentalVariables;
  private final List<Condition> conditions;

  public ExperimentalDesign() {
    experimentalVariables = new ArrayList<>();
    conditions = new ArrayList<>();
  }

  public Result<ExperimentalVariable<ExperimentalValue>, Exception> createExperimentalVariable(
      String variableName, ExperimentalValue... levels) {
    return addExperimentalVariableOrElse(new ExperimentalVariable<>(variableName, levels),
        () -> Result.failure(new ExperimentalVariableExistsException(
            String.format(
                "A variable with name '%s' is already part of the experimental design. Variable names need to be unique within the design.",
                variableName))));
  }

  public Result<Condition, Exception> createCondition(
      VariableLevel<ExperimentalValue>... variableLevels) {
    return addToDesignOrElse(Condition.create(variableLevels), () -> Result.failure(
        new UnknownExperimentalVariableException(
            "At least one experiment variable is not defined in the design.")));
  }

  private Result<Condition, Exception> addToDesignOrElse(Condition condition,
      Supplier<Result<Condition, Exception>> failureResponse) {
    if (allVariablesPartOfDesign(condition.experimentalVariables())) {
      conditions.add(condition);
      return Result.success(condition);
    }
    return failureResponse.get();
  }

  private boolean allVariablesPartOfDesign(
      List<ExperimentalVariable<ExperimentalValue>> experimentalVariables) {
    for (var experimentalVariable : experimentalVariables) {
      if (isVariablePartOfDesign(experimentalVariable)) {
        continue;
      }
      return false;
    }
    return true;
  }

  private boolean isVariablePartOfDesign(ExperimentalVariable<ExperimentalValue> variableToCheck) {
    return this.experimentalVariables.stream()
        .anyMatch(variableInDesign -> variableInDesign.name().equals(variableToCheck.name()));
  }


  private Result<ExperimentalVariable<ExperimentalValue>, Exception> addExperimentalVariableOrElse(
      ExperimentalVariable<ExperimentalValue> experimentalVariable,
      Supplier<Result<ExperimentalVariable<ExperimentalValue>, Exception>> variableExists) {
    if (doesNotExist(experimentalVariable.name())) {
      experimentalVariables.add(experimentalVariable);
      return Result.success(experimentalVariable);
    }
    return variableExists.get();
  }

  private boolean doesNotExist(String name) {
    return experimentalVariables.stream().noneMatch(variable -> variable.name().equals(name));
  }

}
