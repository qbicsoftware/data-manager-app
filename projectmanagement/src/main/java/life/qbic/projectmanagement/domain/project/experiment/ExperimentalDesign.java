package life.qbic.projectmanagement.domain.project.experiment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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
  private List<SampleGroup> sampleGroups;

  public ExperimentalDesign() {
    experimentalVariables = new ArrayList<>();
    conditions = new ArrayList<>();
    sampleGroups = new ArrayList<>();
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

  public Result<SampleGroup, Exception> createSampleGroup(String label, int biologicalReplicates,
      Long conditionId) {
    Optional<Condition> result = findCondition(conditionId);
    if (result.isEmpty()) {
      return Result.failure(
          new UnknownConditionException("Condition with id %d not part of the design"));
    }
    if (findByLabel(label).isPresent()) {
      return Result.failure(
          new SampleGroupExistsException("Sample Group with label %s already exists"));
    }
    var sampleGroup = SampleGroup.with(label, result.get(), biologicalReplicates);
    this.sampleGroups.add(sampleGroup);
    return Result.success(sampleGroup);
  }

  public Iterator<SampleGroup> sampleGroupIterator() {
    return sampleGroups.listIterator();
  }

  public List<SampleGroup> sampleGroups() {
    return sampleGroups.stream().toList();
  }

  public Optional<SampleGroup> findByLabel(String label) {
    return sampleGroups().stream().filter(sampleGroup -> sampleGroup.label().equals(label))
        .findAny();
  }

  private Optional<Condition> findCondition(Long conditionId) {
    return conditions.stream().filter(condition -> condition.id().equals(conditionId)).findAny();
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
