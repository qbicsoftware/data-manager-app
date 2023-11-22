package life.qbic.projectmanagement.domain.model.experiment;


import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * Represents a biological replicate as part of an {@link ExperimentalGroup}.
 *
 * @since 1.0.0
 */
@Entity(name = "bio_replicate")
public class BiologicalReplicate implements Serializable {

  @EmbeddedId
  private BiologicalReplicateId id;
  private String label;
  @Serial
  private static final long serialVersionUID = 1551778532201183788L;

  private BiologicalReplicate(String label) {
    this.label = label;
    this.id = BiologicalReplicateId.create();
  }

  protected BiologicalReplicate() {
    // Needed for JPA
  }

  public static BiologicalReplicate create(String label) {
    return new BiologicalReplicate(label);
  }


  /**
   * Returns the biological replicate label.
   * <p>
   * Note: not the same as {@link BiologicalReplicate#id()}, which returns the unique identifier.
   *
   * @return the label of the replicate
   * @since 1.0.0
   */
  public String label() {
    return this.label;
  }

  /**
   * Returns the unique identifier of the replicate.
   *
   * @return the unique id
   * @since 1.0.0
   */
  public BiologicalReplicateId id() {
    return this.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, label);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BiologicalReplicate that = (BiologicalReplicate) o;
    return Objects.equals(id, that.id) && Objects.equals(label, that.label);
  }

  @Override
  public String toString() {
    return "BiologicalReplicate{" + "id=" + id + ", label='" + label + '\'' + '}';
  }

  /**
   * Provides sorting functionality for labels ending in numbers, e.g. label1 < label2 < label10.
   * This is based on label length and only works for labels starting with the same letters.
   */
  public static class LexicographicLabelComparator implements Comparator<BiologicalReplicate> {

    @Override
    public int compare(BiologicalReplicate r1, BiologicalReplicate r2) {
      int l1 = r1.label.length();
      int l2 = r2.label.length();
      if (l1 == l2) {
        return r1.label.compareTo(r2.label);
      } else {
        return l1-l2;
      }
    }
  }
}
