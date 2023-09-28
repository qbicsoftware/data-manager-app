package life.qbic.datamanager.views.general.funding;

import java.util.Objects;

/**
 * <b><record short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class FundingEntry {

  private String label;

  private String referenceId;

  public FundingEntry(String label, String referenceId) {
    this.label = label;
    this.referenceId = referenceId;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FundingEntry that = (FundingEntry) o;
    return Objects.equals(label, that.label) && Objects.equals(referenceId,
        that.referenceId);
  }

  public boolean isEmpty(){
    return label.isBlank() && referenceId.isBlank();
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, referenceId);
  }

  @Override
  public String toString() {
    return "FundingEntry{" +
        "label='" + label + '\'' +
        ", referenceId='" + referenceId + '\'' +
        '}';
  }
}
