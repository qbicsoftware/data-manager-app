package life.qbic.projectmanagement.domain.model.project.purchase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

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
  @Column(name = "file_content", columnDefinition="BLOB")
  private byte[] fileContent;

  public Offer() {

  }

  public static Offer create(boolean signed, String fileName,
      byte[] fileContent) {
    var randomReferenceId = new Random().nextLong();
    return new Offer(randomReferenceId, signed, fileName, fileContent);
  }

  protected Offer(Long referenceId, boolean signed, String fileName,
      byte[] fileContent) {
    this.signed = signed;
    this.fileName = fileName;
    this.fileContent = fileContent;
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
