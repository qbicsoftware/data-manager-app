package life.qbic.projectmanagement.application.purchase;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * <b>Simple offer information exchange object</b>
 *
 * @since 1.0.0s
 */
public record OfferDTO(boolean signed, String fileName, byte[] content) {

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OfferDTO offerDTO = (OfferDTO) o;
    return signed == offerDTO.signed && Objects.equals(fileName, offerDTO.fileName)
        && Arrays.equals(content, offerDTO.content);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(signed, fileName);
    result = 31 * result + Arrays.hashCode(content);
    return result;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", OfferDTO.class.getSimpleName() + "[", "]")
        .add("signed=" + signed)
        .add("fileName='" + fileName + "'")
        .add("content=" + Arrays.toString(content))
        .toString();
  }
}
