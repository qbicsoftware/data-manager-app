package life.qbic.projectmanagement.domain.repository;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.confounding.jpa.ConfoundingVariableLevelData;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface ConfoundingVariableLevelRepository {

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  List<ConfoundingVariableLevelData> findAllForSample(ProjectId projectId, String sampleId);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  List<ConfoundingVariableLevelData> findAllForVariable(ProjectId projectId, long variableId);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  Optional<ConfoundingVariableLevelData> findVariableLevelOfSample(ProjectId projectId,
      String sampleId, long variableId);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  <S extends ConfoundingVariableLevelData> S save(ProjectId projectId, S entity);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  Optional<ConfoundingVariableLevelData> findById(ProjectId projectId, Long aLong);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  long countLevelsOfVariable(ProjectId projectId, long variableId);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  void deleteById(ProjectId projectId, Long aLong);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  void deleteAllForVariable(ProjectId projectId, long variableId);
}
