package life.qbic.projectmanagement.domain.model.measurement;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b>Proteomics Specific Measurement Metadata</b>
 * <p>
 * A {@link ProteomicsMeasurement} can contain multiple specific properties, that are associated
 * with the sample that has been measured.
 *
 * @since 1.0.0
 */
@Embeddable
public class ProteomicsSpecificMeasurementMetadata {

  @Column(name = "sampleId")
  private SampleId measuredSample;

  @Column(name = "labelingType")
  private String labelingType;

  @Column(name = "label")
  private String label;

  @Column(name = "fractionName")
  private String fractionName;

  @Column(name = "comment")
  private String comment;

  protected ProteomicsSpecificMeasurementMetadata() {
  }

  private ProteomicsSpecificMeasurementMetadata(SampleId measuredSample, String labelingType,
      String label, String fractionName, String comment) {
    this.measuredSample = measuredSample;
    this.labelingType = labelingType;
    this.label = label;
    this.fractionName = fractionName;
    this.comment = comment;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProteomicsSpecificMeasurementMetadata that = (ProteomicsSpecificMeasurementMetadata) o;
    return Objects.equals(measuredSample,
        that.measuredSample) && Objects.equals(labelingType, that.labelingType)
        && Objects.equals(label, that.label) && Objects.equals(fractionName,
        that.fractionName) && Objects.equals(comment, that.comment);
  }

  public static ProteomicsSpecificMeasurementMetadata create(SampleId measuredSample, String labelingType,
      String label, String fractionName, String comment) {
    if (measuredSample == null) {
      throw new IllegalArgumentException("SampleId was null");
    }
    return new ProteomicsSpecificMeasurementMetadata(measuredSample, labelingType, label,
        fractionName, comment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(measuredSample, labelingType, label, fractionName, comment);
  }

  public SampleId measuredSample() {
    return measuredSample;
  }

}
