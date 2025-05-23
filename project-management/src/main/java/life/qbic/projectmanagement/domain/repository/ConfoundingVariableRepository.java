package life.qbic.projectmanagement.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.confounding.jpa.ConfoundingVariableData;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * The repository of confounding variables
 *
 * @since 1.8.0
 */
public interface ConfoundingVariableRepository {


  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  <S extends ConfoundingVariableData> List<S> saveAll(String projectId, Iterable<S> entities);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  List<ConfoundingVariableData> findAll(String projectId, String experimentId);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  List<ConfoundingVariableData> findAllById(String projectId, Iterable<Long> longs);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  boolean existsAllById(String projectId, Collection<Long> longs);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  <S extends ConfoundingVariableData> S save(String projectId, S entity);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  Optional<ConfoundingVariableData> findById(String projectId, Long aLong);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  long countVariablesOfExperiment(String projectId, String experimentId);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  void deleteById(String projectId, Long aLong);

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  void deleteAllById(String projectId, Iterable<Long> longs);

}
