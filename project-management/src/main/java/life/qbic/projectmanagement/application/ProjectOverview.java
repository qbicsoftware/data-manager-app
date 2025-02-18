package life.qbic.projectmanagement.application;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.hibernate.annotations.Immutable;

/**
 * A limited view of the more complex {@link Project}.
 *
 * @since 1.0.0
 */
//Views are aggregates which cannot be changed
@Immutable
@org.springframework.data.annotation.Immutable
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
  private long ngsMeasurementCount;

  @Column(name = "amountPxpMeasurements")
  private long pxpMeasurementCount;


  @Convert(converter = CollaboratorUserInfosConverter.class)
  @Column(name = "userInfos")
  private List<UserInfo> collaboratorUserInfos = new ArrayList<>();

  protected ProjectOverview() {

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

  public long ngsMeasurementCount() {
    return ngsMeasurementCount;
  }

  public long pxpMeasurementCount() {
    return pxpMeasurementCount;
  }

  public Collection<UserInfo> collaboratorUserInfos() {
    return collaboratorUserInfos.stream().distinct().toList();
  }

  public record UserInfo(String userId, String userName) {

  }
}
