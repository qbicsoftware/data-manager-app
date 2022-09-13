package life.qbic.finance.persistence;

import java.util.List;
import life.qbic.projectmanagement.finances.offer.OfferId;
import life.qbic.projectmanagement.finances.offer.Offer;
import life.qbic.projectmanagement.finances.offer.ProjectTitle;
import org.springframework.data.repository.CrudRepository;

public interface OfferOverviewRepository extends CrudRepository<Offer, Long> {

  List<Offer> findByProjectTitleContainingIgnoreCaseOrOfferIdContainingIgnoreCase(String projectTitle, String offerId);

}
