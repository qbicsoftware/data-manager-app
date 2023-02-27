package life.qbic.projectmanagement.domain.project.experiment;

import java.io.Serializable;
import java.util.Random;
import java.util.StringJoiner;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.domain.project.experiment.repository.jpa.VariableNameAttributeConverter;

/**
 * <b>Experimental Variable</b>
 * <p>
 * Describes an experimental variable with a unique and declarative name. In addition, it contains
 * {@link ExperimentalValue}s, representing the levels of the variable that are part of the
 * experiment.
 * <p>
 * Experimental variables can be created via the
 * {@link Experiment#addVariableToDesign(String, ExperimentalValue...)} function.
 *
 * @since 1.0.0
 */
@Entity(name = "experimental_variables")
public class ExperimentalVariable {

  @EmbeddedId
  private ExperimentalVariableId id;

  @Convert(converter = VariableNameAttributeConverter.class)
  @Column(name = "name")
  private VariableName name;

  @ManyToOne
  @MapsId("experimentId")
  @JoinColumn(name = "experimentId")
  private Experiment experiment;

  @ElementCollection(fetch = FetchType.EAGER)
  private final List<ExperimentalValue> levels;

  private ExperimentalVariable(Experiment experiment, String name, ExperimentalValue... levels) {
    Objects.requireNonNull(experiment);
    Objects.requireNonNull(name);
    if (levels.length < 1) {
      throw new IllegalArgumentException("At least one variable required. Got " + levels.length);
    }
    this.experiment = experiment;
    this.id = ExperimentalVariableId.create(experiment.experimentId());
    this.name = VariableName.create(name);
    this.levels = List.of(levels);
  }

  public static ExperimentalVariable createForExperiment(Experiment experiment, String name,
      ExperimentalValue... levels) {
    return new ExperimentalVariable(experiment, name, levels);
  }

  protected ExperimentalVariable() {
    // used by JPA
    levels = new ArrayList<>();
  }

  /**
   * Calling this method ensures that the experimental value is set as a level on the variable.
   *
   * @param experimentalValue the experimental value to be added to possible levels
   */
  void addLevel(ExperimentalValue experimentalValue) {
    if (!levels.contains(experimentalValue)) {
      // the level is already part of the variable. No action needed
      levels.add(experimentalValue);
    }
  }

  public List<ExperimentalValue> levels() {
    return levels.stream().toList();
  }

  public VariableName name() {
    return name;
  }

  public ExperimentId experiment() {
    return id.experimentId();
  }


  public ExperimentalVariableId id() {
    return this.id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExperimentalVariable that = (ExperimentalVariable) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Embeddable
  @Access(AccessType.FIELD)
  public static class ExperimentalVariableId implements Serializable {

    private Long id;
    @Column(name = "experimentId")
    private ExperimentId experimentId;

    protected ExperimentalVariableId() {
      // needed for JPA
    }

    static ExperimentalVariableId create(ExperimentId experimentId) {
      Objects.requireNonNull(experimentId);
      ExperimentalVariableId experimentalVariableId = new ExperimentalVariableId();
      experimentalVariableId.experimentId = experimentId;
      experimentalVariableId.id = new Random().nextLong();
      return experimentalVariableId;
    }

    public ExperimentId experimentId() {
      return experimentId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ExperimentalVariableId that = (ExperimentalVariableId) o;

      if (!id.equals(that.id)) {
        return false;
      }
      return experimentId.equals(that.experimentId);
    }

    @Override
    public int hashCode() {
      int result = id.hashCode();
      result = 31 * result + experimentId.hashCode();
      return result;
    }
  }
}
