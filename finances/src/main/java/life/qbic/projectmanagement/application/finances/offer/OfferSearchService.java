package life.qbic.projectmanagement.application.finances.offer;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;

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

  /**
   * Lists all existing offers that contain a given character sequence in the project title or offer
   * id. The query is also restricted by an offset and limit, therefore it can be used for
   * pagination of query results.
   * <p>
   * Note: the search implementation ignores the case.
   *
   * @param projectTitle the character sequence that need to be contained in the project title of an
   *                     offer
   * @param offerId      the character sequence that need to be contained in the offer id of an
   *                     offer
   * @param offset       the offset index to start to show query results
   * @param limit        the overall size of the query results to show after the offset
   * @return a list of matching {@link OfferPreview}
   * @since 1.0.0
   */
  List<OfferPreview> findByProjectTitleOrOfferId(String projectTitle, String offerId, int offset,
      int limit);

  /**
   * Searches for an offer based on the provided identifier.
   *
   * @param offerId the offer identifier to use for the offer search.
   * @return the offer when found, otherwise {@link Optional#empty()}
   * @since 1.0.0
   */
  Optional<Offer> findByOfferId(String offerId);

}
