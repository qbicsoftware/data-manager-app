package life.qbic.projectmanagement.domain.project;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import life.qbic.projectmanagement.domain.project.repository.jpa.OfferIdentifierConverter;

/**
 * A project planned and run at QBiC.
 *
 * @since <version tag>
 */
@Entity
@Table(name = "projects_datamanager")
public class Project {

  @EmbeddedId
  private ProjectId projectId;

  @Embedded
  private ProjectIntent projectIntent;

  @Convert(converter = ProjectCode.Converter.class)
  @Column(name = "projectCode", nullable = false)
  private ProjectCode projectCode;

  @Column(name = "lastModified", nullable = false)
  private Instant lastModified;

  @ElementCollection(fetch = FetchType.EAGER, targetClass = String.class)
  @Convert(converter = OfferIdentifierConverter.class)
  @CollectionTable(name = "projects_offers", joinColumns = @JoinColumn(name = "projectIdentifier"))
  @Column(name = "offerIdentifier")
  private List<OfferIdentifier> linkedOffers;

  private Project(ProjectId projectId, ProjectIntent projectIntent, ProjectCode projectCode) {
    requireNonNull(projectId);
    requireNonNull(projectIntent);
    requireNonNull(projectCode);
    setProjectId(projectId);
    setProjectIntent(projectIntent);
    setProjectCode(projectCode);
    linkedOffers = new ArrayList<>();
  }

  private void setProjectCode(ProjectCode projectCode) {
    this.projectCode = projectCode;
  }

  protected Project() {
    linkedOffers = new ArrayList<>();
  }

  public void linkOffer(OfferIdentifier offerIdentifier) {
    if (linkedOffers.contains(offerIdentifier)) {
      return;
    }
    linkedOffers.add(offerIdentifier);
    this.lastModified = Instant.now();
  }

  public void unlinkOffer(OfferIdentifier offerIdentifier) {
    boolean offerRemoved = linkedOffers.remove(offerIdentifier);
    if (offerRemoved) {
      this.lastModified = Instant.now();
    }
  }

  public List<OfferIdentifier> linkedOffers() {
    return linkedOffers.stream().toList();
  }

  protected void setProjectId(ProjectId projectId) {
    this.projectId = projectId;
    this.lastModified = Instant.now();
  }

  protected void setProjectIntent(ProjectIntent projectIntent) {
    this.projectIntent = projectIntent;
    this.lastModified = Instant.now();
  }

  /**
   * Creates a new project with code and project intent
   *
   * @param projectIntent the intent of the project
   * @return a new project instance
   */
  public static Project create(ProjectIntent projectIntent, ProjectCode projectCode) {
    return new Project(ProjectId.create(), projectIntent, projectCode);
  }

  /**
   * Generates a project with the specified values injected.
   *
   * @param projectId     the identifier of the project
   * @param projectIntent the project intent
   * @return a project with the given identity and project intent
   */
  public static Project of(ProjectId projectId, ProjectIntent projectIntent,
      ProjectCode projectCode) {
    return new Project(projectId, projectIntent, projectCode);
  }

  public ProjectId getId() {
    return projectId;
  }

  public ProjectIntent getProjectIntent() {
    return projectIntent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Project project = (Project) o;

    return projectId.equals(project.projectId);
  }

  @Override
  public int hashCode() {
    return projectId.hashCode();
  }
}
