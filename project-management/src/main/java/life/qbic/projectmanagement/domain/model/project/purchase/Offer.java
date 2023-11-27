package life.qbic.projectmanagement.domain.model.project.purchase;

import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import life.qbic.projectmanagement.domain.model.project.translation.CharsetConverter;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Entity(name = "purchase_offer")
public class Offer {

  @Id
  @GeneratedValue()
  private Long id;

  private Long referenceId;

  private String offerId;

  private boolean signed;

  private String fileName;

  private Byte[] fileContent;

  @Convert(converter = CharsetConverter.class)
  private Charset charset;

  public Offer() {

  }

  public static Offer create(String offerId, boolean signed, String fileName,
      Byte[] fileContent,
      Charset charset) {
    var randomReferenceId = new Random().nextLong();
    return new Offer(randomReferenceId, offerId, signed, fileName, fileContent, charset);
  }

  protected Offer(Long referenceId, String offerId, boolean signed, String fileName,
      Byte[] fileContent,
      Charset charset) {
    this.referenceId = referenceId;
    this.offerId = offerId;
    this.signed = signed;
    this.fileName = fileName;
    this.fileContent = fileContent;
    this.charset = charset;
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
        && Objects.equals(referenceId, offer.referenceId) && Objects.equals(
        offerId, offer.offerId) && Objects.equals(fileName, offer.fileName)
        && Arrays.equals(fileContent, offer.fileContent) && Objects.equals(
        charset, offer.charset);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(id, referenceId, offerId, signed, fileName, charset);
    result = 31 * result + Arrays.hashCode(fileContent);
    return result;
  }
}
