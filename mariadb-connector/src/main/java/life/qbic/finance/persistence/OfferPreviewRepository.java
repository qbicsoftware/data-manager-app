package life.qbic.finance.persistence;

import java.util.List;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import org.springframework.data.repository.CrudRepository;

/**
 * JPA repository for {@link OfferPreview} queries.
 *
 * @since 1.0.0
 */
public interface OfferPreviewRepository extends CrudRepository<OfferPreview, Long> {

  /**
   * Case-insensitive search for offer previews that contain a given project title or offer id character
   * sequence
   *
   * @param projectTitle the project title character sequence contained in the project title of an
   *                     offer
   * @param offerId      the offer id character sequence contained in the offer id of an offer
   * @return matching search results
   * @since 1.0.0
   */
  List<OfferPreview> findByProjectTitleContainingIgnoreCaseOrOfferIdContainingIgnoreCase(
      String projectTitle, String offerId);

}
