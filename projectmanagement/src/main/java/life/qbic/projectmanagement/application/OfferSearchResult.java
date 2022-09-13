package life.qbic.projectmanagement.application;

import static java.util.Objects.*;

import java.util.Objects;
import life.qbic.projectmanagement.domain.project.ProjectTitle;
import life.qbic.projectmanagement.domain.offer.OfferId;

/**
 * <b>Offer Search Result</b>
 * <p>
 * Concise offer search result overview
 *
 * @since 1.0.0
 */
public record OfferSearchResult(OfferId offerId, ProjectTitle title) {

  public OfferSearchResult {
    requireNonNull(offerId);
    requireNonNull(title);
  }

}
