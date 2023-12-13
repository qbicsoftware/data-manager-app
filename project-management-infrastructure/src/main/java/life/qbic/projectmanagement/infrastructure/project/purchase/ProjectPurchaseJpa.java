package life.qbic.projectmanagement.infrastructure.project.purchase;

import java.util.List;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.purchase.ServicePurchase;
import org.springframework.data.repository.CrudRepository;

/**
 * <b>Project Purchase Jpa</b>
 * <p>
 * JPA interface for {@link ServicePurchase} items.
 *
 * @since 1.0.0
 */
public interface ProjectPurchaseJpa extends CrudRepository<ServicePurchase, Long> {

  List<ServicePurchase> findServicePurchasesByProjectIdEquals(ProjectId projectId);
}
