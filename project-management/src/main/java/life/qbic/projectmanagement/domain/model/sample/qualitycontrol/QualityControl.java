package life.qbic.projectmanagement.domain.model.sample.qualitycontrol;

import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.util.Arrays;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;

/**
 * <b>Quality Control</b>
 * <p>
 * A quality control associated with an experiment in the context of project management
 */

public class QualityControl {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private ExperimentId experimentId;
  private String fileName;
  @Lob
  @Column(name = "file_content", columnDefinition = "BLOB")
  @Basic(fetch = LAZY)
  private byte[] fileContent;

  public QualityControl() {

  }

  public static QualityControl create(String fileName, ExperimentId experimentId,
      byte[] fileContent) {
    return new QualityControl(fileName, experimentId, fileContent);
  }

  protected QualityControl(String fileName, ExperimentId experimentId, byte[] fileContent) {
    this.fileName = fileName;
    this.experimentId = experimentId;
    this.fileContent = fileContent;
  }

  public String getFileName() {
    return fileName;
  }

  public ExperimentId experimentId() {
    return experimentId;
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
    QualityControl qualityControl = (QualityControl) o;
    return Objects.equals(id, qualityControl.id)
        && Objects.equals(fileName, qualityControl.fileName) && Objects.equals(experimentId,
        qualityControl.experimentId) && Arrays.equals(fileContent,
        qualityControl.fileContent);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(id, experimentId, fileName);
    result = 31 * result + Arrays.hashCode(fileContent);
    return result;
  }
}
