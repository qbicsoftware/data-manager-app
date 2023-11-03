package life.qbic.projectmanagement.domain.model.experiment.vocabulary;

import java.io.Serializable;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;

/**
 * <a href="https://medical-dictionary.thefreedictionary.com/specimen">specimen</a>: A sample, as
 * of tissue, blood or urine, used for analysis and diagnosis.
 *
 * @param label a natural string representation of the {@link Specimen}
 */
public record Specimen(String label) implements Serializable {

  public Specimen {
    Objects.requireNonNull(label);
    if (label.isEmpty()) {
      throw new ApplicationException("Vocabulary label for Specimen is empty.");
    }
  }

  public static Specimen create(String label) {
    return new Specimen(label);
  }

  public String value() {
    return this.label();
  }

}
