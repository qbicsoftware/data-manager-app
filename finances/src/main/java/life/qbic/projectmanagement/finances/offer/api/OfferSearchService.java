package life.qbic.projectmanagement.finances.offer.api;

import java.util.List;
import life.qbic.projectmanagement.finances.offer.Offer;
import life.qbic.projectmanagement.finances.offer.OfferId;
import life.qbic.projectmanagement.finances.offer.ProjectTitle;

/**
 * <b>Offer Search Service</b>
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
   * @return a list of {@link Offer} matching the search criteria
   * @since 1.0.0
   */
  List<Offer> findByProjectTitleOrOfferId(String projectTitle, String offerId);

}
