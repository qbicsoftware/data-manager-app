package life.qbic.projectmanagement.domain.project.experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PostLoad;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.experiment.exception.ConditionExistsException;
import life.qbic.projectmanagement.domain.project.experiment.exception.ExperimentalVariableExistsException;
import life.qbic.projectmanagement.domain.project.experiment.exception.ExperimentalVariableNotDefinedException;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;


/**
 * <b>Experimental</b>
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project", nullable = false)
  private Project project;

  @EmbeddedId
  private ExperimentId experimentId;

  @Embedded
  private ExperimentalDesign experimentalDesign;

  @ElementCollection(targetClass = Analyte.class)
  private List<Analyte> analytes = new ArrayList<>();
  @ElementCollection(targetClass = Species.class)
  private List<Species> species = new ArrayList<>();
  @ElementCollection(targetClass = Specimen.class)
  private List<Specimen> specimens = new ArrayList<>();


  /**
   * Please use {@link Experiment#createForProject(Project, List, List, List)} instead
   */
  protected Experiment() {
    // Please use the create method. This is needed for JPA
  }

  @PostLoad
  private void loadCollections() {
    int analyteCount = analytes.size();
    int specimenCount = specimens.size();
    int speciesCount = species.size();
  }


  public static Experiment createForProject(Project project, List<Analyte> analytes,
      List<Specimen> specimens,
      List<Species> species) {
    Experiment experiment = new Experiment();
    experiment.experimentalDesign = ExperimentalDesign.create();
    experiment.experimentId = ExperimentId.create();
    experiment.project = project;
    experiment.addSpecies(species);
    experiment.addSpecimens(specimens);
    experiment.addAnalytes(analytes);
    return experiment;
  }

  /**
   * Provides a {@link VariableLevel} of the <code>value</code> for the variable
   * <code>variableName</code>. If the variable does not exist, or level creation failed, an
   * {@link Optional#empty()} is returned.
   *
   * @param value        the value of the variable
   * @param variableName the name of the variable
   */
  public Optional<VariableLevel> getLevel(String variableName,
      ExperimentalValue value) {
    return experimentalDesign.getLevel(variableName, value);
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
  public Result<ExperimentalValue, Exception> addLevelToVariable(String variableName,
      ExperimentalValue level) {
    return experimentalDesign.addLevelToVariable(variableName, level);
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
  public Collection<Species> getSpecies() {
    return species.stream().toList();
  }

  /**
   * @return the collection of specimens in this experiment
   */
  public Collection<Specimen> getSpecimens() {
    return specimens.stream().toList();
  }

  /**
   * @return the collection of analytes in this experiment
   */
  public Collection<Analyte> getAnalytes() {
    return analytes.stream().toList();
  }

  /**
   * Adds {@link Specimen}s to the experimental design. If the design contains more than one
   * specimen, the {@link ExperimentalVariable} <code>specimen</code> is created and all specimens
   * are added as levels.
   *
   * @param specimens The specimens to add to the experimental design
   */
  public void addSpecimens(Collection<Specimen> specimens) {
    final String specimensVariableName = "specimen";

    if (specimens.size() < 1) {
      throw new IllegalArgumentException(
          "Did not get any specimen to add.");
    }
    // only add specimen that are not present already
    List<Specimen> newSpecimens = specimens.stream()
        .filter(it -> !specimens.contains(it))
        .toList();
    this.specimens.addAll(newSpecimens);

    if (this.specimens.size() > 1) {
      // we have more than 1 specimen, thus a new variable is created or levels are added
      List<ExperimentalValue> levels = specimens.stream()
          .map(it -> ExperimentalValue.create(it.label()))
          .toList();
      addVariableOrLevels(specimensVariableName, levels);
    }
  }

  /**
   * Adds {@link Analyte}s to the experimental design. If the design contains more than one analyte,
   * the {@link ExperimentalVariable} <code>analyte</code> is created and all analytes are added as
   * levels.
   *
   * @param analytes          The analytes to add to the experimental design
   */
  public void addAnalytes(Collection<Analyte> analytes) {
    final String analytesVariableName = "analyte";

    if (analytes.size() < 1) {
      throw new IllegalArgumentException(
          "Did not get any analyte to add.");
    }

    // only add analytes that are not present already
    List<Analyte> newAnalytes = analytes.stream()
        .filter(it -> !analytes.contains(it))
        .toList();
    this.analytes.addAll(newAnalytes);

    if (this.analytes.size() > 1) {
      // we have mone than 1 analyte, thus a new variable is created or levels are added
      List<ExperimentalValue> levels = analytes.stream()
          .map(it -> ExperimentalValue.create(it.label()))
          .toList();
      addVariableOrLevels(analytesVariableName, levels);
    }
  }

  /**
   * Adds {@link Species}s to the experimental design. If the design contains more than one species,
   * the {@link ExperimentalVariable} <code>species</code> is created and all species are added as
   * levels.
   *
   * @param species The species to add to the experimental design
   */
  public void addSpecies(Collection<Species> species) {
    final String speciesVariableName = "species";

    if (species.size() < 1) {
      throw new IllegalArgumentException(
          "Did not get any species to add.");
    }
    // only add specimen that are not present already
    List<Species> newSpecies = species.stream()
        .filter(it -> !species.contains(it))
        .toList();
    this.species.addAll(newSpecies);

    // check whether we need a variable
    if (this.species.size() > 1) {
      // we have smore than 1 species, thus a new variable is created or levels are added
      List<ExperimentalValue> levels = species.stream()
          .map(it -> ExperimentalValue.create(it.label()))
          .toList();

      addVariableOrLevels(speciesVariableName, levels);
    }
  }

  /**
   * Creates a new experimental variable and adds it to the experimental design. A successful
   * operation is indicated in the result, which can be verified via {@link Result#isSuccess()}.
   * <p>
   * <b>Note</b>: If a variable with the provided name already exists, the creation will fail with
   * an {@link ExperimentalVariableExistsException} and no variable is added to the design. You can
   * check via {@link Result#isFailure()} if this is the case.
   *
   * @param variableName a declarative and unique name for the variable
   * @param levels       a list containing at least one value for the variable
   * @return a {@link Result} object containing the {@link VariableName} or contains declarative
   * exceptions. The result will contain an {@link ExperimentalVariableExistsException} if the
   * variable already exists or an {@link IllegalArgumentException} if no level has been provided.
   * @since 1.0.0
   */
  public Result<VariableName, Exception> addVariableToDesign(String variableName,
      List<ExperimentalValue> levels) {
    if (levels.size() < 1) {
      return Result.failure(new IllegalArgumentException(
          "At least one level required. Got " + levels));
    }

    if (experimentalDesign.isVariableDefined(variableName)) {
      return Result.failure(new ExperimentalVariableExistsException(
          "A variable with the name " + variableName + " already exists."));
    }
    try {
      ExperimentalVariable variable = ExperimentalVariable.createForExperiment(this, variableName,
          levels.toArray(ExperimentalValue[]::new));
      experimentalDesign.variables.add(variable);
      return Result.success(variable.name());
    } catch (IllegalArgumentException e) {
      return Result.failure(e);
    }
  }

  /**
   * Adds a variable to the design with the levels specified. If the variable exists already then
   * adds the levels to the variable.
   *
   * @param variableName the name of the variable
   * @param levels       the levels of the variable
   */
  private void addVariableOrLevels(String variableName, List<ExperimentalValue> levels) {
    addVariableToDesign(variableName, levels).ifFailure(
        e -> {
          if (e instanceof ExperimentalVariableExistsException) {
            // at this point we know there is a variable with the name `specimen`, so we only need to add the levels
            for (ExperimentalValue level : levels) {
              experimentalDesign.addLevelToVariable(variableName, level).ifFailure(
                  e2 -> {
                    //FIXME what exception to throw here?
                    //   we know that the experimental variable must be defined at that point so we do not expect this exception
                    throw new RuntimeException(
                        "could not add level " + level + " to variable "
                            + variableName, e2);
                  });
            }
          } else if (e instanceof IllegalArgumentException) {
            //FIXME what exception to throw here?
            throw new RuntimeException(e);
          }
        }
    );
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
  public Result<ConditionLabel, Exception> addConditionToDesign(String conditionLabel,
      VariableLevel[] levels) {
    Arrays.stream(levels).forEach(Objects::requireNonNull);

    for (VariableLevel level : levels) {
      if (!experimentalDesign.isVariableDefined(level.variableName().value())) {
        return Result.failure(new IllegalArgumentException(
            "There is no variable " + level.variableName().value() + " in this experiment."));
      }
    }
    boolean areAllLevelsFromDefinedVariables = Arrays.stream(levels)
        .allMatch(it -> experimentalDesign.isVariableDefined(it.variableName().value()));
    if (!areAllLevelsFromDefinedVariables) {
      return Result.failure(
          new IllegalArgumentException(
              "Not all levels are from variables defined in this experiment"));
    }

    try {
      Condition condition = Condition.createForExperiment(this, conditionLabel, levels);
      if (experimentalDesign.isConditionDefined(conditionLabel)) {
        return Result.failure(new ConditionExistsException(
            "please provide a different condition label. A condition with the label "
                + conditionLabel + " exists."));
      }
      if (experimentalDesign.containsConditionWithSameLevels(condition)) {
        return Result.failure(new ConditionExistsException(
            "A condition containing the provided levels exists."));
      }
      experimentalDesign.conditions.add(condition);
      return Result.success(condition.label());
    } catch (IllegalArgumentException e) {
      return Result.failure(e);
    }
  }
}
