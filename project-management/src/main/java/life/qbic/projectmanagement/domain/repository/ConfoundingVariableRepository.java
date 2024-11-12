package life.qbic.projectmanagement.domain.repository;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.confounding.jpa.ConfoundingVariableData;
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
public interface ConfoundingVariableRepository {


  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  <S extends ConfoundingVariableData> List<S> saveAll(ProjectId projectId, Iterable<S> entities);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  List<ConfoundingVariableData> findAll(ProjectId projectId, String experimentId);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  List<ConfoundingVariableData> findAllById(ProjectId projectId, Iterable<Long> longs);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  <S extends ConfoundingVariableData> S save(ProjectId projectId, S entity);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  Optional<ConfoundingVariableData> findById(ProjectId projectId, Long aLong);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  long countVariablesOfExperiment(ProjectId projectId, String experimentId);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  void deleteById(ProjectId projectId, Long aLong);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  void deleteAllById(ProjectId projectId, Iterable<Long> longs);

}
