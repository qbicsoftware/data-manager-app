package life.qbic.projectmanagement.infrastructure.project.purchase;

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

}
