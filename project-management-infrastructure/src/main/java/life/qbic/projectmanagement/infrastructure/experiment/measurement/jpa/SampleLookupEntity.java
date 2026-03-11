package life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import life.qbic.projectmanagement.infrastructure.PreventAnyUpdateEntityListener;

/**
 * Read-only JPA entity providing stable sample metadata from the {@code sample} table.
 *
 * <p>This entity exists solely as a lookup target for measurement-sample associations
 * ({@link NgsMeasurementJpaRepository.NgsSampleInfo} and
 * {@link PxpMeasurementJpaRepository.PxpSampleInfo}). It avoids the need for a
 * {@code @SecondaryTable} join — which cannot reference a composite primary key — by instead
 * exposing sample metadata through a regular {@code @ManyToOne} relationship mapped via
 * {@code @MapsId}.
 *
 * <p>This entity is strictly read-only. Any attempted update will be intercepted by
 * {@link PreventAnyUpdateEntityListener}.
 */
@Entity
@Table(name = "sample")
@EntityListeners(PreventAnyUpdateEntityListener.class)
public final class SampleLookupEntity {

  /**
   * The unique identifier of this sample. Serves as the primary key in the {@code sample} table.
   */
  @Id
  @Column(name = "sample_id")
  private String sampleId;

  /**
   * The identifier of the experiment this sample belongs to.
   */
  @Column(name = "experiment_id")
  private String experimentId;

  /**
   * The human-readable code assigned to this sample (e.g. {@code QTEST001AE}).
   */
  @Column(name = "code")
  private String sampleCode;

  /**
   * The descriptive label assigned to this sample by the researcher.
   */
  @Column(name = "label")
  private String sampleLabel;

  /**
   * Required by JPA. Do not use directly.
   */
  protected SampleLookupEntity() {
  }

  /**
   * Returns the identifier of the experiment this sample belongs to.
   *
   * @return the experiment id; never null for a valid persisted entity
   */
  public String experimentId() {
    return experimentId;
  }

  /**
   * Returns the human-readable code of this sample.
   *
   * @return the sample code; never null for a valid persisted entity
   */
  public String sampleCode() {
    return sampleCode;
  }

  public String sampleLabel() {
    return sampleLabel;
  }

}
