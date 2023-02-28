package life.qbic.projectmanagement.domain.project.experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.experiment.exception.ExperimentalVariableExistsException;
import life.qbic.projectmanagement.domain.project.experiment.exception.ExperimentalVariableNotDefinedException;
import life.qbic.projectmanagement.domain.project.experiment.exception.VariableLevelExistsException;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

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
// TODO JAVADOC
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
  // so no extra table is created as experimental_variables contains that column
  final List<ExperimentalVariable> variables = new ArrayList<>();

  @OneToMany(targetEntity = Condition.class, orphanRemoval = true, cascade = CascadeType.ALL)
  @JoinColumn(name = "experimentId")
  // so no extra table is created as conditions contains that column
  final Collection<Condition> conditions = new ArrayList<>();

  public Collection<Condition> conditions() {
    return conditions;
  }

  protected ExperimentalDesign() {
    // needed for JPA
  }

  public static ExperimentalDesign create() {
    return new ExperimentalDesign();
  }

  /*do we need the lists to be populated? Can we use @Transactional instead?
    benefits (+) and draw-backs (-)
      (+) less load on the database when designs are loaded
      (-) we cannot be sure the collections are loaded and might get LazyInvocationExceptions
      (-) we have to be careful to use @Transactional in the context where experimental designs are loaded from the database. In application services for example
   */
  @PostLoad
  private void loadCollections() {
    int analyteCount = analytes.size();
    int specimenCount = specimens.size();
    int speciesCount = species.size();
    int variableCount = variables.size();
    int conditionCount = conditions.size();
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

  private Optional<ExperimentalVariable> variableWithName(String variableName) {
    return variables.stream().filter(it -> it.name().value().equals(variableName)).findAny();
  }

  public boolean isVariableDefined(String variableName) {
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

  public boolean isConditionDefined(String conditionLabel) {
    return conditions.stream()
        .map(Condition::label)
        .anyMatch(label -> label.equals(conditionLabel));
  }

  public boolean containsConditionWithSameLevels(Condition condition) {
    return conditions.stream().anyMatch(c -> c.hasIdenticalContent(condition));
  }

  Result<String, Exception> addVariableToDesign(String variableName,
      ExperimentalValue[] levels, Experiment experiment) {
    if (levels.length < 1) {
      return Result.failure(new IllegalArgumentException(
          "At least one level required. Got " + Arrays.deepToString(levels)));
    }

    if (isVariableDefined(variableName)) {
      return Result.failure(new ExperimentalVariableExistsException(
          "A variable with the name " + variableName + " already exists."));
    }
    try {
      ExperimentalVariable variable = ExperimentalVariable.createForExperiment(experiment,
          variableName, levels);
      variables.add(variable);
    } catch (IllegalArgumentException e) {
      return Result.failure(e);
    }
    return Result.success(variableName);
  }

  Result<VariableLevel, Exception> getLevels(String variableName, ExperimentalValue value) {
    Objects.requireNonNull(variableName);
    Objects.requireNonNull(value);
    Optional<ExperimentalVariable> variableOptional = variables.stream()
        .filter(it -> it.name().value().equals(variableName))
        .findAny();
    if (variableOptional.isEmpty()) {
      throw new IllegalArgumentException(
          "There is no variable " + variableName + "in this experiment");
    }
    try {
      var level = new VariableLevel(variableOptional.get(), value);
      return Result.success(level);
    } catch (RuntimeException e) {
      return Result.failure(e);
    }
  }

  Result<String, Exception> addConditionToDesign(String conditionLabel,
      VariableLevel[] levels, Experiment experiment) {
    Arrays.stream(levels).forEach(Objects::requireNonNull);

    for (VariableLevel level : levels) {
      if (!isVariableDefined(level.variableName().value())) {
        return Result.failure(new IllegalArgumentException(
            "There is no variable " + level.variableName().value() + " in this experiment."));
      }
    }
    boolean areAllLevelsFromDefinedVariables = Arrays.stream(levels)
        .allMatch(it -> isVariableDefined(it.variableName().value()));
    if (!areAllLevelsFromDefinedVariables) {
      return Result.failure(
          new IllegalArgumentException(
              "Not all levels are from variables defined in this experiment"));
    }

    try {
      Condition condition = Condition.createForExperiment(experiment, conditionLabel, levels);
      if (isConditionDefined(conditionLabel)) {
        //TODO label is not available <- same id
        return Result.failure(new RuntimeException(
            "please provide a different condition label. A condition with the label "
                + conditionLabel + " exists."));
      }
      if (containsConditionWithSameLevels(condition)) {
        //TODO another condition with the same levels is already defined <- same content
        return Result.failure(new RuntimeException(
            "A condition containing the provided levels exists."));
      }
      conditions.add(condition);
    } catch (IllegalArgumentException e) {
      return Result.failure(e);
    }
    return Result.success(conditionLabel);
  }

  /**
   * add a variable, or if it exists add missing levels to the variable
   *
   * @param variableName
   * @param levels
   * @param experiment
   */
  void addVariableOrLevels(String variableName, ExperimentalValue[] levels, Experiment experiment) {
    experiment.addVariableToDesign(variableName, levels).ifSuccessOrElse(
        v -> {
        },
        e -> {
          if (e instanceof ExperimentalVariableExistsException) {
            // at this point we know there is a variable with the name `specimen`, so we only need to add the levels
            for (ExperimentalValue level : levels) {
              addLevelToVariable(variableName, level).ifSuccessOrElse(
                  v2 -> {
                  },
                  e2 -> {
                    //TODO what exception to throw here?
                    throw new RuntimeException(
                        "could not add level " + level + " to variable "
                            + variableName, e2);
                  });
            }
          } else if (e instanceof IllegalArgumentException) {
            //TODO what exception to throw here?
            throw new RuntimeException(e);
          }
        }
    );
  }

  void addSpecies(Species[] species, Experiment experiment) {
    final String speciesVariableName = "species";

    if (species.length < 1) {
      throw new IllegalArgumentException(
          "Did not get any species to add.");
    }
    // only add specimen that are not present already
    List<Species> newSpecies = Arrays.stream(species)
        .filter(it -> !this.species.contains(it))
        .toList();
    this.species.addAll(newSpecies);

    // check whether we need a variable
    if (this.species.size() > 1) {
      // we have smore than 1 species, thus a new variable is created or levels are added
      ExperimentalValue[] levels = this.species.stream()
          .map(it -> ExperimentalValue.create(it.label()))
          .toArray(ExperimentalValue[]::new);

      addVariableOrLevels(speciesVariableName, levels, experiment);
    }
  }

  void addSpecimens(Specimen[] specimens, Experiment experiment) {
    final String specimensVariableName = "specimen";

    if (specimens.length < 1) {
      throw new IllegalArgumentException(
          "Did not get any specimen to add.");
    }
    // only add specimen that are not present already
    List<Specimen> newSpecimens = Arrays.stream(specimens)
        .filter(it -> !this.specimens.contains(it))
        .toList();
    this.specimens.addAll(newSpecimens);

    if (this.specimens.size() > 1) {
      // we have more than 1 specimen, thus a new variable is created or levels are added
      ExperimentalValue[] levels = this.specimens.stream()
          .map(it -> ExperimentalValue.create(it.label()))
          .toArray(ExperimentalValue[]::new);
      addVariableOrLevels(specimensVariableName, levels, experiment);
    }
  }

  void addAnalytes(Analyte[] analytes, Experiment experiment) {
    final String analytesVariableName = "analyte";

    if (analytes.length < 1) {
      throw new IllegalArgumentException(
          "Did not get any analyte to add.");
    }

    // only add analytes that are not present already
    List<Analyte> newAnalytes = Arrays.stream(analytes)
        .filter(it -> !this.analytes.contains(it))
        .toList();
    this.analytes.addAll(newAnalytes);

    if (this.analytes.size() > 1) {
      this.analytes.addAll(List.of(analytes));
      // we have mone than 1 analyte, thus a new variable is created or levels are added
      ExperimentalValue[] levels = this.analytes.stream()
          .map(it -> ExperimentalValue.create(it.label()))
          .toArray(ExperimentalValue[]::new);
      addVariableOrLevels(analytesVariableName, levels, experiment);
    }
  }
}
