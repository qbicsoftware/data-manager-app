package life.qbic.finance.application;

import java.util.List;
import java.util.Optional;
import life.qbic.finance.domain.model.OfferPreview;
import life.qbic.finances.api.FinanceService;
import life.qbic.finances.api.Offer;
import life.qbic.finances.api.OfferSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Finance Service</b>
 * <p>
 * Enables search queries for offers.
 *
 * @since 1.0.0
 */
@Service
public class FinanceServiceImpl implements FinanceService {

  private final OfferSearchService offerSearchService;

  @Autowired
  public FinanceServiceImpl(
      OfferSearchService offerSearchService) {
    this.offerSearchService = offerSearchService;
  }

  private static OfferSummary convert(OfferPreview offerPreview) {
    return new OfferSummary(offerPreview.getProjectTitle().title(), offerPreview.offerId().id());
  }

  private static Offer convert(life.qbic.finance.domain.model.Offer offer) {
    return new Offer(offer.offerId().id(), offer.projectTitle().title(),
        offer.projectObjective().objective(), offer.experimentalDesignDescription().description());
  }

  /**
   * @inheritDocs
   */
  @Override
  public List<OfferSummary> findOfferContainingProjectTitleOrId(String projectTitle,
      String offerId) {
    return offerSearchService.findByProjectTitleOrOfferId(projectTitle, offerId).stream()
        .map(FinanceServiceImpl::convert).toList();
  }

  /**
   * @inheritDocs
   */
  @Override
  public List<OfferSummary> findOfferContainingProjectTitleOrId(String projectTitle, String offerId,
      int offset, int limit) {
    return offerSearchService.findByProjectTitleOrOfferId(projectTitle, offerId, offset, limit)
        .stream().map(FinanceServiceImpl::convert).toList();
  }

  /**
   * @inheritDocs
   */
  @Override
  public Optional<life.qbic.finances.api.Offer> findOfferById(String offerId) {
    return offerSearchService.findByOfferId(offerId).map(FinanceServiceImpl::convert);
  }

}
