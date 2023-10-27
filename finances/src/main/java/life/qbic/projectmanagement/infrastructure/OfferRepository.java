package life.qbic.projectmanagement.infrastructure;

import life.qbic.controlling.domain.finances.offer.Offer;
import life.qbic.controlling.domain.finances.offer.OfferId;
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
