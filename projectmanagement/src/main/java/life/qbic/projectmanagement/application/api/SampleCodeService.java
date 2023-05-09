package life.qbic.projectmanagement.application.api;

import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.sample.SampleCode;

public interface SampleCodeService {

  SampleCode generateFor(ProjectId projectId);

}
