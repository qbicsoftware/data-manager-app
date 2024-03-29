package life.qbic.finance.infrastructure;

import java.util.List;
import java.util.Optional;
import life.qbic.finance.application.OfferSearchService;
import life.qbic.finance.domain.model.Offer;
import life.qbic.finance.domain.model.OfferId;
import life.qbic.finance.domain.model.OfferPreview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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

  private final OfferRepository offerRepository;


  @Override
  @PreAuthorize("hasAnyAuthority('ROLE_PROJECT_MANAGER', 'ROLE_ADMIN')")
  public List<OfferPreview> findByProjectTitleOrOfferId(String projectTitle, String offerId) {
    return offerPreviewRepository.findByProjectTitleContainingIgnoreCaseOrOfferIdContainingIgnoreCase(
        projectTitle, offerId);
  }

  @Override
  @PreAuthorize("hasAnyAuthority('ROLE_PROJECT_MANAGER', 'ROLE_ADMIN')")
  public List<OfferPreview> findByProjectTitleOrOfferId(String projectTitle, String offerId,
      int offset, int limit) {
    return offerPreviewRepository.findByProjectTitleContainingIgnoreCaseOrOfferIdContainingIgnoreCase(
        projectTitle, offerId, new OffsetBasedRequest(offset, limit)).stream().toList();
  }

  @Override
  @PreAuthorize("hasAnyAuthority('ROLE_PROJECT_MANAGER', 'ROLE_ADMIN')")
  public Optional<Offer> findByOfferId(String offerId) {
    return Optional.ofNullable(offerRepository.findByOfferId(OfferId.from(offerId)));
  }

  @Autowired
  public SimpleOfferSearchService(OfferPreviewRepository offerPreviewRepository,
      OfferRepository offerRepository) {
    this.offerPreviewRepository = offerPreviewRepository;
    this.offerRepository = offerRepository;
  }
}
