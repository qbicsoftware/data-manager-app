package life.qbic.projectmanagement.application.api;

import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;

/**
 * A sample code service offers functionality within the scope of sample code handling in projects.
 *
 * @since 1.0.0
 */
public interface SampleCodeService {

  /**
   * Generates a project unique sample code.
   * <p>
   * It is guaranteed, that subsequent calling with the same input parameters does not result in
   * code duplicates.
   *
   * @param projectId the project reference to generate a sample code for
   * @return a result with the sample code or an error response code
   * @since 1.0.0
   */
  Result<SampleCode, ResponseCode> generateFor(ProjectId projectId);

  /**
   * Creates a sample stats entry for a project.
   * <p>
   * The implementation must guarantee to be idempotent: multiple calls with the same input
   * parameters must not override existing entries.
   *
   * @param projectId   the project reference
   * @param projectCode the project code
   * @since 1.0.0
   */
  void addProjectToSampleStats(ProjectId projectId, ProjectCode projectCode);

  /**
   * Examines if a sample statistics entry for a project already exists.
   *
   * @param projectId the project reference to examine
   * @return true, if a sample statistics entry already exists for the project, else false
   * @since 1.0.0
   */
  boolean sampleStatisticsEntryExistsFor(ProjectId projectId);

  enum ResponseCode {

    SAMPLE_STATISTICS_RECORD_NOT_FOUND
  }

}
