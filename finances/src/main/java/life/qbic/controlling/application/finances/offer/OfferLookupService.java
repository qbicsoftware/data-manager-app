package life.qbic.controlling.application.finances.offer;

import java.util.List;
import java.util.Optional;
import life.qbic.controlling.domain.finances.offer.Offer;
import life.qbic.controlling.domain.finances.offer.OfferId;
import life.qbic.controlling.domain.finances.offer.OfferPreview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>OfferPreview Search Service</b>
 * <p>
 * Enables search queries for offers.
 *
 * @since 1.0.0
 */
@Service
public class OfferLookupService {

  private final OfferSearchService offerSearchService;

  @Autowired
  public OfferLookupService(
      OfferSearchService offerSearchService) {
    this.offerSearchService = offerSearchService;
  }

  /**
   * Lists concise offer information for offers that contain a given character sequence in the
   * project title or in the offerId.
   * <p>
   * The search is inclusive, so either a match in the title or the project id will be returned.
   *
   * @param projectTitle a character sequence to search for in the project title of an offer
   * @param offerId      a character sequence to search for in the offer id of an offer
   * @return list of {@link OfferPreview} matching the criteria
   * @since 1.0.0
   */
  public List<OfferPreview> findOfferContainingProjectTitleOrId(String projectTitle,
      String offerId) {
    return offerSearchService.findByProjectTitleOrOfferId(projectTitle, offerId);
  }

  /**
   * Same as {@link OfferSearchService#findByProjectTitleOrOfferId(String, String)} but with a
   * possibility for pagination by providing an offset and query result size limit.
   *
   * @param projectTitle a character sequence to search for in the project title of an offer
   * @param offerId      a character sequence to search for in the offer id of an offer
   * @param offset       the offset to start listing the matching search results
   * @param limit        the maximum number of matching search results
   * @return list of {@link OfferPreview} matching the criteria
   * @since 1.0.0
   */
  public List<OfferPreview> findOfferContainingProjectTitleOrId(String projectTitle, String offerId,
      int offset, int limit) {
    return offerSearchService.findByProjectTitleOrOfferId(projectTitle, offerId, offset, limit);
  }

  /**
   * Searches for an offer based on a given offer id
   *
   * @param offerId the offer id
   * @return an optional offer, is {@link Optional#empty()} if no matching offer was found
   * @since 1.0.0
   */
  public Optional<Offer> findOfferById(OfferId offerId) {
    return offerSearchService.findByOfferId(offerId.id());
  }


}
