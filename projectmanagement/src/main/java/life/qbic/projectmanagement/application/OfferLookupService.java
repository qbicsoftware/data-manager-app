package life.qbic.projectmanagement.application;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.domain.offer.OfferId;
import life.qbic.projectmanagement.domain.project.ProjectTitle;
import life.qbic.projectmanagement.finances.offer.OfferPreview;
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

  private final life.qbic.projectmanagement.finances.offer.api.OfferSearchService offerSearchService;

  @Autowired
  public OfferLookupService(
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
   * @return {@link OfferPreviewSearchResult} matching the criteria
   * @since 1.0.0
   */
  public List<OfferPreviewSearchResult> findOfferContainingProjectTitleOrId(String projectTitle, String offerId) {
    var searchResults = offerSearchService.findByProjectTitleOrOfferId(projectTitle, offerId);
    return searchResults.stream().map(
        offer -> new OfferPreviewSearchResult(new OfferId(offer.offerId().id()),
            new ProjectTitle(offer.getProjectTitle().title()))).collect(Collectors.toList());
  }

  public Optional<OfferPreview> findOfferById(OfferId offerId) {
    Optional<OfferPreview> test;
    return Optional.empty();
  }




}
