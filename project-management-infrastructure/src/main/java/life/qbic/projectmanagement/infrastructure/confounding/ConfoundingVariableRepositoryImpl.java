package life.qbic.projectmanagement.infrastructure.confounding;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.confounding.jpa.ConfoundingVariableData;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.ConfoundingVariableRepository;
import org.springframework.stereotype.Repository;

/**
 * The implementation of the {@link ConfoundingVariableRepository} interface.
 * Delegates method calls to the corresponding JPA repositories.
 * @since 1.6.0
 */
@Repository
public class ConfoundingVariableRepositoryImpl implements ConfoundingVariableRepository {

  private final ConfoundingVariableJpaRepository jpaRepository;

  public ConfoundingVariableRepositoryImpl(ConfoundingVariableJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public <S extends ConfoundingVariableData> List<S> saveAll(ProjectId projectId,
      Iterable<S> entities) {
    return jpaRepository.saveAll(entities);
  }

  @Override
  public List<ConfoundingVariableData> findAll(ProjectId projectId, String experimentId) {
    return jpaRepository.findAllByExperimentIdEquals(experimentId);
  }

  @Override
  public List<ConfoundingVariableData> findAllById(ProjectId projectId, Iterable<Long> longs) {
    return jpaRepository.findAllById(longs);
  }

  @Override
  public <S extends ConfoundingVariableData> S save(ProjectId projectId, S entity) {
    return jpaRepository.save(entity);
  }

  @Override
  public Optional<ConfoundingVariableData> findById(ProjectId projectId, Long aLong) {
    return jpaRepository.findById(aLong);
  }

  @Override
  public long countVariablesOfExperiment(ProjectId projectId, String experimentId) {
    return jpaRepository.countByExperimentIdEquals(experimentId);
  }

  @Override
  public void deleteById(ProjectId projectId, Long aLong) {
    jpaRepository.deleteById(aLong);
  }

  @Override
  public void deleteAllById(ProjectId projectId, Iterable<Long> longs) {
    jpaRepository.deleteAllById(longs);
  }
}
