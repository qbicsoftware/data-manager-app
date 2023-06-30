package life.qbic.projectmanagement.application.sample;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleId;
import org.hibernate.annotations.Formula;

/**
 * An amalgamation of information stored in {@link Experiment} and {@link Sample}
 */
@Entity()
@Table(name = "sample")
public class SamplePreview {
  @AttributeOverride(name = "uuid", column = @Column(name = "experiment_id"))
  @Embedded
  private ExperimentId experimentId;
  @Column(name = "code")
  private String sampleCode;
  @EmbeddedId
  @Column(name = "sample_id")
  private SampleId sampleId;
  @Formula("(select sample_batches.batchLabel from sample_batches where sample_batches.id = assigned_batch_id)")
  private String batchLabel;
  @Formula("(select bio_replicate.label from bio_replicate where bio_replicate.id = bio_replicate_id)")
  private String bioReplicateLabel;
  @Column(name = "label")
  private String sampleLabel;
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "experimentalGroupId")
  private ExperimentalGroup experimentalGroup;
  private String species;
  private String specimen;
  private String analyte;

  protected SamplePreview() {
    //needed by JPA
  }

  private SamplePreview(ExperimentId experimentId, SampleId sampleId, String sampleCode,
      String batchLabel, String bio_replicate_id,
      String sampleLabel, ExperimentalGroup experimentalGroup, String species, String specimen,
      String analyte) {
    Objects.requireNonNull(experimentId);
    Objects.requireNonNull(sampleId);
    Objects.requireNonNull(sampleCode);
    Objects.requireNonNull(batchLabel);
    Objects.requireNonNull(bio_replicate_id);
    Objects.requireNonNull(sampleLabel);
    Objects.requireNonNull(experimentalGroup);
    Objects.requireNonNull(species);
    Objects.requireNonNull(specimen);
    Objects.requireNonNull(analyte);
    this.experimentId = experimentId;
    this.sampleId = sampleId;
    this.sampleCode = sampleCode;
    this.batchLabel = batchLabel;
    this.bioReplicateLabel = bio_replicate_id;
    this.sampleLabel = sampleLabel;
    this.experimentalGroup = experimentalGroup;
    this.species = species;
    this.specimen = specimen;
    this.analyte = analyte;
  }

  public ExperimentId experimentId() {
    return experimentId;
  }

  public String sampleCode() {
    return sampleCode;
  }

  public SampleId sampleId() {
    return sampleId;
  }

  public String batchLabel() {
    return batchLabel;
  }

  public String replicateLabel() {
    return bioReplicateLabel;
  }

  public String sampleLabel() {
    return sampleLabel;
  }

  public String species() {
    return species;
  }

  public String specimen() {
    return specimen;
  }

  public String analyte() {
    return analyte;
  }

  public ExperimentalGroup experimentalGroup() {
    return experimentalGroup;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SamplePreview that = (SamplePreview) o;
    return Objects.equals(experimentId, that.experimentId) && Objects.equals(
        sampleCode, that.sampleCode) && Objects.equals(sampleId, that.sampleId)
        && Objects.equals(batchLabel, that.batchLabel) && Objects.equals(
        bioReplicateLabel, that.bioReplicateLabel) && Objects.equals(sampleLabel,
        that.sampleLabel)
        && Objects.equals(species, that.species) && Objects.equals(specimen,
        that.specimen) && Objects.equals(analyte, that.analyte) && Objects.equals(
        experimentalGroup, that.experimentalGroup);
  }

  @Override
  public int hashCode() {
    return Objects.hash(experimentId, sampleCode, sampleId, batchLabel, bioReplicateLabel,
        sampleLabel,
        species, specimen, analyte, experimentalGroup);
  }

  @Override
  public String toString() {
    return "SamplePreview{" +
        "experimentId=" + experimentId +
        ", sampleCode='" + sampleCode + '\'' +
        ", sampleId='" + sampleId + '\'' +
        ", batchLabel='" + batchLabel + '\'' +
        ", sampleSource='" + bioReplicateLabel + '\'' +
        ", sampleLabel='" + sampleLabel + '\'' +
        ", species='" + species + '\'' +
        ", specimen='" + specimen + '\'' +
        ", analyte='" + analyte + '\'' +
        ", conditions=" + experimentalGroup +
        '}';
  }
}
