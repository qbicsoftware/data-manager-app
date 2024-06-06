package life.qbic.projectmanagement.domain.model.experiment;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalDesign.AddExperimentalGroupResponse.ResponseCode;
import life.qbic.projectmanagement.domain.model.experiment.exception.ConditionExistsException;
import life.qbic.projectmanagement.domain.model.experiment.exception.ExperimentalVariableExistsException;
import life.qbic.projectmanagement.domain.model.experiment.exception.ExperimentalVariableNotDefinedException;


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
  @Column(name = "speciesIconName")
  private String speciesIconName;
  @Column(name = "specimenIconName")
  private String specimenIconName;
  @Column(name = "analyteIconName")
  private String analyteIconName;

  @ElementCollection(targetClass = OntologyTerm.class)
  @Column(name = "analytes", columnDefinition = "longtext CHECK (json_valid(`analytes`))")
  //FIXME should be `analyte`in the database and here
  private List<OntologyTerm> analytes = new ArrayList<>();
  @ElementCollection(targetClass = OntologyTerm.class)
  @Column(name = "species", columnDefinition = "longtext CHECK (json_valid(`species`))")
  private List<OntologyTerm> species = new ArrayList<>();
  @ElementCollection(targetClass = OntologyTerm.class)
  @Column(name = "specimens", columnDefinition = "longtext CHECK (json_valid(`specimens`))")
  //FIXME should be `specimen`in the database and here
  private List<OntologyTerm> specimens = new ArrayList<>();
  private static final String defaultIconName = "default";

  /**
   * Please use {@link Experiment#create(String)} instead
   */
  protected Experiment() {
    // Please use the create method. This is needed for JPA
  }

  public static Experiment create(String name) {
    Experiment experiment = new Experiment();
    experiment.name = name;
    experiment.experimentalDesign = ExperimentalDesign.create();
    experiment.experimentId = ExperimentId.create();
    experiment.speciesIconName = defaultIconName;
    experiment.specimenIconName = defaultIconName;
    experiment.analyteIconName = defaultIconName;

    return experiment;
  }

  @PostLoad
  private void loadCollections() {
    int analyteCount = analytes.size();
    int specimenCount = specimens.size();
    int speciesCount = species.size();
  }

  /**
   * Returns the name of the experiment.
   *
   * @return the name of the experiment
   */
  public String getName() {
    return name;
  }

  public String getSpeciesIconName() {
    return speciesIconName;
  }

  public String getSpecimenIconName() {
    return specimenIconName;
  }

  public String getAnalyteIconName() {
    return analyteIconName;
  }

  public void setIconNames(String speciesIconName, String specimenIconName, String analyteIconName) {
    this.speciesIconName = validateIconName(speciesIconName);
    this.specimenIconName = validateIconName(specimenIconName);
    this.analyteIconName = validateIconName(analyteIconName);
  }

  private String validateIconName(String iconName) {
    requireNonNull(iconName, "Icon names must not be null");
    if(iconName.isBlank()) {
      throw new ApplicationException("Icon names must not be blank");
    }
    return iconName;
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
  public Collection<OntologyTerm> getSpecies() {
    return species.stream().toList();
  }

  /**
   * @return the collection of specimens in this experiment
   */
  public Collection<OntologyTerm> getSpecimens() {
    return specimens.stream().toList();
  }

  /**
   * @return the collection of analytes in this experiment
   */
  public Collection<OntologyTerm> getAnalytes() {
    return analytes.stream().toList();
  }

  /**
   * Adds specimens to the experiment.
   *
   * @param specimens The specimens to add to the experiment
   */
  public void addSpecimens(Collection<OntologyTerm> specimens) {
    if (specimens.isEmpty()) {
      return;
    }
    List<OntologyTerm> missingSpecimens = specimens.stream()
        .filter(specimen -> !this.specimens.contains(specimen))
        .distinct()
        .toList();
    this.specimens.addAll(missingSpecimens);
  }

  /**
   * Removes all experimental variables AND all experimental groups.
   *
   * @since 1.0.0
   */
  public void removeAllExperimentalVariables() {
    removeAllExperimentalGroups();
    experimentalDesign.removeAllExperimentalVariables();
  }

  /**
   * Removes experimental groups with the provided ids from the experiment .
   *
   * @since 1.0.0
   */
  public void removeExperimentalGroups(List<Long> ids) {
    ids.forEach(experimentalDesign::removeExperimentalGroup);
  }

  /**
   * Removes all experimental groups in an experiment.
   *
   * @since 1.0.0
   */
  public void removeAllExperimentalGroups() {
    for (ExperimentalGroup experimentalGroup : experimentalDesign.getExperimentalGroups()) {
      experimentalDesign.removeExperimentalGroup(experimentalGroup.id());
    }
  }

  /**
   * Adds analytes to the experiment.
   *
   * @param analytes The analytes to add to the experiment
   */
  public void addAnalytes(Collection<OntologyTerm> analytes) {
    if (analytes.isEmpty()) {
      return;
    }

    // only add analytes that are not present already
    List<OntologyTerm> missingAnalytes = analytes.stream()
        .filter(analyte -> !this.analytes.contains(analyte))
        .distinct()
        .toList();
    this.analytes.addAll(missingAnalytes);
  }

  /**
   * Adds species to the experiment.
   *
   * @param species The species to add to the experiment
   */
  public void addSpecies(Collection<OntologyTerm> species) {
    if (species.isEmpty()) {
      return;
    }
    // only add species that are not present already
    List<OntologyTerm> missingSpecies = species.stream()
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
   * @param groupName      the name of this experimental group, which can be empty
   * @param variableLevels at least one value for a variable defined in this experiment
   * @param sampleSize     the number of samples that are expected for this experimental group
   * @return
   */
  public Result<ExperimentalGroup, ResponseCode> addExperimentalGroup(String groupName,
      Collection<VariableLevel> variableLevels,
      int sampleSize) {
    return experimentalDesign.addExperimentalGroup(groupName, variableLevels, sampleSize);
  }

  /**
   * Updates an experimental group of to the experimental design.
   * <p>
   * <ul>
   *   <li>If an experimental group with the same variable levels already exists, the creation will fail with an {@link ConditionExistsException} and no condition is added to the design.
   *   <li>If the {@link VariableLevel}s belong to variables not specified in this experiment, the creation will fail with an {@link IllegalArgumentException}
   *   <li>If the sample size is not at least 1, the creation will fail with an {@link IllegalArgumentException}
   * </ul>
   *
   * @param id             the unique identifier of the experimental group to update
   * @param groupName      the name of this experimental group, which can be empty
   * @param variableLevels at least one value for a variable defined in this experiment
   * @param sampleSize     the number of samples that are expected for this experimental group
   * @return
   */
  public Result<ExperimentalGroup, ResponseCode> updateExperimentalGroup(long id, String groupName,
      Collection<VariableLevel> variableLevels,
      int sampleSize) {
    return experimentalDesign.updateExperimentalGroup(id, groupName, variableLevels, sampleSize);
  }

  public List<ExperimentalGroup> getExperimentalGroups() {
    return experimentalDesign.getExperimentalGroups();
  }

  public void removeExperimentGroup(long groupId) {
    experimentalDesign.removeExperimentalGroup(groupId);
  }

  /**
   * Sets the name of the experiment.
   */
  public void setName(String name) {
    if (name.isEmpty()) {
      throw new ApplicationException("An Experiment must have a name");
    }
    this.name = name;
  }

  /**
   * Sets the list of species for an experiment.
   */
  public void setSpecies(
      List<OntologyTerm> species) {
    if (species == null || species.isEmpty()) {
      throw new ApplicationException(ErrorCode.NO_SPECIES_DEFINED,
          ErrorParameters.of(species));
    }
    this.species = species;
  }

  /**
   * Sets the list of specimen for an experiment.
   */
  public void setSpecimens(
      List<OntologyTerm> specimens) {
    if (specimens == null || specimens.isEmpty()) {
      throw new ApplicationException(ErrorCode.NO_SPECIMEN_DEFINED,
          ErrorParameters.of(specimens));
    }
    this.specimens = specimens;
  }

  /**
   * Sets the list of analytes for an experiment.
   */
  public void setAnalytes(
      List<OntologyTerm> analytes) {
    if (analytes == null || analytes.isEmpty()) {
      throw new ApplicationException(ErrorCode.NO_ANALYTE_DEFINED,
          ErrorParameters.of(analytes));
    }
    this.analytes = analytes;
  }
}
