package life.qbic.projectmanagement.offer;

import static java.util.Objects.requireNonNull;

/**
 * The id of an offer.
 *
 * @since <version tag>
 */
public record OfferId(String value) {

  public OfferId {
    requireNonNull(value);
  }

}
