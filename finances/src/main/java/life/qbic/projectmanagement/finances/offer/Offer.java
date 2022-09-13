package life.qbic.projectmanagement.finances.offer;

import static java.util.Objects.requireNonNull;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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

  public static Offer from(ProjectTitle title, OfferId offerId) {
    requireNonNull(title);
    requireNonNull(offerId);
    return new Offer(title.title(), offerId.id());
  }

  private Offer(String projectTitle, String offerId) {
    this.projectTitle = projectTitle;
    this.offerId = offerId;
  }

  public ProjectTitle getProjectTitle() {
    return new ProjectTitle(projectTitle);
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
}
