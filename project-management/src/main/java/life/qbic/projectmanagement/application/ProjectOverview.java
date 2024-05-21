package life.qbic.projectmanagement.application;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.data.annotation.Immutable;

/**
 * A limited view of the more complex {@link Project}.
 *
 * @since 1.0.0
 */
//Views are aggregates which cannot be changed
@Immutable
@Entity
@Table(name = "project_overview")
public class ProjectOverview {

  @EmbeddedId()
  private ProjectId id;

  @Column(name = "projectTitle")
  private String projectTitle;

  @Column(name = "projectCode")
  private String projectCode;

  @Column(name = "lastModified")
  private Instant lastModified;

  @Column(name = "principalInvestigatorFullName")
  private String principalInvestigatorName;

  @Column(name = "projectManagerFullName")
  private String projectManagerName;

  @Column(name = "responsibePersonFullName")
  private String projectResponsibleName;

  @Column(name = "amountNgsMeasurements")
  private String ngsMeasurementCount;

  @Column(name = "amountPxpMeasurements")
  private String pxpMeasurementCount;

  @Column(name = "userName")
  @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
  @CollectionTable(name = "project_usernames", joinColumns = @JoinColumn(name = "projectId"))
  private Collection<String> collaboratorUserNames = new ArrayList<>();

  protected ProjectOverview() {

  }

  public ProjectOverview(ProjectId id, String projectTitle, String projectCode,
      Instant lastModified, String principalInvestigatorName, String projectManagerName,
      String projectResponsibleName,
      String ngsMeasurementCount, String pxpMeasurementCount,
      Collection<String> usernames) {
    this.id = Objects.requireNonNull(id, "project Id cannot be null");
    this.projectTitle = Objects.requireNonNull(projectTitle, "projectTitle cannot be null");
    this.projectCode = Objects.requireNonNull(projectCode, "project code cannot be null");
    this.lastModified = Objects.requireNonNull(lastModified, "lastModified date cannot be null");
    this.principalInvestigatorName = Objects.requireNonNull(principalInvestigatorName,
        "principal investigator name cannot be null");
    this.projectManagerName = Objects.requireNonNull(projectManagerName,
        "project manager name cannot be null");
    this.projectResponsibleName = projectResponsibleName;
    this.ngsMeasurementCount = ngsMeasurementCount;
    this.pxpMeasurementCount = pxpMeasurementCount;
    this.collaboratorUserNames = Objects.requireNonNull(usernames,
        "The collaborator user names cannot be null");
  }

  public ProjectId projectId() {
    return id;
  }

  public String projectCode() {
    return projectCode;
  }

  public String projectTitle() {
    return projectTitle;
  }

  public Instant lastModified() {
    return lastModified;
  }

  public String principalInvestigatorName() {
    return principalInvestigatorName;
  }

  public String projectManagerName() {
    return projectManagerName;
  }

  public String projectResponsibleName() {
    return projectResponsibleName;
  }

  public String ngsMeasurementCount() {
    return ngsMeasurementCount;
  }

  public String pxpMeasurementCount() {
    return pxpMeasurementCount;
  }

  public Collection<String> collaboratorUserNames() {
    return collaboratorUserNames.stream().distinct().toList();
  }

}