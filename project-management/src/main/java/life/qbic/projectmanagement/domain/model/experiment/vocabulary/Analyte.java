package life.qbic.projectmanagement.domain.model.experiment.vocabulary;

import java.io.Serializable;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;

/**
 * <a href="https://medical-dictionary.thefreedictionary.com/analyte">analyte</a>: Any material or
 * chemical substance subjected to analysis.
 *
 * @param label a natural string representation of the {@link Analyte}
 */
public record Analyte(String label) implements Serializable {

  public Analyte {
    Objects.requireNonNull(label);
    if (label.isEmpty()) {
      throw new ApplicationException("Vocabulary label for Analyte is empty.");
    }
  }

  public static Analyte create(String label) {
    return new Analyte(label);
  }

  public String value() {
    return this.label();
  }
}
