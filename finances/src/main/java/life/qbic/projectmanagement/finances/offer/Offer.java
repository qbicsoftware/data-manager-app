package life.qbic.projectmanagement.finances.offer;

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

  @Id
  private Long id;

  @Convert(converter = OfferId.Converter.class)
  private OfferId offerId;

  @Convert(converter = ProjectTitle.Converter.class)
  private ProjectTitle projectTitle;

  @Convert(converter = ProjectObjective.Converter.class)
  private ProjectObjective projectObjective;

  @Convert(converter = ExperimentalDesignDescription.Converter.class)
  private ExperimentalDesignDescription experimentalDesignDescription;

  protected Offer(){}

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
