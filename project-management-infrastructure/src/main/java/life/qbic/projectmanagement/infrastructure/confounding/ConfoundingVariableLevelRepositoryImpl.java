package life.qbic.projectmanagement.infrastructure.confounding;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.confounding.jpa.ConfoundingVariableLevelData;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.ConfoundingVariableLevelRepository;
import org.springframework.stereotype.Repository;

/**
 * The implementation of the {@link ConfoundingVariableLevelRepository} interface.
 * Delegates method calls to the corresponding JPA repositories.
 * @since 1.6.0
 */
@Repository
public class ConfoundingVariableLevelRepositoryImpl implements ConfoundingVariableLevelRepository {

  private final ConfoundingVariableLevelJpaRepository jpaRepository;

  public ConfoundingVariableLevelRepositoryImpl(
      ConfoundingVariableLevelJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public List<ConfoundingVariableLevelData> findAllForSample(ProjectId projectId, String sampleId) {
    return jpaRepository.findAllBySampleIdEquals(sampleId);
  }

  @Override
  public List<ConfoundingVariableLevelData> findAllForVariable(ProjectId projectId,
      long variableId) {
    return jpaRepository.findAllByVariableIdEquals(variableId);
  }

  @Override
  public Optional<ConfoundingVariableLevelData> findVariableLevelOfSample(ProjectId projectId,
      String sampleId, long variableId) {
    return jpaRepository.findBySampleIdEqualsAndVariableIdEquals(sampleId, variableId);
  }

  @Override
  public <S extends ConfoundingVariableLevelData> S save(ProjectId projectId, S entity) {
    return jpaRepository.save(entity);
  }

  @Override
  public Optional<ConfoundingVariableLevelData> findById(ProjectId projectId, Long aLong) {
    return jpaRepository.findById(aLong);
  }

  @Override
  public long countLevelsOfVariable(ProjectId projectId, long variableId) {
    return jpaRepository.countByVariableIdEquals(variableId);
  }

  @Override
  public void deleteById(ProjectId projectId, Long aLong) {
    jpaRepository.deleteById(aLong);
  }

  @Override
  public void deleteAllForVariable(ProjectId projectId, long variableId) {
    jpaRepository.deleteAllByVariableIdEquals(variableId);
  }
}
