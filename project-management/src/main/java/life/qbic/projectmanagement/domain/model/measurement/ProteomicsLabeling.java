package life.qbic.projectmanagement.domain.model.measurement;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * <b>Proteomics Labeling Method</b>
 *
 * <p>Describes a proteomics labeling method applied to prepare for a measurement</p>
 * <p>
 * Example: SILAC with N15 label.
 *
 * @since 1.0.0
 */
@Embeddable
public class ProteomicsLabeling {

  @Column(name="sampleCode")
  private String sampleCode;

  @Column(name="labelType")
  private String labelType;

  @Column(name="label")
  private String label;

  public ProteomicsLabeling(String sampleCode, String labelType, String label) {
    this.sampleCode = sampleCode;
    this.labelType = labelType;
    this.label = label;
  }

  protected ProteomicsLabeling() {

  }

  public String sampleCode() {
    return sampleCode;
  }

  public String labelType() {
    return labelType;
  }

  public String label() {
    return label;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProteomicsLabeling that = (ProteomicsLabeling) o;
    return Objects.equals(sampleCode, that.sampleCode) && Objects.equals(
        labelType, that.labelType) && Objects.equals(label, that.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sampleCode, labelType, label);
  }

  @Override
  public String toString() {
    return "ProteomicsLabeling{" +
        "sampleCode='" + sampleCode + '\'' +
        ", labelType='" + labelType + '\'' +
        ", label='" + label + '\'' +
        '}';
  }
}
