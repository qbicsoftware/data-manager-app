package life.qbic.finance.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import static java.util.Objects.requireNonNull;

/**
 * <b>OfferPreview</b>
 * <p>
 * Describes an offer object in the context of project management.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "offers")
public class OfferPreview {

  private String projectTitle;

  private String offerId;
  @Id
  @Column(name = "id")
  private Long id;

  protected OfferPreview() {

  }

  /**
   * Creates a new instance of {@link OfferPreview} from a title and offer id.
   *
   * @param projectTitle the project title found in an offer
   * @param offerId      the offer id, uniquely representing the offer resource
   * @return an offer instance
   * @since 1.0.0
   */
  public static OfferPreview from(ProjectTitle projectTitle, OfferId offerId) {
    requireNonNull(projectTitle);
    requireNonNull(offerId);
    return new OfferPreview(projectTitle.title(), offerId.id());
  }

  private OfferPreview(String projectTitle, String offerId) {
    this.projectTitle = projectTitle;
    this.offerId = offerId;
  }

  public ProjectTitle getProjectTitle() {
    return ProjectTitle.from(projectTitle);
  }

  public void setProjectTitle(String projectTitle) {
    this.projectTitle = projectTitle;
  }

  public OfferId offerId() {
    return OfferId.from(offerId);
  }

  private void setOfferId(OfferId offerId) {
    this.offerId = offerId.id();
  }

  private void setId(Long id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OfferPreview offerPreview = (OfferPreview) o;
    return Objects.equals(projectTitle, offerPreview.projectTitle) && Objects.equals(
        offerId, offerPreview.offerId) && Objects.equals(id, offerPreview.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectTitle, offerId, id);
  }
}
