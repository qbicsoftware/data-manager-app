package life.qbic.projectmanagement.finances.offer.api;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.finances.offer.Offer;
import life.qbic.projectmanagement.finances.offer.OfferPreview;

/**
 * <b>OfferPreview Search Service</b>
 * <p>
 * Service that enables queries for offer information.
 *
 * @since 1.0.0
 */
public interface OfferSearchService {

  /**
   * Lists all existing offers that contain a given character sequence in the project title or offer
   * id.
   * <p>
   * Note: the search implementation ignores the case.
   *
   * @param projectTitle the character sequence that need to be contained in the project title of an
   *                     offer
   * @param offerId      the character sequence that need to be contained in the offer id of an
   *                     offer
   * @return a list of {@link OfferPreview} matching the search criteria
   * @since 1.0.0
   */
  List<OfferPreview> findByProjectTitleOrOfferId(String projectTitle, String offerId);

  Optional<Offer> findByOfferId(String offerId);

}
