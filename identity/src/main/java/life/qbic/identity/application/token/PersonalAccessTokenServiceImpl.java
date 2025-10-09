package life.qbic.identity.application.token;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.Collection;
import life.qbic.identity.api.PersonalAccessToken;
import life.qbic.identity.api.PersonalAccessTokenService;
import life.qbic.identity.api.RawToken;
import life.qbic.identity.api.UnknownUserIdException;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.domain.model.token.TokenEncoder;
import life.qbic.identity.domain.model.token.TokenGenerator;
import life.qbic.identity.domain.model.token.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final TokenEncoder tokenEncoder;

  public PersonalAccessTokenServiceImpl(UserInformationService basicUserInformationService,
      TokenRepository tokenRepository,
      TokenEncoder tokenEncoder) {
    this.userInformationService = requireNonNull(basicUserInformationService,
        "basicUserInformationService must not be null");
    this.tokenRepository = requireNonNull(tokenRepository, "tokenRepository must not be null");
    this.tokenEncoder = requireNonNull(tokenEncoder, "tokenEncoder must not be null");
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
    String rawToken = TokenGenerator.token();
    String encodedToken = tokenEncoder.encode(rawToken.toCharArray());
    var token = new life.qbic.identity.domain.model.token.PersonalAccessToken(userId, description,
        duration, encodedToken);
    tokenRepository.save(token);
    return new RawToken(rawToken);
  }

  @Override
  public Collection<PersonalAccessToken> findAll(String userId) {
    return tokenRepository.findAllByUserId(userId).stream()
        .map(PersonalAccessTokenServiceImpl::convert).toList();
  }

  @Override
  public void delete(String tokenId, String userId) {
    tokenRepository.findAllByUserId(userId).stream()
        .filter(token -> token.tokenId().equals(tokenId))
        .findAny().ifPresent(tokenRepository::delete);
  }
}
