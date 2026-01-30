package life.qbic.projectmanagement.infrastructure.jpa;

import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @param <T> The type of item to filter
 */
public interface JpaFilter<T> {

  Specification<T> asSpecification();
}
