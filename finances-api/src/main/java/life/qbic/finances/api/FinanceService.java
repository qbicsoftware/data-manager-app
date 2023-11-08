package life.qbic.finances.api;

import java.util.Collection;
import java.util.Optional;

/**
 * <b>Finance service</b>
 * <p>
 * Interface to enable clients to look up basic offer information.
 *
 * @since 1.0.0
 */
public interface FinanceService {

  /**
   * Lists concise offer information for offers that contain a given character sequence in the
   * project title or in the offerId.
   * <p>
   * The search is inclusive, so either a match in the title or the project id will be returned.
   *
   * @param projectTitle a character sequence to search for in the project title of an offer
   * @param offerId      a character sequence to search for in the offer id of an offer
   * @return list of {@link OfferSummary} matching the criteria
   * @since 1.0.0
   */
  Collection<OfferSummary> findOfferContainingProjectTitleOrId(String projectTitle,
      String offerId);

  /**
   * Same as {@link FinanceService#findOfferContainingProjectTitleOrId(String, String)} but with a
   * possibility for pagination by providing an offset and query result size limit.
   *
   * @param projectTitle a character sequence to search for in the project title of an offer
   * @param offerId      a character sequence to search for in the offer id of an offer
   * @param offset       the offset to start listing the matching search results
   * @param limit        the maximum number of matching search results
   * @return list of {@link OfferSummary} matching the criteria
   * @since 1.0.0
   */
  Collection<OfferSummary> findOfferContainingProjectTitleOrId(String projectTitle, String offerId,
      int offset, int limit);


  /**
   * Searches for an offer based on a given offer id
   *
   * @param offerId the offer id
   * @return an optional offer, is {@link Optional#empty()} if no matching offer was found
   * @since 1.0.0
   */
  Optional<Offer> findOfferById(String offerId);

}
