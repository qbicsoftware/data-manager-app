package life.qbic.finance.persistence;

import java.util.List;
import life.qbic.projectmanagement.finances.offer.Offer;
import org.springframework.data.repository.CrudRepository;

public interface OfferRepository extends CrudRepository<Offer, Long> {

  List<Offer> findByProjectTitleContainingIgnoreCaseOrOfferIdContainingIgnoreCase(String projectTitle, String offerId);

}
