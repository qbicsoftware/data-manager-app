package life.qbic.projectmanagement.domain.model.project;

import java.util.Objects;

/**
 * The identifier of an offer
 */
public record OfferIdentifier(String value) {

  public OfferIdentifier {
    if (Objects.isNull(value)) {
      throw new IllegalArgumentException("No value provided. Input is null");
    }
    Objects.requireNonNull(value);
    if (value.isBlank()) {
      throw new IllegalArgumentException("Please provide non-blank input.");
    }
  }

  public static OfferIdentifier of(String value) {
    return new OfferIdentifier(value);
  }
}
