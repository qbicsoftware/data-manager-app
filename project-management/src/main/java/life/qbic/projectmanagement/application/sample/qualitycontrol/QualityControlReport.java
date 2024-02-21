package life.qbic.projectmanagement.application.sample.qualitycontrol;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;

/**
 * <b>Simple quality control information exchange object</b>
 */
public record QualityControlReport(String fileName, ExperimentId experimentId, byte[] content) {

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QualityControlReport qualityControlReport = (QualityControlReport) o;
    return Objects.equals(fileName, qualityControlReport.fileName) && Objects.equals(experimentId,
        qualityControlReport.experimentId)
        && Arrays.equals(content, qualityControlReport.content);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(experimentId, fileName);
    result = 31 * result + Arrays.hashCode(content);
    return result;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", QualityControlReport.class.getSimpleName() + "[", "]")
        .add("fileName='" + fileName + "'")
        .add("experimentId='" + experimentId + "'")
        .add("content=" + Arrays.toString(content))
        .toString();
  }
}
