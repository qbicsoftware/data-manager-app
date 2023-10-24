package life.qbic.projectmanagement.infrastructure;

import java.util.List;
import life.qbic.controlling.domain.finances.offer.OfferPreview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * JPA repository for {@link OfferPreview} queries.
 *
 * @since 1.0.0
 */
public interface OfferPreviewRepository extends PagingAndSortingRepository<OfferPreview, Long> {

  /**
   * Case-insensitive search for offer previews that contain a given project title or offer id
   * character sequence
   *
   * @param projectTitle the project title character sequence contained in the project title of an
   *                     offer
   * @param offerId      the offer id character sequence contained in the offer id of an offer
   * @return matching search results
   * @since 1.0.0
   */
  List<OfferPreview> findByProjectTitleContainingIgnoreCaseOrOfferIdContainingIgnoreCase(
      String projectTitle, String offerId);

  /**
   * Case-insensitive and pageable search fo offer previews that contain a given project title or
   * offer id character sequence
   *
   * @param projectTitle the project title character sequence contained in the project title of an
   *                     offer
   * @param offerId      the offer id character sequence contained in the offer id of an offer
   * @param pageable     an implementation of the {@link Pageable} interface
   * @return a {@link Page} containing the query result
   * @since 1.0.0
   */
  Page<OfferPreview> findByProjectTitleContainingIgnoreCaseOrOfferIdContainingIgnoreCase(
      String projectTitle, String offerId, Pageable pageable);


}
