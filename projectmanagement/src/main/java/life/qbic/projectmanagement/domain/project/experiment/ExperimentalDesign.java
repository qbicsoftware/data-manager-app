package life.qbic.projectmanagement.domain.project.experiment;

import java.util.ArrayList;
import java.util.List;
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

  public ExperimentalDesign() {
    experimentalVariables = new ArrayList<>();
  }

  public Result<ExperimentalVariable<ExperimentalValue>, Exception> createExperimentalVariable(
      String variableName, ExperimentalValue... levels) {
    if (doesNotExist(variableName)) {
      experimentalVariables.add(new ExperimentalVariable<>(variableName, levels));
      return Result.success(new ExperimentalVariable<>(variableName, levels));
    } else {
      return Result.failure(new ExperimentalVariableExistsException(
          String.format(
              "A variable with name '%s' is already part of the experimental design. Variable names need to be unique within the design.",
              variableName)));
    }
  }

  private boolean doesNotExist(String name) {
    return experimentalVariables.stream().noneMatch(variable -> variable.name().equals(name));
  }


}
