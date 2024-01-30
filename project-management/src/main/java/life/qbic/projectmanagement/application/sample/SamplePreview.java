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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.experiment.BiologicalReplicate;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.repository.jpa.OntologyClassAttributeConverter;
import life.qbic.projectmanagement.domain.model.experiment.repository.jpa.SamplePropertyAttributeConverter;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.OntologyClassDTO;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
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

  @Column(name = "organism_id")
  private String organismId;
  private String comment;
  @Column(name = "analysis_method")
  private String analysisMethod;
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "experimentalGroupId")
  private ExperimentalGroup experimentalGroup;
  private OntologyClassDTO species;
  private OntologyClassDTO specimen;
  private OntologyClassDTO analyte;
  @Convert(converter = SamplePropertyAttributeConverter.class)
  private HashMap<String, String> properties;

  protected SamplePreview() {
    //needed by JPA
  }

  private SamplePreview(ExperimentId experimentId, SampleId sampleId, String sampleCode,
      String batchLabel, String bioReplicateLabel, String sampleLabel, String organismId,
      ExperimentalGroup experimentalGroup, OntologyClassDTO species,
      OntologyClassDTO specimen, OntologyClassDTO analyte, String analysisMethod, String comment) {
    Objects.requireNonNull(experimentId);
    Objects.requireNonNull(sampleId);
    Objects.requireNonNull(sampleCode);
    Objects.requireNonNull(batchLabel);
    Objects.requireNonNull(bioReplicateLabel);
    Objects.requireNonNull(sampleLabel);
    Objects.requireNonNull(experimentalGroup);
    Objects.requireNonNull(species);
    Objects.requireNonNull(specimen);
    Objects.requireNonNull(analyte);
    Objects.requireNonNull(analysisMethod);
    this.experimentId = experimentId;
    this.sampleId = sampleId;
    this.sampleCode = sampleCode;
    this.batchLabel = batchLabel;
    this.bioReplicateLabel = bioReplicateLabel;
    this.sampleLabel = sampleLabel;
    this.experimentalGroup = experimentalGroup;
    this.species = species;
    this.specimen = specimen;
    this.analyte = analyte;
    this.analysisMethod = analysisMethod;
    // optional columns
    this.comment = comment;
    this.organismId = organismId;
  }

  /**
   * Creates a new instance of a SamplePreview object.
   *
   * @param experimentId      the {@link ExperimentId} of the associated experiment
   * @param sampleId          the {@link SampleId} from which this preview was created
   * @param sampleCode        the {@link SampleCode} associated with this SamplePreview
   * @param batchLabel        the label of the {@link Batch} which contains the {@link Sample}
   *                          associated with this preview
   * @param bioReplicateLabel the label of the {@link BiologicalReplicate} for the {@link Sample}
   *                          associated with this preview
   * @param sampleLabel       the label of the {@link Sample} associated with this preview
   * @param organismId        optional identifier of the patient or organism a {@link Sample} was taken of
   * @param experimentalGroup the {@link ExperimentalGroup} for the {@link Sample} associated with
   *                          this preview
   * @param species           the {@link OntologyClassDTO} for the species of this {@link Sample}
   *                          associated with this preview
   * @param specimen          the {@link OntologyClassDTO} for the specimen of this {@link Sample}
   *                          associated with this preview
   * @param analyte           the {@link OntologyClassDTO} for the analyte of this {@link Sample}
   *                          associated with this preview
   * @param analysisMethod    the analysis method to be performed for this {@link Sample}
   * @param comment           an optional comment pertaining to the associated {@link Sample}
   * @return the sample preview
   */
  public static SamplePreview create(ExperimentId experimentId, SampleId sampleId,
      String sampleCode,
      String batchLabel, String bioReplicateLabel,
      String sampleLabel, String organismId, ExperimentalGroup experimentalGroup,
      OntologyClassDTO species, OntologyClassDTO specimen, OntologyClassDTO analyte,
      String analysisMethod, String comment) {
    return new SamplePreview(experimentId, sampleId, sampleCode, batchLabel, bioReplicateLabel,
        sampleLabel, organismId, experimentalGroup, species, specimen, analyte, analysisMethod,
        comment);
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

  public OntologyClassDTO species() {
    return species;
  }

  public OntologyClassDTO specimen() {
    return specimen;
  }

  public OntologyClassDTO analyte() {
    return analyte;
  }

  public String analysisMethod() {
    return analysisMethod;
  }

  public String comment() {
    return comment;
  }

  public String organismId() {
    return organismId;
  }

  public ExperimentalGroup experimentalGroup() {
    return experimentalGroup;
  }

  public HashMap<String, String> sampleProperties() {
    return properties;
  }

  public void addSampleProperties(Map<String, String> sampleProperties) {
    if (sampleProperties.isEmpty()) {
      return;
    }
    this.properties.putAll(sampleProperties);
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
        that.sampleLabel) && Objects.equals(organismId, that.organismId)
        && Objects.equals(species, that.species) && Objects.equals(specimen,
        that.specimen) && Objects.equals(analyte, that.analyte) && Objects.equals(
        experimentalGroup, that.experimentalGroup) && Objects.equals(analysisMethod,
        that.analysisMethod) && Objects.equals(comment, that.comment) && Objects.equals(properties,
        that.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(experimentId, sampleCode, sampleId, batchLabel, bioReplicateLabel,
        sampleLabel, organismId,
        species, specimen, analyte, experimentalGroup, analysisMethod, comment, properties);
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
        ", organismId='" + organismId + '\'' +
        ", species='" + species + '\'' +
        ", specimen='" + specimen + '\'' +
        ", analyte='" + analyte + '\'' +
        ", analysisMethod='" + analysisMethod + '\'' +
        ", comment='" + comment + '\'' +
        ", conditions=" + experimentalGroup + '\'' +
        ", properties=" + properties +
        '}';
  }
}
