package life.qbic.projectmanagement.infrastructure.confounding;

import java.util.Collection;
import java.util.List;
import life.qbic.projectmanagement.domain.model.confounding.jpa.ConfoundingVariableData;
import org.springframework.data.repository.ListCrudRepository;

public interface ConfoundingVariableJpaRepository extends
    ListCrudRepository<ConfoundingVariableData, Long> {

  long countByExperimentIdEquals(String experimentId);

  List<ConfoundingVariableData> findAllByExperimentIdEquals(String experimentId);

  boolean existsDistinctByIdIsIn(Collection<Long> ids);
}
