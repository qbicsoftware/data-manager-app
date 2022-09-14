package life.qbic.projectmanagement.application.finances.offer;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.application.finances.offer.OfferSearchService;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.finances.offer.OfferId;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>OfferPreview Search Service</b>
 *
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
  public List<OfferPreview> findOfferContainingProjectTitleOrId(String projectTitle, String offerId) {
    return offerSearchService.findByProjectTitleOrOfferId(projectTitle, offerId);
  }

  /**
   * Searches for an offer based on a given offer id
   * @param offerId the offer id
   * @return
   * @since
   */
  public Optional<Offer> findOfferById(OfferId offerId) {
    return offerSearchService.findByOfferId(offerId.id());
  }




}
