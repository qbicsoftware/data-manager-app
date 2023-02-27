package life.qbic.projectmanagement.domain.project.experiment;

import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.experiment.exception.ExperimentalVariableNotDefinedException;
import life.qbic.projectmanagement.domain.project.experiment.exception.SampleGroupExistsException;
import life.qbic.projectmanagement.domain.project.experiment.exception.UnknownConditionException;
import life.qbic.projectmanagement.domain.project.experiment.exception.UnknownExperimentalVariableException;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

import javax.persistence.*;
import java.util.*;

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
 *   <li>{@link SampleGroup}</li>
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
 * Experimental variables can be created via the {@link ExperimentalDesign#addExperimentalVariable(String, ExperimentalValue...)} function.
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
 * Conditions can be defined via {@link ExperimentalDesign#createCondition(VariableLevel[])}.
 * <p>
 * <b>Note:</b> The {@link ExperimentalVariable} defined in the {@link VariableLevel} needs to be defined in the experimental design first ({@link ExperimentalDesign#addExperimentalVariable(String, ExperimentalValue...)})
 *
 * <p>
 * <b>3. Define a Sample Group</b>
 * <p>
 * A {@link SampleGroup} can be defined via {@link ExperimentalDesign#createSampleGroup(String, int, Long)} and represent
 * a logical container of biological replicates of one condition in an experimental design.
 *
 * @since 1.0.0
 */
@Embeddable
public class ExperimentalDesign {

  @ElementCollection(targetClass = Analyte.class)
  final List<Analyte> analytes = new ArrayList<>();
  @ElementCollection(targetClass = Species.class)
  final List<Species> species = new ArrayList<>();
  @ElementCollection(targetClass = Specimen.class)
  final List<Specimen> specimens = new ArrayList<>();
  @OneToMany(targetEntity = ExperimentalVariable.class, orphanRemoval = true, cascade = CascadeType.ALL)
  @JoinColumn(name = "experimentId")
  final List<ExperimentalVariable> variables = new ArrayList<>();

  @OneToMany(targetEntity = Condition.class, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "experimentId")
  final Collection<Condition> conditions = new ArrayList<>();

  public Collection<Condition> conditions() {
    return conditions;
  }
//
//  @OneToMany(targetEntity = Condition.class, orphanRemoval = true, cascade = CascadeType.ALL)
//  final List<Condition> conditions = new ArrayList<>();

//  final List<Condition> conditions;
//  private final List<SampleGroup> sampleGroups;


  public ExperimentalDesign() {

  }

  @PostLoad
  private void loadCollections() {
    int analyteCount = analytes.size();
    int specimenCount = specimens.size();
    int speciesCount = species.size();
    int variableCount = variables.size();
  }

  private ExperimentalDesign(List<Analyte> analytes, List<Specimen> specimens,
      List<Species> species) {
    this.analytes.addAll(analytes);
    this.specimens.addAll(specimens);
    this.species.addAll(species);
  }

  public static ExperimentalDesign create(List<Analyte> analytes, List<Specimen> specimens,
      List<Species> species) {
    return new ExperimentalDesign(analytes, specimens, species);
  }


  /**
   * Adds a level to an experimental variable with the given name. A successful operation is
   * indicated in the result, which can be verified via {@link Result#isSuccess()}.
   * <p>
   * <b>Note</b>: If a variable with the provided name is not defined in the design, the creation
   * will fail with
   * an {@link ExperimentalVariableNotDefinedException}. You can check via
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
    experimentalVariable.addLevel(level);
    return Result.success(level);
  }

  private Optional<ExperimentalVariable> variableWithName(String variableName) {
    return variables.stream().filter(it -> it.name().equals(variableName)).findAny();
  }

  public boolean isVariableDefined(String variableName) {
    return variables.stream().anyMatch(it -> it.name().equals(variableName));
  }
  /**
   * Creates and adds a new condition to the experimental design. A successful creation can e
   * verified via {@link Result#isSuccess()}.
   * <p>
   * <b>Note:</b> All experimental variables in the condition need to be already part of the design
   * and created via
   * {@link ExperimentalDesign#addExperimentalVariable(String, ExperimentalValue...)}. Otherwise,
   * the operation will fail and the result object contain an
   * {@link UnknownExperimentalVariableException}.
   *
   * @param variableLevels one more levels of distinct experimental variables
   * @return a result object with the created condition, or with an exception if the creation
   * failed.
   * @since 1.0.0
   *//*
  public final Result<Condition, Exception> createCondition(VariableLevel... variableLevels) {
    if (!new HashSet<>(this.variableLevels).containsAll(List.of(variableLevels))) {
      return Result.failure(
              new UnknownExperimentalVariableException(
                      "At least one experiment variable is not defined in the design."));
    }
    VariableLevel[] levels = Arrays.copyOf(variableLevels, variableLevels.length);
    Condition condition = null;
    try {
      condition = Condition.create("condition" + conditions.size(), levels); //assumption: no condition is removed ever
      conditions.add(condition);
    } catch (IllegalArgumentException e) {
      return Result.failure(e);
    }
    return Result.success(condition);
  }


  */

  /**
   * Creates a new sample group and adds it to the experimental design.
   * <p>
   * A sample group represents a certain condition (experimental variable level combination) within
   * an experiment and the amount of true biological replicates.
   * <p>
   * <b>Note:</b> the referenced condition needs to be already defined in the experimental design
   * using {@link ExperimentalDesign#createCondition(VariableLevel[])}. Also, the label for the
   * sample group needs to be unique within the design.
   *
   * @param label                A unique textual tag representing the group within the design
   * @param biologicalReplicates the number of true biological replicates of the group
   * @param conditionId          the condition id that references the condition that is represented
   *                             in the sample group
   * @return a {@link Result} object indicates if the operation was successful when it returns the
   * sample group object that has been created. If the operation failed, the result object will
   * contain an exception. A {@link UnknownConditionException} is present, if the referenced
   * condition is not part of the design, {@link SampleGroupExistsException} will be present a
   * sample group with the same label already exists in the design.
   * @since 1.0.0
   *//*
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
    return this.variableLevels.stream()
        .anyMatch(variableInDesign -> variableInDesign.name().equals(variableToCheck.name()));
  }


  private Result<ExperimentalVariable<ExperimentalValue>, Exception> addExperimentalVariableOrElse(
      ExperimentalVariable<ExperimentalValue> experimentalVariable,
      Supplier<Result<ExperimentalVariable<ExperimentalValue>, Exception>> variableExists) {
    if (doesNotExist(experimentalVariable)) {
      variableLevels.add(experimentalVariable);
      return Result.success(experimentalVariable);
    }
    return variableExists.get();
  }

  private boolean doesNotExist(ExperimentalVariable<ExperimentalValue> experimentalVariable) {
    return variableLevels.stream()
        .noneMatch(variable -> variable.equals(experimentalVariable));
  }
*/
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ExperimentalDesign design = (ExperimentalDesign) o;

    if (!analytes.equals(design.analytes)) {
      return false;
    }
    if (!species.equals(design.species)) {
      return false;
    }
    if (!specimens.equals(design.specimens)) {
      return false;
    }
    return variables.equals(design.variables);
  }

  @Override
  public int hashCode() {
    int result = analytes.hashCode();
    result = 31 * result + species.hashCode();
    result = 31 * result + specimens.hashCode();
    result = 31 * result + variables.hashCode();
    return result;
  }

  boolean noSpecimenPresent() {
    return specimens.isEmpty();
  }

  boolean noSpeciesPresent() {
    return species.isEmpty();
  }

  boolean noAnalytePresent() {
    return analytes.isEmpty();
  }

  public boolean isConditionDefined(String conditionLabel) {
    return conditions.stream()
        .map(Condition::label)
        .anyMatch(label -> label.equals(conditionLabel));
  }

  public boolean containsConditionWithSameLevels(Condition condition) {
    return conditions.stream().anyMatch(c -> c.hasIdenticalContent(condition));
  }
}
