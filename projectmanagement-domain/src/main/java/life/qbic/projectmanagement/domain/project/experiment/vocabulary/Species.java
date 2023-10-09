package life.qbic.projectmanagement.domain.project.experiment.vocabulary;

import java.io.Serializable;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;

/**
 * <a href="https://medical-dictionary.thefreedictionary.com/species">species</a>: a taxonomic
 * category subordinate to a genus (or subgenus) and superior to a subspecies or variety; composed
 * of individuals similar in certain morphologic and physiologic characteristics.
 *
 * @param label a natural string representation of the {@link Species}
 */
public record Species(String label) implements Serializable {

  public Species {
    Objects.requireNonNull(label);
    if (label.isEmpty()) {
      throw new ApplicationException("Vocabulary label for Species is empty.");
    }
  }

  public static Species create(String label) {
    return new Species(label);
  }

  public String value() {
    return this.label();
  }
}
