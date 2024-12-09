package life.qbic.projectmanagement.infrastructure.confounding;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.confounding.jpa.ConfoundingVariableData;
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
  public <S extends ConfoundingVariableData> List<S> saveAll(String projectId,
      Iterable<S> entities) {
    return jpaRepository.saveAll(entities);
  }

  @Override
  public List<ConfoundingVariableData> findAll(String projectId, String experimentId) {
    return jpaRepository.findAllByExperimentIdEquals(experimentId);
  }

  @Override
  public List<ConfoundingVariableData> findAllById(String projectId, Iterable<Long> longs) {
    return jpaRepository.findAllById(longs);
  }

  @Override
  public <S extends ConfoundingVariableData> S save(String projectId, S entity) {
    return jpaRepository.save(entity);
  }

  @Override
  public Optional<ConfoundingVariableData> findById(String projectId, Long aLong) {
    return jpaRepository.findById(aLong);
  }

  @Override
  public long countVariablesOfExperiment(String projectId, String experimentId) {
    return jpaRepository.countByExperimentIdEquals(experimentId);
  }

  @Override
  public void deleteById(String projectId, Long aLong) {
    jpaRepository.deleteById(aLong);
  }

  @Override
  public void deleteAllById(String projectId, Iterable<Long> longs) {
    jpaRepository.deleteAllById(longs);
  }
}
