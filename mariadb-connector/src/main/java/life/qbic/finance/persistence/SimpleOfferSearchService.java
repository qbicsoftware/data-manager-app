package life.qbic.finance.persistence;

import java.util.List;
import life.qbic.projectmanagement.finances.offer.Offer;
import life.qbic.projectmanagement.finances.offer.api.OfferSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Simple Offer Search Service</b>
 * <p>
 * Basic implementation of the {@link OfferSearchService} interface.
 *
 * @since 1.0.0
 */
@Service
public class SimpleOfferSearchService implements OfferSearchService {

  private final OfferRepository offerRepository;

  @Override
  public List<Offer> findByProjectTitleOrOfferId(String projectTitle, String offerId) {
    return offerRepository.findByProjectTitleContainingIgnoreCaseOrOfferIdContainingIgnoreCase(
        projectTitle, offerId);
  }

  @Autowired
  public SimpleOfferSearchService(OfferRepository offerRepository) {
    this.offerRepository = offerRepository;
  }
}
