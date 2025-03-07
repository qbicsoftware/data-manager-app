package life.qbic.projectmanagement.domain.model.sample.qualitycontrol;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import static jakarta.persistence.FetchType.LAZY;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;

/**
 * <b>Quality Control Upload</b>
 * <p>
 * The quality control upload entity handles a {@link QualityControl}, with which an experiment was
 * associated during the provision step by the user in the context of project management
 */

@Entity(name = "quality_control_upload")
public class QualityControlUpload {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @AttributeOverride(name = "uuid", column = @Column(name = "experiment_id"))
  @Embedded
  private ExperimentId experimentId;
  private String fileName;
  @Lob
  @Column(name = "file_content", columnDefinition = "LONGBLOB")
  @Basic(fetch = LAZY)
  private byte[] fileContent;

  public QualityControlUpload() {

  }

  public static QualityControlUpload create(String fileName, ExperimentId experimentId,
      byte[] fileContent) {
    return new QualityControlUpload(fileName, experimentId, fileContent);
  }

  protected QualityControlUpload(String fileName, ExperimentId experimentId, byte[] fileContent) {
    this.fileName = fileName;
    this.experimentId = experimentId;
    this.fileContent = fileContent;
  }

  public String getFileName() {
    return fileName;
  }

  public Optional<ExperimentId> experimentId() {
    return Optional.ofNullable(experimentId);
  }

  public byte[] fileContent() {
    return Arrays.copyOf(fileContent, fileContent.length);
  }

  public Long id() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QualityControlUpload qualityControlUpload = (QualityControlUpload) o;
    return Objects.equals(id, qualityControlUpload.id)
        && Objects.equals(fileName, qualityControlUpload.fileName) && Objects.equals(experimentId,
        qualityControlUpload.experimentId) && Arrays.equals(fileContent,
        qualityControlUpload.fileContent);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(id, experimentId, fileName);
    result = 31 * result + Arrays.hashCode(fileContent);
    return result;
  }
}
