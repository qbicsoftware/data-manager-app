package life.qbic.projectmanagement.domain.model.measurement;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Embeddable
public class NGSSpecificMeasurementMetadata {

  @Column(name = "sampleId")
  private SampleId measuredSample;

  @Column(name = "indexI5")
  private String indexI5;

  @Column(name = "indexI7")
  private String indexI7;

  @Column(name = "comment")
  private String comment;

  protected NGSSpecificMeasurementMetadata() {
  }

  private NGSSpecificMeasurementMetadata(SampleId measuredSample, String indexI5, String indexI7,
      String comment) {
    this.measuredSample = measuredSample;
    this.indexI5 = indexI5;
    this.indexI7 = indexI7;
    this.comment = comment;
  }

  public static NGSSpecificMeasurementMetadata create(SampleId sampleId, String indexI5,
      String indexI7, String comment) {
    Objects.requireNonNull(sampleId);
    Objects.requireNonNull(indexI5);
    Objects.requireNonNull(indexI7);
    Objects.requireNonNull(comment);
    // According to the NGS experts, there can be
    // - Double indexing i5 +i7
    // - Single indexing i7
    // - no indexing (if no multiplexing is done)
    if (!indexI5.isBlank() && !indexI7.isBlank()) {
      // Double indexing
      return new NGSSpecificMeasurementMetadata(sampleId, indexI5, indexI7, comment);
    }
    if (!indexI7.isBlank()) {
      // Single indexing
      return new NGSSpecificMeasurementMetadata(sampleId, indexI5, indexI7, comment);
    }
    if (!indexI5.isBlank()) {
      throw new IllegalArgumentException("Index i5 cannot be set as single index");
    }
    // No indexing provided
    return new NGSSpecificMeasurementMetadata(sampleId, indexI5, indexI7, comment);
  }

  public SampleId measuredSample() {
    return measuredSample;
  }

  public Optional<String> comment() {
    return comment.isBlank() ? Optional.empty() : Optional.of(comment);
  }

  public Optional<NGSIndex> index() {
    if (indexI7.isBlank()) {
      return Optional.empty();
    }
    if (indexI5.isBlank()) {
      return Optional.of(NGSIndex.singleIndexing(indexI7));
    }
    return Optional.of(new NGSIndex(indexI5, indexI7));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NGSSpecificMeasurementMetadata that = (NGSSpecificMeasurementMetadata) o;
    return Objects.equals(measuredSample, that.measuredSample) && Objects.equals(
        indexI5, that.indexI5) && Objects.equals(indexI7, that.indexI7)
        && Objects.equals(comment, that.comment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(measuredSample, indexI5, indexI7, comment);
  }
}
