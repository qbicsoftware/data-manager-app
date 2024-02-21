package life.qbic.identity.infrastructure;

import java.util.Collection;
import life.qbic.identity.domain.model.token.PersonalAccessToken;
import org.springframework.data.repository.CrudRepository;

/**
 * <b>Token Jpa Repository/b>
 *
 * <p>Token repository extending Springs {@link CrudRepository} to enable specific queries.</p>
 *
 * @since 1.0.0
 */
public interface TokenJpaRepository extends CrudRepository<PersonalAccessToken, Integer> {

  Collection<PersonalAccessToken> findAllByUserId(String userId);

  PersonalAccessToken findByTokenId(String tokenId);

}
