package life.qbic.projectmanagement.application.measurement.foobar.jpa;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.Embeddable;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Embeddable
public class NGSSampleSpecificMetadata {

  private SampleId sampleId;
  private String indexI5;
  private String indexI7;
  private String comment;

  protected NGSSampleSpecificMetadata() {
    //for JPA specification
  }

  public NGSSampleSpecificMetadata(SampleId sampleId, String indexI5, String indexI7,
      String comment) {
    this.sampleId = requireNonNull(sampleId, "sampleId must not be null");
    this.indexI5 = indexI5;
    this.indexI7 = indexI7;
    this.comment = comment;
    validateIndex(indexI5, indexI7);
  }

  private void validateIndex(String indexI5, String indexI7) {
    requireNonNull(indexI7, "indexI7 must not be null");
    if (indexI7.isBlank()) {
      throw new IllegalArgumentException("At least index i7 must be provided.");
    }
  }

  public SampleId getSampleId() {
    return sampleId;
  }

  public String getIndexI5() {
    return indexI5;
  }

  public String getIndexI7() {
    return indexI7;
  }

  public String getComment() {
    return comment;
  }
}
