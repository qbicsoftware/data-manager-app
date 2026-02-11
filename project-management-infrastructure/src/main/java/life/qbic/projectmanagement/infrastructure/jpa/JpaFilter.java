package life.qbic.projectmanagement.infrastructure.jpa;

import org.springframework.data.jpa.domain.Specification;

/**
 * A filter for JPA entities. Can be provided by JPA repositores and converted into a {@link Specification}.
 * @param <T> The type of item to filter
 */
public interface JpaFilter<T> {

  Specification<T> asSpecification();
}
