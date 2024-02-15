package life.qbic.projectmanagement.infrastructure.sample.qualitycontrol;

import java.util.List;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.qualitycontrol.QualityControl;
import org.springframework.data.repository.CrudRepository;

/**
 * <b>Quality Control JPA</b>
 * <p>
 * JPA interface for {@link QualityControl} items.
 */
public interface QualityControlJpa extends CrudRepository<QualityControl, Long> {

  List<QualityControl> findQualityControlAssociationByProjectIdEquals(ProjectId projectId);
}
