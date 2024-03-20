package life.qbic.projectmanagement.domain.model.rawdata;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import java.time.Instant;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;

/**
 * <b>Raw Data</b>
 * <p>
 * Raw data representation employed in the data manager
 * application generated from the associated measurementId
 */
@Entity(name = "raw_data")
public class RawData {

  @EmbeddedId
  @AttributeOverride(name = "uuid", column = @Column(name = "raw_data_id"))
  private RawDataId rawDataId;
  @Column(name = "measurement_id")
  private MeasurementId measurementId;
  @Column(name = "registration")
  private Instant registration;

  protected RawData() {
    // Needed for JPA
  }

  //Todo handle sample label and sample code

  private RawData(RawDataId rawDataId, MeasurementId measurementId, Instant registration) {
    this.rawDataId = rawDataId;
    this.measurementId = measurementId;
    this.registration = registration;
  }

  /**
   * Creates a new {@link RawData} object instance, that describes an NGS measurement
   * entity with many describing properties about provenance and instrumentation.
   *
   */
  public static RawData create(MeasurementId measurementId) {
    Objects.requireNonNull(measurementId);
    var rawDataId = RawDataId.create();
    return new RawData(rawDataId, measurementId, Instant.now());
  }

  public RawDataId id() {
    return rawDataId;
  }

  public MeasurementId measurementId() {
    return measurementId;
  }

  public Instant registrationDate() {
    return registration;
  }
}
