package life.qbic.projectmanagement.domain.model.sample;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * The sample code of a sample.
 * <li> Must not be empty or null
 */
public class SampleCode implements Serializable {

  @Serial
  private static final long serialVersionUID = -4338664985893856989L;

  private String code;

  protected SampleCode() {
    // needed for JPA
  }

  private SampleCode(String code) throws IllegalArgumentException{
    Objects.requireNonNull(code, "Sample code must not be null");
    if (code.isBlank()) {
      throw new IllegalArgumentException("Sample code must not be blank");
    }
    this.code = code;
  }

  public static SampleCode create(String code) throws IllegalArgumentException {
    return new SampleCode(code);
  }

  public String code() {
    return this.code;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SampleCode that = (SampleCode) o;
    return Objects.equals(code, that.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code);
  }
}
