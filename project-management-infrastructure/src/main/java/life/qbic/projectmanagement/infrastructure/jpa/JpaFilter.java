package life.qbic.projectmanagement.infrastructure.jpa;

import org.springframework.data.jpa.domain.Specification;

public interface JpaFilter<T> {

  Specification<T> asSpecification();

  interface JpaFilterBuilder<S, T extends JpaFilter<S>, X extends JpaFilterBuilder<S, T, X>> {

    T build();
  }
}
