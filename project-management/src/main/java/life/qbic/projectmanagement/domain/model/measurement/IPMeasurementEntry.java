package life.qbic.projectmanagement.domain.model.measurement;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b>IP Measurement Entry</b>
 * <p>
 * An Immunopeptidomics measurement can contain multiple specific properties, that are
 * associated with the sample that has been measured. Mostly relevant if the measurement was
 * generated in a pooling context.
 */
@Embeddable
public class IPMeasurementEntry {

  @Column(name = "sampleId")
  private SampleId measuredSample;

  @Column(name = "sampleName")
  private String sampleName;

  @Column(name = "measurementName")
  private String measurementName;

  @Column(name = "sampleMass")
  private Double sampleMass;

  @Column(name = "sampleVolume")
  private Double sampleVolume;

  @Column(name = "cycleFractionName")
  private String cycleFractionName;

  @Column(name = "mhcAntibody")
  private String mhcAntibody;

  @Column(name = "mhcTypingMethod")
  private String mhcTypingMethod;

  @Column(name = "enrichmentMethod")
  private String enrichmentMethod;

  @Column(name = "prepDate")
  private LocalDate prepDate;

  @Column(name = "msRunDate")
  private LocalDate msRunDate;

  @Column(name = "lcmsMethod")
  private String lcmsMethod;

  @Column(name = "lcColumn")
  private String lcColumn;

  @Column(name = "dataAcquisition")
  private String dataAcquisition;

  @Column(name = "massRange")
  private String massRange;

  @Column(name = "retentionTimeRange")
  private Integer retentionTimeRange;

  @Column(name = "chargeRange")
  private String chargeRange;

  @Column(name = "ionMobilityRange")
  private String ionMobilityRange;

  @Column(name = "comment")
  private String comment;

  protected IPMeasurementEntry() {
  }

  private IPMeasurementEntry(SampleId measuredSample, String sampleName,
      String measurementName, Double sampleMass, Double sampleVolume,
      String cycleFractionName, String mhcAntibody, String mhcTypingMethod,
      String enrichmentMethod, LocalDate prepDate, LocalDate msRunDate,
      String lcmsMethod, String lcColumn, String dataAcquisition,
      String massRange, Integer retentionTimeRange, String chargeRange,
      String ionMobilityRange, String comment) {
    this.measuredSample = measuredSample;
    this.sampleName = sampleName;
    this.measurementName = measurementName;
    this.sampleMass = sampleMass;
    this.sampleVolume = sampleVolume;
    this.cycleFractionName = cycleFractionName;
    this.mhcAntibody = mhcAntibody;
    this.mhcTypingMethod = mhcTypingMethod;
    this.enrichmentMethod = enrichmentMethod;
    this.prepDate = prepDate;
    this.msRunDate = msRunDate;
    this.lcmsMethod = lcmsMethod;
    this.lcColumn = lcColumn;
    this.dataAcquisition = dataAcquisition;
    this.massRange = massRange;
    this.retentionTimeRange = retentionTimeRange;
    this.chargeRange = chargeRange;
    this.ionMobilityRange = ionMobilityRange;
    this.comment = comment;
  }

  public static IPMeasurementEntry create(SampleId sampleId, String sampleName,
      String measurementName, Double sampleMass, Double sampleVolume,
      String cycleFractionName, String mhcAntibody, String mhcTypingMethod,
      String enrichmentMethod, LocalDate prepDate, LocalDate msRunDate,
      String lcmsMethod, String lcColumn, String dataAcquisition,
      String massRange, Integer retentionTimeRange, String chargeRange,
      String ionMobilityRange, String comment) {
    Objects.requireNonNull(sampleId, "Sample Id cannot be null");
    return new IPMeasurementEntry(sampleId, sampleName, measurementName, sampleMass, sampleVolume,
        cycleFractionName, mhcAntibody, mhcTypingMethod, enrichmentMethod, prepDate, msRunDate,
        lcmsMethod, lcColumn, dataAcquisition, massRange, retentionTimeRange, chargeRange,
        ionMobilityRange, comment);
  }

  public SampleId measuredSample() {
    return measuredSample;
  }

  public Optional<String> sampleName() {
    return sampleName.isBlank() ? Optional.empty() : Optional.of(sampleName);
  }

  public Optional<String> measurementName() {
    return measurementName.isBlank() ? Optional.empty() : Optional.of(measurementName);
  }

  public Double sampleMass() {
    return sampleMass;
  }

  public Double sampleVolume() {
    return sampleVolume;
  }

  public Optional<String> cycleFractionName() {
    return cycleFractionName.isBlank() ? Optional.empty() : Optional.of(cycleFractionName);
  }

  public String mhcAntibody() {
    return mhcAntibody;
  }

  public Optional<String> mhcTypingMethod() {
    return mhcTypingMethod.isBlank() ? Optional.empty() : Optional.of(mhcTypingMethod);
  }

  public String enrichmentMethod() {
    return enrichmentMethod;
  }

  public Optional<LocalDate> prepDate() {
    return Optional.ofNullable(prepDate);
  }

  public Optional<LocalDate> msRunDate() {
    return Optional.ofNullable(msRunDate);
  }

  public String lcmsMethod() {
    return lcmsMethod;
  }

  public String lcColumn() {
    return lcColumn;
  }

  public String dataAcquisition() {
    return dataAcquisition;
  }

  public String massRange() {
    return massRange;
  }

  public Integer retentionTimeRange() {
    return retentionTimeRange;
  }

  public String chargeRange() {
    return chargeRange;
  }

  public Optional<String> ionMobilityRange() {
    return ionMobilityRange.isBlank() ? Optional.empty() : Optional.of(ionMobilityRange);
  }

  public Optional<String> comment() {
    return comment.isBlank() ? Optional.empty() : Optional.of(comment);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IPMeasurementEntry that = (IPMeasurementEntry) o;
    return Objects.equals(measuredSample, that.measuredSample)
        && Objects.equals(sampleName, that.sampleName)
        && Objects.equals(measurementName, that.measurementName)
        && Objects.equals(sampleMass, that.sampleMass)
        && Objects.equals(sampleVolume, that.sampleVolume)
        && Objects.equals(cycleFractionName, that.cycleFractionName)
        && Objects.equals(mhcAntibody, that.mhcAntibody)
        && Objects.equals(mhcTypingMethod, that.mhcTypingMethod)
        && Objects.equals(enrichmentMethod, that.enrichmentMethod)
        && Objects.equals(prepDate, that.prepDate)
        && Objects.equals(msRunDate, that.msRunDate)
        && Objects.equals(lcmsMethod, that.lcmsMethod)
        && Objects.equals(lcColumn, that.lcColumn)
        && Objects.equals(dataAcquisition, that.dataAcquisition)
        && Objects.equals(massRange, that.massRange)
        && Objects.equals(retentionTimeRange, that.retentionTimeRange)
        && Objects.equals(chargeRange, that.chargeRange)
        && Objects.equals(ionMobilityRange, that.ionMobilityRange)
        && Objects.equals(comment, that.comment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(measuredSample, sampleName, measurementName, sampleMass, sampleVolume,
        cycleFractionName, mhcAntibody, mhcTypingMethod, enrichmentMethod, prepDate, msRunDate,
        lcmsMethod, lcColumn, dataAcquisition, massRange, retentionTimeRange, chargeRange,
        ionMobilityRange, comment);
  }
}
