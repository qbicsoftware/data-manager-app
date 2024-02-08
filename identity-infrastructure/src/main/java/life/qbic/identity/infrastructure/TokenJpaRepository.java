package life.qbic.identity.infrastructure;

import life.qbic.identity.domain.model.token.PersonalAccessToken;
import org.springframework.data.repository.CrudRepository;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface TokenJpaRepository extends CrudRepository<PersonalAccessToken, Integer> {

}
