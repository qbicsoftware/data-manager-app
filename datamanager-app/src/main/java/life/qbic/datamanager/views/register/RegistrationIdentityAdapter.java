package life.qbic.datamanager.views.register;

import life.qbic.application.commons.ApplicationResponse;
import life.qbic.identity.application.user.IdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Wires {@link RegistrationIdentity} to the identity application service.
 *
 * <p>A thin adapter on purpose: it exists so the registration flow depends on two operations rather
 * than on the whole {@code IdentityService}, and so the flow can be tested with a collaborator that is
 * not {@code final}.</p>
 *
 * @since 1.19.0
 */
@Component
public class RegistrationIdentityAdapter implements RegistrationIdentity {

  private final IdentityService identityService;

  public RegistrationIdentityAdapter(@Autowired IdentityService identityService) {
    this.identityService = identityService;
  }

  @Override
  public ApplicationResponse registerLocal(String fullName, String userName, String email,
      char[] password) {
    return identityService.registerUser(fullName, userName, email, password);
  }
}
