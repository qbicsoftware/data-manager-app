package life.qbic.projectmanagement.domain.project.experiment;

import java.util.List;
import java.util.Objects;

/**
 * <b>Experimental Variable</b>
 * <p>
 * Describes an experimental variable with a unique and declarative name. In addition, it contains
 * {@link ExperimentalValue}s, representing the levels of the variable that are part of the
 * experiment.
 * <p>
 * Experimental variables can be created via the
 * {@link ExperimentalDesign#createExperimentalVariable(String, ExperimentalValue...)} function.
 *
 * @since 1.0.0
 */
public class ExperimentalVariable<T extends ExperimentalValue> {

  private final List<T> levels;

  private final String name;

  @SafeVarargs
  public ExperimentalVariable(String name, T... levels) {
    if (levels.length < 1) {
      throw new IllegalArgumentException("No levels provided. Please provide at least one.");
    }
    this.name = name;
    this.levels = List.of(levels);
  }

  public List<T> values() {
    return levels.stream().toList();
  }

  public String name() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExperimentalVariable<?> that = (ExperimentalVariable<?>) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
