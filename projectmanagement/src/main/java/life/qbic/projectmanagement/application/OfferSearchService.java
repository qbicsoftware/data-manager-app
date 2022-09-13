package life.qbic.projectmanagement.application;

import java.util.List;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.domain.offer.OfferId;
import life.qbic.projectmanagement.domain.project.ProjectTitle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Offer Search Service</b>
 *
 * Enables search queries for offers.
 *
 * @since 1.0.0
 */
@Service
public class OfferSearchService {

  private final life.qbic.projectmanagement.finances.offer.api.OfferSearchService offerSearchService;

  @Autowired
  public OfferSearchService(
      life.qbic.projectmanagement.finances.offer.api.OfferSearchService offerSearchService) {
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
   * @return {@link OfferSearchResult} matching the criteria
   * @since 1.0.0
   */
  List<OfferSearchResult> findOfferContainingProjectTitleOrId(String projectTitle, String offerId) {
    var searchResults = offerSearchService.findByProjectTitleOrOfferId(projectTitle, offerId);
    return searchResults.stream().map(
        offer -> new OfferSearchResult(new OfferId(offer.offerId().id()),
            new ProjectTitle(offer.getProjectTitle().title()))).collect(Collectors.toList());
  }

}
