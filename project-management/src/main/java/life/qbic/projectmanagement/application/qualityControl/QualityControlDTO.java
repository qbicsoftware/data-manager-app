package life.qbic.projectmanagement.application.qualityControl;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;

/**
 * <b>Simple quality control information exchange object</b>
 */
public record QualityControlDTO(String fileName, ExperimentId experimentId, byte[] content) {

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QualityControlDTO qualityControlDTO = (QualityControlDTO) o;
    return Objects.equals(fileName, qualityControlDTO.fileName) && Objects.equals(experimentId,
        qualityControlDTO.experimentId)
        && Arrays.equals(content, qualityControlDTO.content);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(experimentId, fileName);
    result = 31 * result + Arrays.hashCode(content);
    return result;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", QualityControlDTO.class.getSimpleName() + "[", "]")
        .add("fileName='" + fileName + "'")
        .add("experimentId='" + experimentId + "'")
        .add("content=" + Arrays.toString(content))
        .toString();
  }
}
