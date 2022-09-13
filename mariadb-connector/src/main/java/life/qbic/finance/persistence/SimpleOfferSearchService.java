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
  private final OfferOverviewRepository offerOverviewRepository;

  @Override
  public List<Offer> findByProjectTitleOrOfferId(ProjectTitle projectTitle, OfferId offerId) {
    return offerOverviewRepository.findByProjectTitleContainingIgnoreCaseOrOfferIdContainingIgnoreCase(projectTitle.title(), offerId.id());
  }

  @Override
  public List<Offer> findAll() {
    return (List<Offer>) offerOverviewRepository.findAll();
  }

  @Autowired
  public SimpleOfferSearchService(OfferOverviewRepository offerOverviewRepository) {
    this.offerOverviewRepository = offerOverviewRepository;
  }
}
