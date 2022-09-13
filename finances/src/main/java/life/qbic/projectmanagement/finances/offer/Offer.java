package life.qbic.projectmanagement.finances.offer;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * <b>Offer</b>
 * <p>
 * Describes an offer object in the context of project management.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "offers")
public class Offer {

  private String projectTitle;

  private String offerId;
  @Id
  @Column(name = "id")
  private Long id;

  protected Offer() {

  }

  /**
   * Creates a new instance of {@link Offer} from a title and offer id.
   *
   * @param projectTitle the project title found in an offer
   * @param offerId      the offer id, uniquely representing the offer resource
   * @return an offer instance
   * @since 1.0.0
   */
  public static Offer from(ProjectTitle projectTitle, OfferId offerId) {
    requireNonNull(projectTitle);
    requireNonNull(offerId);
    return new Offer(projectTitle.title(), offerId.id());
  }

  private Offer(String projectTitle, String offerId) {
    this.projectTitle = projectTitle;
    this.offerId = offerId;
  }

  public ProjectTitle getProjectTitle() {
    return ProjectTitle.of(projectTitle);
  }

  public void setProjectTitle(String projectTitle) {
    this.projectTitle = projectTitle;
  }

  public OfferId offerId() {
    return OfferId.of(offerId);
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
    Offer offer = (Offer) o;
    return Objects.equals(projectTitle, offer.projectTitle) && Objects.equals(
        offerId, offer.offerId) && Objects.equals(id, offer.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectTitle, offerId, id);
  }
}
