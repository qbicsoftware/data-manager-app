package life.qbic.projectmanagement.domain.model.project.purchase;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import static jakarta.persistence.FetchType.LAZY;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.util.Arrays;
import java.util.Objects;

/**
 * <b>Offer</b>
 *
 * An offer in the context of project management, not finance and accounting.
 *
 * @since 1.0.0
 */
@Entity(name = "purchase_offer")
public class Offer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private boolean signed;

  private String fileName;

  @Lob
  @Column(name = "file_content", columnDefinition = "LONGBLOB")
  @Basic(fetch=LAZY)
  private byte[] fileContent;

  public Offer() {

  }

  public static Offer create(boolean signed, String fileName,
      byte[] fileContent) {
    return new Offer(signed, fileName, fileContent);
  }

  protected Offer(boolean signed, String fileName, byte[] fileContent) {
    this.signed = signed;
    this.fileName = fileName;
    this.fileContent = fileContent;
  }

  public boolean isSigned() {
    return signed;
  }

  public String getFileName() {
    return fileName;
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
    Offer offer = (Offer) o;
    return signed == offer.signed && Objects.equals(id, offer.id)
        && Objects.equals(fileName, offer.fileName) && Arrays.equals(fileContent,
        offer.fileContent);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(id, signed, fileName);
    result = 31 * result + Arrays.hashCode(fileContent);
    return result;
  }
}
