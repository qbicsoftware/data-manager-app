package life.qbic.projectmanagement.application.api;

import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.sample.SampleCode;

public interface SampleCodeService {

  Result<SampleCode, ResponseCode> generateFor(ProjectId projectId);

  enum ResponseCode {
    SAMPLE_STATISTICS_RECORD_NOT_FOUND
  }

}
