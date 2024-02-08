package life.qbic.identity.application.token;

import java.time.Duration;
import java.util.Collection;
import life.qbic.identity.api.PersonalAccessToken;
import life.qbic.identity.api.PersonalAccessTokenService;
import life.qbic.identity.api.RawToken;
import life.qbic.identity.api.UnknownUserIdException;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.domain.model.token.TokenGenerator;
import life.qbic.identity.domain.model.token.TokenRepository;
import org.springframework.stereotype.Service;

/**
 * <b>Personal Access Token Service</b>
 *
 * <p>Implementation of the {@link PersonalAccessTokenService} interface.</p>
 *
 * @since 1.0.0
 */
@Service
public class PersonalAccessTokenServiceImpl implements PersonalAccessTokenService {

  private final UserInformationService userInformationService;

  private final TokenRepository tokenRepository;

  public PersonalAccessTokenServiceImpl(UserInformationService basicUserInformationService,
      TokenRepository tokenRepository) {
    this.userInformationService = basicUserInformationService;
    this.tokenRepository = tokenRepository;
  }

  private static PersonalAccessToken convert(
      life.qbic.identity.domain.model.token.PersonalAccessToken token) {
    return new PersonalAccessToken(token.tokenId(),
        token.description(), token.expirationDate(),
        token.hasExpired());
  }

  @Override
  public RawToken create(String userId, String description, Duration duration)
      throws UnknownUserIdException {
    var userInfo = userInformationService.findById(userId);
    if (userInfo.isEmpty()) {
      throw new UnknownUserIdException("No user found for id: \"%s\"".formatted(userId));
    }
    return processTokenRequest(userId, description, duration);
  }

  private RawToken processTokenRequest(String userId, String description, Duration duration) {
    TokenGenerator tokenGenerator = new TokenGenerator();
    String rawToken = tokenGenerator.token();
    var token = life.qbic.identity.domain.model.token.PersonalAccessToken.create(userId,
        description, duration, rawToken);
    tokenRepository.save(token);
    return new RawToken(rawToken);
  }

  @Override
  public Collection<PersonalAccessToken> find(String userId) {
    return tokenRepository.findAllByUserId(userId).stream()
        .map(PersonalAccessTokenServiceImpl::convert).toList();
  }

  @Override
  public void delete(String tokenId, String userId) {
    // TODO
  }
}
