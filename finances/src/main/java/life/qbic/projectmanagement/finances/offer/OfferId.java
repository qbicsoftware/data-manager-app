package life.qbic.projectmanagement.finances.offer;

import java.util.Objects;

/**
 * <b>Offer Id</b>
 * <p>
 * Describes an identifier for an offer.
 *
 * @since 1.0.0
 */
public class OfferId {

  private String id;

  /**
   * Creates an instance of an {@link OfferId}
   *
   * @param id the value for the offer id
   * @return a new instance of an offer id
   * @since 1.0.0
   */
  public static OfferId of(String id) {
    return new OfferId(id);
  }

  private OfferId(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  private void setId(String id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OfferId offerId = (OfferId) o;
    return Objects.equals(id, offerId.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
