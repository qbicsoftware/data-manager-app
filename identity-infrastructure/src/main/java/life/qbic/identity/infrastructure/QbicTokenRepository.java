package life.qbic.identity.infrastructure;

import java.util.Collection;
import java.util.Optional;
import life.qbic.identity.domain.model.token.PersonalAccessToken;
import life.qbic.identity.domain.model.token.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * <b>Qbic Token Repository</b>
 *
 * <p>Implementation of the {@link TokenRepository} interface. </p>
 *
 * @since 1.0.0
 */
@Repository
public class QbicTokenRepository implements TokenRepository {

  private final TokenJpaRepository tokenJpaRepository;

  @Autowired
  public QbicTokenRepository(TokenJpaRepository tokenJpaRepository) {
    this.tokenJpaRepository = tokenJpaRepository;
  }

  @Override
  public Collection<PersonalAccessToken> findAllByUserId(String userId) {
    return tokenJpaRepository.findAllByUserId(userId);
  }

  @Override
  public void save(PersonalAccessToken token) {
    this.tokenJpaRepository.save(token);
  }

  @Override
  public void delete(PersonalAccessToken token) {
    tokenJpaRepository.delete(token);
  }

  @Override
  public Optional<PersonalAccessToken> find(String accessTokenId) {
    return Optional.ofNullable(tokenJpaRepository.findByTokenId(accessTokenId));
  }
}
