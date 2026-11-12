package life.qbic.datamanager.views.register;

import life.qbic.application.commons.ApplicationResponse;

/**
 * <b>The identity operations registration needs, and nothing else.</b>
 *
 * <p>Registration touches exactly two things: create a local account, and create an account from an ORCID
 * assertion. Naming them as a port keeps the registration flow independent of the remaining twenty-odd
 * operations on {@code IdentityService}, and — practically — makes the flow testable: {@code IdentityService}
 * is {@code final}, so it cannot be mocked, which is how the account-existence decision ended up untested
 * inside a view in the first place.</p>
 *
 * <p>Implemented for Spring in {@code RegistrationIdentityAdapter}.</p>
 *
 * @since 1.19.0
 */
public interface RegistrationIdentity {

  /**
   * Creates a local account.
   *
   * @return failure carrying {@code UserExistsException} when the address is already registered — the
   *     caller must treat that as a success and must not render it ({@code AUTH-NFR-03}).
   */
  ApplicationResponse registerLocal(String fullName, String userName, String email,
      char[] password);

  /**
   * Creates an account from an ORCID assertion.
   *
   * @return failure carrying {@code UserExistsException} when the address is already registered
   */
  ApplicationResponse registerOpenId(String fullName, String userName, String email, String issuer,
      String subject);

}
