package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class ExperimentalVariable<T extends ExperimentalValue> {

  private final List<T> levels;

  private final String name;

  @SafeVarargs
  public ExperimentalVariable(String name, T... levels) {
    if (levels.length < 1) {
      throw new IllegalArgumentException("No levels provided. Please provide at least one.");
    }
    this.name = name;
    this.levels = Collections.unmodifiableList(Arrays.asList(levels));
  }

  public List<T> levels() {
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
