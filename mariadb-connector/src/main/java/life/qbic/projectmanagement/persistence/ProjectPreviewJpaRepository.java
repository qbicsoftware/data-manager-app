package life.qbic.projectmanagement.persistence;

import java.util.List;
import java.util.Objects;
import life.qbic.OffsetBasedRequest;
import life.qbic.projectmanagement.application.api.ProjectPreviewLookup;
import life.qbic.projectmanagement.application.ProjectPreview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Basic implementation to query project preview information
 *
 * @since 1.0.0
 */
@Component
@Scope("singleton")
public class ProjectPreviewJpaRepository implements ProjectPreviewLookup {

  private final ProjectPreviewRepository projectPreviewRepository;

  public ProjectPreviewJpaRepository(@Autowired ProjectPreviewRepository projectPreviewRepository) {
    Objects.requireNonNull(projectPreviewRepository);
    this.projectPreviewRepository = projectPreviewRepository;
  }

  @Override
  public List<ProjectPreview> query(int offset, int limit) {
    return projectPreviewRepository.findAll(new OffsetBasedRequest(offset, limit)).getContent();
  }

  @Override
  public List<ProjectPreview> query(String filter, int offset, int limit) {
    return projectPreviewRepository.findByProjectTitleContainingIgnoreCase(filter,
        new OffsetBasedRequest(offset, limit)).getContent();
  }

}
