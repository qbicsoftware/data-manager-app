package life.qbic.finance.infrastructure;

import life.qbic.finance.domain.model.Offer;
import life.qbic.finance.domain.model.OfferId;
import org.springframework.data.repository.CrudRepository;

/**
 * Offer JPA repository
 *
 * @since 1.0.0
 */
public interface OfferRepository extends CrudRepository<Offer, Long> {

  /**
   * Searches for an {@link Offer} based on the provided offer id.
   *
   * @param offerId the offer identifier
   * @return the offer if found, else null
   * @since 1.0.0
   */
  Offer findByOfferId(OfferId offerId);

}
