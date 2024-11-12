package life.qbic.projectmanagement.infrastructure.confounding;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.confounding.jpa.ConfoundingVariableLevelData;
import org.springframework.data.repository.ListCrudRepository;

public interface ConfoundingVariableLevelJpaRepository extends
    ListCrudRepository<ConfoundingVariableLevelData, Long> {

  List<ConfoundingVariableLevelData> findAllBySampleIdEquals(String sampleId);

  List<ConfoundingVariableLevelData> findAllByVariableIdEquals(long variableId);

  Optional<ConfoundingVariableLevelData> findBySampleIdEqualsAndVariableIdEquals(String sampleId,
      long variableId);

  long countByVariableIdEquals(long variableId);

  void deleteAllByVariableIdEquals(long variableId);

}
