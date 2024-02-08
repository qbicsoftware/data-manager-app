package life.qbic.identity.application.token;

import java.util.Collection;
import life.qbic.identity.api.PersonalAccessToken;
import life.qbic.identity.api.PersonalAccessTokenService;
import life.qbic.identity.api.RawToken;
import life.qbic.identity.api.UnknownUserIdException;
import life.qbic.identity.api.UserInformationService;

/**
 * <b>Personal Access Token Service</b>
 *
 * <p>Implementation of the {@link PersonalAccessTokenService} interface.</p>
 *
 * @since 1.0.0
 */
public class PersonalAccessTokenServiceImpl implements PersonalAccessTokenService {

  private final UserInformationService userInformationService;

  public PersonalAccessTokenServiceImpl(UserInformationService basicUserInformationService) {
    this.userInformationService = basicUserInformationService;
  }

  @Override
  public RawToken create(String userId) throws UnknownUserIdException {
    var userInfo = userInformationService.findById(userId);
    if (userInfo.isEmpty()) {
      throw new UnknownUserIdException("No user found for id: \"%s\"".formatted(userId));
    }
    return processTokenRequest(userId);
  }

  private RawToken processTokenRequest(String userId) {
    return null;
  }


  @Override
  public Collection<PersonalAccessToken> find(String userId) {
    return null;
  }
}
