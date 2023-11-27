package life.qbic.projectmanagement.infrastructure.project.purchase;

import life.qbic.projectmanagement.domain.model.project.purchase.ServicePurchase;
import org.springframework.data.repository.CrudRepository;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface ProjectPurchaseJpa extends CrudRepository<ServicePurchase, Long> {

}
