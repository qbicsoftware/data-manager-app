package life.qbic.projectmanagement.domain.finances.offer;

import jakarta.persistence.*;

/**
 * <b>Offer entity</b>
 * <p>
 * Represents an offer in the project management context
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "offers")
public class Offer {

  @Id
  private Long id;

  @Convert(converter = OfferId.Converter.class)
  private OfferId offerId;

  @Convert(converter = ProjectTitle.Converter.class)
  private ProjectTitle projectTitle;

  @Convert(converter = ProjectObjective.Converter.class)
  private ProjectObjective projectObjective;

  @Column(name = "experimentalDesign")
  @Convert(converter = ExperimentalDesignDescription.Converter.class)
  private ExperimentalDesignDescription experimentalDesignDescription;

  protected Offer() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public OfferId offerId() {
    return offerId;
  }

  public void setOfferId(OfferId offerId) {
    this.offerId = offerId;
  }

  public ProjectTitle projectTitle() {
    return projectTitle;
  }

  private void setProjectTitle(ProjectTitle projectTitle) {
    this.projectTitle = projectTitle;
  }

  public ProjectObjective projectObjective() {
    return projectObjective;
  }

  private void setProjectObjective(
      ProjectObjective projectObjective) {
    this.projectObjective = projectObjective;
  }

  public ExperimentalDesignDescription experimentalDesignDescription() {
    return experimentalDesignDescription;
  }

  private void setExperimentalDesignDescription(
      ExperimentalDesignDescription experimentalDesignDescription) {
    this.experimentalDesignDescription = experimentalDesignDescription;
  }
}
