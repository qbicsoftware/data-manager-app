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

  @Column(name = "amountIpMeasurements")
  private long ipMeasurementCount;

  /** Aggregate count of datasets currently connected to the project. */
  @Column(name = "connectedDatasetCount", nullable = false)
  private int connectedDatasetCount;

  /** Aggregate count of connected datasets with PUBLIC access level. */
  @Column(name = "openDatasetCount", nullable = false)
  private int openDatasetCount;

  /** Aggregate count of connected datasets with RESTRICTED access level. */
  @Column(name = "restrictedDatasetCount", nullable = false)
  private int restrictedDatasetCount;

  /**
   * Most-recent {@code connected_on} timestamp across all connected
   * datasets for this project. Null when no datasets are connected.
   */
  @Column(name = "lastConnectedOn")
  private Instant lastConnectedOn;


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

  public long ipMeasurementCount() {
    return ipMeasurementCount;
  }

  public int connectedDatasetCount() {
    return connectedDatasetCount;
  }

  public int openDatasetCount() {
    return openDatasetCount;
  }

  public int restrictedDatasetCount() {
    return restrictedDatasetCount;
  }

  /** Nullable — {@code null} for projects with no connected datasets. */
  public Instant lastConnectedOn() {
    return lastConnectedOn;
  }

  public Collection<UserInfo> collaboratorUserInfos() {
    return collaboratorUserInfos.stream().distinct().toList();
  }

  public record UserInfo(String userId, String userName) {

  }
}
