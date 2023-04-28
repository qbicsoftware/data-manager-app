package life.qbic.projectmanagement.domain.project.experiment;

import jakarta.persistence.*;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign.AddExperimentalGroupResponse;
import life.qbic.projectmanagement.domain.project.experiment.exception.ConditionExistsException;
import life.qbic.projectmanagement.domain.project.experiment.exception.ExperimentalVariableExistsException;
import life.qbic.projectmanagement.domain.project.experiment.exception.ExperimentalVariableNotDefinedException;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * <b>Experiment</b>
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

  @EmbeddedId
  private ExperimentId experimentId;
  @Column(name = "experimentName")
  private String name;

  @Embedded
  private ExperimentalDesign experimentalDesign;
  @ElementCollection(targetClass = Analyte.class)
  private List<Analyte> analytes = new ArrayList<>();
  @ElementCollection(targetClass = Species.class)
  private List<Species> species = new ArrayList<>();
  @ElementCollection(targetClass = Specimen.class)
  private List<Specimen> specimens = new ArrayList<>();


  /**
   * Please use {@link Experiment#create(String)} instead
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


  public static Experiment create(String name) {
    Experiment experiment = new Experiment();
    experiment.name = name;
    experiment.experimentalDesign = ExperimentalDesign.create();
    experiment.experimentId = ExperimentId.create();
    return experiment;
  }

  /**
   * Returns the name of the experiment.
   *
   * @return the name of the experiment
   */
  public String getName() {
    return name;
  }

  /**
   * Retrieves the list of experimental variables stored within the Experiment.
   *
   * @return Provides the list of {@link ExperimentalVariable} defined within the
   * {@link ExperimentalDesign} of the Experiment
   */

  public List<ExperimentalVariable> variables() {
    return experimentalDesign.variables();
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
  public Result<VariableLevel, Exception> addLevelToVariable(String variableName,
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
   * Adds {@link Specimen}s to the experiment.
   *
   * @param specimens The specimens to add to the experiment
   */
  public void addSpecimens(Collection<Specimen> specimens) {
    if (specimens.isEmpty()) {
      return;
    }
    List<Specimen> missingSpecimens = specimens.stream()
        .filter(specimen -> !this.specimens.contains(specimen))
        .distinct()
        .toList();
    this.specimens.addAll(missingSpecimens);
  }

  /**
   * Adds {@link Analyte}s to the experiment.
   *
   * @param analytes The analytes to add to the experiment
   */
  public void addAnalytes(Collection<Analyte> analytes) {
    if (analytes.isEmpty()) {
      return;
    }

    // only add analytes that are not present already
    List<Analyte> missingAnalytes = analytes.stream()
        .filter(analyte -> !this.analytes.contains(analyte))
        .distinct()
        .toList();
    this.analytes.addAll(missingAnalytes);
  }

  /**
   * Adds {@link Species}s to the experiment.
   *
   * @param species The species to add to the experiment
   */
  public void addSpecies(Collection<Species> species) {
    if (species.isEmpty()) {
      return;
    }
    // only add specimen that are not present already
    List<Species> missingSpecies = species.stream()
        .filter(speci -> !this.species.contains(speci))
        .distinct()
        .toList();
    this.species.addAll(missingSpecies);
  }

  /**
   * Creates a new experimental variable and adds it to the experimental design. A successful
   * operation is indicated in the result, which can be verified via {@link Result#isValue()}.
   * <p>
   * <b>Note</b>: If a variable with the provided name already exists, the creation will fail with
   * an {@link ExperimentalVariableExistsException} and no variable is added to the design. You can
   * check via {@link Result#isError()} if this is the case.
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
    return experimentalDesign.addVariable(variableName, levels);
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
   * @param variableLevels at least one value for a variable defined in this experiment
   * @param sampleSize     the number of samples that are expected for this experimental group
   * @return
   */
  public AddExperimentalGroupResponse addExperimentalGroup(Collection<VariableLevel> variableLevels,
                                                           int sampleSize) {
    return experimentalDesign.addExperimentalGroup(variableLevels, sampleSize);
  }

  public Set<ExperimentalGroup> getExperimentalGroups() {
    return experimentalDesign.getExperimentalGroups();
  }
}
