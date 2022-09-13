package life.qbic.finance.persistence;

import java.util.List;
import life.qbic.projectmanagement.finances.offer.OfferId;
import life.qbic.projectmanagement.finances.offer.Offer;
import life.qbic.projectmanagement.finances.offer.ProjectTitle;
import life.qbic.projectmanagement.finances.offer.api.OfferSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class SimpleOfferSearchService implements OfferSearchService {
  private final OfferRepository offerRepository;

  @Override
  public List<Offer> findByProjectTitleOrOfferId(String projectTitle, String offerId) {
    return offerRepository.findByProjectTitleContainingIgnoreCaseOrOfferIdContainingIgnoreCase(projectTitle, offerId);
  }

  @Autowired
  public SimpleOfferSearchService(OfferRepository offerRepository) {
    this.offerRepository = offerRepository;
  }
}
