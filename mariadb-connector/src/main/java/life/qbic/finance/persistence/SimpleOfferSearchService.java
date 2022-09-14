package life.qbic.finance.persistence;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.finances.offer.Offer;
import life.qbic.projectmanagement.finances.offer.OfferPreview;
import life.qbic.projectmanagement.finances.offer.api.OfferSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Simple OfferPreview Search Service</b>
 * <p>
 * Basic implementation of the {@link OfferSearchService} interface.
 *
 * @since 1.0.0
 */
@Service
public class SimpleOfferSearchService implements OfferSearchService {

  private final OfferPreviewRepository offerPreviewRepository;

  @Override
  public List<OfferPreview> findByProjectTitleOrOfferId(String projectTitle, String offerId) {
    return offerPreviewRepository.findByProjectTitleContainingIgnoreCaseOrOfferIdContainingIgnoreCase(
        projectTitle, offerId);
  }

  @Override
  public Optional<Offer> findByOfferId(String offerId) {
    return Optional.empty();
  }

  @Autowired
  public SimpleOfferSearchService(OfferPreviewRepository offerPreviewRepository) {
    this.offerPreviewRepository = offerPreviewRepository;
  }
}
