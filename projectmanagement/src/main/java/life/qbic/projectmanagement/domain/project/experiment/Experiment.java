package life.qbic.projectmanagement.domain.project.experiment;

import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.experiment.exception.ExperimentalVariableExistsException;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

import javax.persistence.*;
import java.util.*;


@Entity(name = "experiments_datamanager")
public class Experiment {

  @ManyToOne
  private Project project;

  @EmbeddedId
  private ExperimentId experimentId;

  @Embedded
  private ExperimentalDesign experimentalDesign;


  protected Experiment() {
    experimentalDesign = new ExperimentalDesign();
  }


  public static Experiment create(Project project, List<Analyte> analytes, List<Specimen> specimens,
      List<Species> species) {
    Experiment experiment = new Experiment();
    ExperimentalDesign experimentalDesign = ExperimentalDesign.create(analytes, specimens, species);
    experiment.experimentId = ExperimentId.create();
    experiment.project = project;
    experiment.experimentalDesign = experimentalDesign;
    return experiment;
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
   * @param levels       at least one or more values for the variable
   * @return a {@link Result} object containing the new variable or contains declarative exceptions.
   * The result will contain an {@link ExperimentalVariableExistsException} if the variable already
   * exists or an {@link IllegalArgumentException} if no level has been provided.
   * @since 1.0.0
   */
  public Result<String, Exception> addVariableToDesign(String variableName,
      ExperimentalValue... levels) {
    if (levels.length < 1) {
      return Result.failure(new IllegalArgumentException(
          "At least one level required. Got " + Arrays.deepToString(levels)));
    }

    if (experimentalDesign.isVariableDefined(variableName)) {
      return Result.failure(new ExperimentalVariableExistsException(
          "A variable with the name " + variableName + " already exists."));
    }
    try {
      ExperimentalVariable variable = ExperimentalVariable.createForExperiment(
          this, variableName, levels);
      this.experimentalDesign.variables.add(variable);
    } catch (IllegalArgumentException e) {
      return Result.failure(e);
    }
    return Result.success(variableName);
  }

  /**
   * TODO
   *
   * @param conditionLabel
   * @param levels
   * @return
   */
  public Result<String, Exception> defineCondition(String conditionLabel,
      VariableLevel... levels) {
    Arrays.stream(levels).forEach(Objects::requireNonNull);

    try {
      Condition condition = Condition.createForExperiment(this, conditionLabel, levels);
      if (experimentalDesign.isConditionDefined(conditionLabel)) {
        //TODO label is not available <- same id
        return Result.failure(new RuntimeException(
            "please provide a different condition label. A condition with the label "
                + conditionLabel + " exists."));
      }
      if (experimentalDesign.containsConditionWithSameLevels(condition)) {
        //TODO another condition with the same levels is already defined <- same content
      }
      this.experimentalDesign.conditions.add(condition);
    } catch (IllegalArgumentException e) {
      return Result.failure(e);
    }
    return Result.success(conditionLabel);
  }

  /**
   * Adds
   *
   * @param variableName
   * @param level
   * @return a Result object containing the value of the added level or a declarative exception. The
   * result will contain an IllegalArgumentException if the variable already exists or an
   * IllegalArgumentException if no level has been provided.
   */
  public Result<ExperimentalValue, Exception> addLevelToVariable(String variableName,
      ExperimentalValue level) {
    return experimentalDesign.addLevelToVariable(variableName, level);
  }

  public ExperimentId experimentId() {
    return experimentId;
  }

  /**
   * @param sampleGroupName
   * @param conditionName
   * @param biologicalReplicates
   * @return sample group name
   */
  String addSampleGroupForCondition(String sampleGroupName, String conditionName,
      int biologicalReplicates) {
    throw new RuntimeException("not implemented");
  }


  public Collection<Species> getSpecies() {
    return experimentalDesign.species.stream().toList();
  }

  public Collection<Specimen> getSpecimens() {
    return experimentalDesign.specimens.stream().toList();
  }

  public Collection<Analyte> getAnalytes() {
    return experimentalDesign.analytes.stream().toList();
  }

  public void addSpecies(Species... species) {
    final String speciesVariableName = "species";

    if (species.length < 1) {
      throw new IllegalArgumentException(
          "Did not get any species to add.");
    }
    // only add specimen that are not present already
    List<Species> newSpecies = Arrays.stream(species)
        .filter(it -> !experimentalDesign.species.contains(it))
        .toList();
    experimentalDesign.species.addAll(newSpecies);

    // check whether we need a variable
    if (experimentalDesign.species.size() > 1) {
      // we already have species, thus a new variable is created
      ExperimentalValue[] levels = experimentalDesign.species.stream()
          .map(it -> ExperimentalValue.create(it.label()))
          .toArray(ExperimentalValue[]::new);

      addVariableOrLevels(speciesVariableName, levels);
    }
  }

  /**
   * TODO: document and make code nicer
   *
   * @param specimens
   */
  public void addSpecimens(Specimen... specimens) {
    final String specimensVariableName = "specimen";

    if (specimens.length < 1) {
      throw new IllegalArgumentException(
          "Did not get any specimen to add.");
    }
    // only add specimen that are not present already
    List<Specimen> newSpecimens = Arrays.stream(specimens)
        .filter(it -> !experimentalDesign.specimens.contains(it))
        .toList();
    experimentalDesign.specimens.addAll(newSpecimens);

    if (experimentalDesign.noSpecimenPresent()) {
      // we do not have any specimens yet
      experimentalDesign.specimens.addAll(List.of(specimens));
    } else {
      // we already have specimen, thus a new variable is created
      ExperimentalValue[] levels = experimentalDesign.specimens.stream()
          .map(it -> ExperimentalValue.create(it.label()))
          .toArray(ExperimentalValue[]::new);

      addVariableOrLevels(specimensVariableName, levels);
    }
  }

  public void addAnalytes(Analyte... analytes) {
    final String analytesVariableName = "analyte";

    if (analytes.length < 1) {
      throw new IllegalArgumentException(
          "Did not get any analyte to add.");
    }
    if (experimentalDesign.analytes.size() > 0) {
      experimentalDesign.analytes.addAll(List.of(analytes));
      // we already have species, thus a new variable is created
      ExperimentalValue[] levels = experimentalDesign.analytes.stream()
          .map(it -> ExperimentalValue.create(it.label()))
          .toArray(ExperimentalValue[]::new);

      Result<String, Exception> variableAdded = addVariableToDesign(analytesVariableName,
          levels);
      if (variableAdded.isSuccess()) {
        return;
      }
      if (variableAdded.isFailure()
          && variableAdded.exception() instanceof ExperimentalVariableExistsException) {
        // at this point we know there is a variable with the name `analyte`, so we only need to add the levels
        for (ExperimentalValue level : levels) {
          addLevelToVariable(analytesVariableName, level);
        }
      }
    }
  }

  /**
   * add a variable, or if it exists add missing levels to the variable
   *
   * @param variableName
   * @param levels
   */
  private void addVariableOrLevels(String variableName, ExperimentalValue[] levels) {
    addVariableToDesign(variableName, levels).ifSuccessOrElse(
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

}
