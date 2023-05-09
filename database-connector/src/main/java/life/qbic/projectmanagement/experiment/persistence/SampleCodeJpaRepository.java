package life.qbic.projectmanagement.experiment.persistence;

import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.sample.SampleCode;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class SampleCodeJpaRepository implements SampleCodeService {

  @Override
  public SampleCode generateFor(ProjectId projectId) {
    return null;
  }
}
