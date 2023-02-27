package life.qbic.projectmanagement.domain.project.experiment;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable.ExperimentalVariableId;
import life.qbic.projectmanagement.domain.project.experiment.repository.jpa.ConditionLabelAttributeConverter;

/**
 * <b>Condition</b>
 * <p>
 * Conditions represent different linear combinations of experimental variable level(s). For example
 * you want to compare different genotypes of a specimen, or different treatments. Let's assume you
 * have a experimental design with two variables:
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
 * Conditions in an experimental design can be defined via {@link Experiment#defineCondition(String, VariableLevel...)}.
 * <p>
 *
 * @since 1.0.0
 */
@Entity(name = "conditions")
//IMPORTANT: do not name the table condition; condition is a reserved keyword
public class Condition {


  @EmbeddedId
  private ConditionId id;

  @Convert(converter = ConditionLabelAttributeConverter.class)
  private ConditionLabel label;

  @ElementCollection(targetClass = VariableLevel.class, fetch = FetchType.EAGER)
  @JoinColumn(name = "conditionId")
  private List<VariableLevel> variableLevels;

  @ManyToOne
  @MapsId("experimentId")
  @JoinColumn(name = "experimentId")
  private Experiment experiment;

  protected Condition() {
    //used by jpa
  }

  /**
   * Creates a new instance of a condition object with one or more {@link VariableLevel} defining
   * the level of an experimental variable.
   *
   * @param definedVariables the linear combination of experimental variable levels
   * @return the condition
   * @since 1.0.0
   */
  public static Condition createForExperiment(Experiment experiment, String label,
      VariableLevel... definedVariables) {
    return new Condition(experiment, label, definedVariables);
  }


  private Condition(Experiment experiment, String label, VariableLevel... variableLevels) {
    Arrays.stream(variableLevels).forEach(Objects::requireNonNull);
    Objects.requireNonNull(experiment, "experiment must not be null");
    Objects.requireNonNull(label, "condition label must not be null");

    boolean allLevelsOfTheSameExperiment = Arrays.stream(variableLevels)
        .map(VariableLevel::variableId)
        .map(ExperimentalVariableId::experimentId)
        .allMatch(experimentId -> experimentId.equals(experiment.experimentId()));
    if (!allLevelsOfTheSameExperiment) {
      throw new IllegalArgumentException(
          "Please provide only variable levels of the experiment.");
    }

    if (variableLevels.length < 1) {
      throw new IllegalArgumentException("Please define at least one variable level.");
    }

    int distinctExperimentVariables = Arrays.stream(variableLevels)
        .map(VariableLevel::variableId)
        .collect(Collectors.toSet())
        .size();
    if (distinctExperimentVariables < variableLevels.length) {
      throw new IllegalArgumentException(
          "Variable levels are not from distinct experimental variables.");
    }
    this.id = ConditionId.create(experiment.experimentId());
    this.experiment = experiment;
    this.label = ConditionLabel.create(label);
    this.variableLevels = Arrays.stream(variableLevels).toList();
  }

  /**
   * Queries the {@link ExperimentalValue} of an experimental variable. If the condition does not
   * contain a value for the experimental variable, the result will be empty.
   *
   * @param variableName the experimental variable to look for
   * @return the value if present, else empty
   * @since 1.0.0
   */
  public Optional<ExperimentalValue> valueOf(String variableName) {
    return variableLevels.stream()
        .filter(level -> level.variableName().value().equals(variableName))
        .map(VariableLevel::experimentalValue).findAny();
  }

  public ConditionLabel label() {
    return label;
  }

  public boolean hasIdenticalContent(Condition other) {
    Comparator<VariableLevel> variableNameComparator = Comparator.comparing(
        l -> l.variableName().value());
    Stream<VariableLevel> sortedLevels = variableLevels.stream().sorted(variableNameComparator);
    return sortedLevels.equals(other.variableLevels.stream().sorted(variableNameComparator));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Condition condition = (Condition) o;

    return id.equals(condition.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Embeddable
  private static class ConditionId implements Serializable {

    private Long id;
    private ExperimentId experimentId;

    static ConditionId create(ExperimentId experimentId) {
      Objects.requireNonNull(experimentId);
      ConditionId conditionId = new ConditionId();
      conditionId.experimentId = experimentId;
      conditionId.id = new Random().nextLong();
      return conditionId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ConditionId that = (ConditionId) o;

      if (!experimentId.equals(that.experimentId)) {
        return false;
      }
      return id.equals(that.id);
    }

    @Override
    public int hashCode() {
      int result = experimentId.hashCode();
      result = 31 * result + id.hashCode();
      return result;
    }
  }


}
