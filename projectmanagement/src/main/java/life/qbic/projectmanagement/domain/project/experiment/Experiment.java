package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Collection;
import java.util.List;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.experiment.exception.ExperimentalVariableExistsException;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;


@Entity(name = "experiments_datamanager")
public class Experiment {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project", nullable = false)
  private Project project;

  @EmbeddedId
  private ExperimentId experimentId;

  @Embedded
  private ExperimentalDesign experimentalDesign;


  protected Experiment() {
    // please use the create method. This is needed for JPA
  }


  public static Experiment createForProject(Project project, List<Analyte> analytes,
      List<Specimen> specimens,
      List<Species> species) {
    Experiment experiment = new Experiment();
    ExperimentalDesign experimentalDesign = ExperimentalDesign.create();
    experiment.experimentalDesign = experimentalDesign;
    experiment.experimentId = ExperimentId.create();
    experiment.project = project;
    experimentalDesign.addSpecies(species.toArray(Species[]::new), experiment);
    experimentalDesign.addSpecimens(specimens.toArray(Specimen[]::new), experiment);
    experimentalDesign.addAnalytes(analytes.toArray(Analyte[]::new), experiment);
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
    return experimentalDesign.addVariableToDesign(variableName, levels, this);
  }

  public Result<VariableLevel, Exception> getLevel(String variableName,
      ExperimentalValue value) {
    return experimentalDesign.getLevels(variableName, value);
  }

  /**
   * TODO
   *
   * @param conditionLabel
   * @param levels
   * @return
   */
  public Result<String, Exception> addConditionToDesign(String conditionLabel,
      VariableLevel... levels) {
    return experimentalDesign.addConditionToDesign(conditionLabel, levels, this);
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

  /**
   * @return the identifier of this experiment
   */
  public ExperimentId experimentId() {
    return experimentId;
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
    experimentalDesign.addSpecies(species, this);
  }

  /**
   * TODO: document and make code nicer
   *
   * @param specimens
   */
  public void addSpecimens(Specimen... specimens) {
    experimentalDesign.addSpecimens(specimens, this);
  }

  /**
   * TODO
   *
   * @param analytes
   */
  public void addAnalytes(Analyte... analytes) {
    experimentalDesign.addAnalytes(analytes, this);
  }

}
