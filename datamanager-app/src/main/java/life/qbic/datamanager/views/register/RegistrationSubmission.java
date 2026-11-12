package life.qbic.datamanager.views.register;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationResponse;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.application.throttle.AuthAttemptThrottle;
import life.qbic.identity.application.throttle.AuthAttemptThrottle.Scope;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * <b>The registration submission: throttling, uniform outcome, and notification.</b>
 *
 * <p>This is the part of registration that decides what happens, separated from the Vaadin view that
 * renders it. The separation exists because these are exactly the behaviours that were wrong and that a
 * spec could not reach: the view's logic lived in a private method on a {@code @UIScope} component wired
 * to {@code getUI().navigate(...)} and a {@code final} service, so nothing could assert that a duplicate
 * registration sent a notification or that a throttled one disclosed nothing.</p>
 *
 * <h3>The three rules implemented here</h3>
 * <ol>
 *   <li><b>Throttle first.</b> Before any domain work, so a blocked client costs nothing and the
 *       outcome of the submission cannot depend on how much work happened.</li>
 *   <li><b>A duplicate registration is a registration.</b> Same navigation, same wording, no error.
 *       The address owner is told by email instead, so the outcome is not a silent dead end.</li>
 *   <li><b>Never say "already in use".</b> That sentence is the account-existence oracle
 *       ({@code AUTH-NFR-03}); see {@link RegistrationOutcome}.</li>
 * </ol>
 *
 * @since 1.19.0
 */
public class RegistrationSubmission {

  private static final Logger log = LoggerFactory.logger(RegistrationSubmission.class);

  /**
   * Where a submission sends the visitor. Extracted so the flow can be asserted without a UI.
   */
  @FunctionalInterface
  public interface Navigator {

    /**
     * Sends the visitor to the "check your inbox" page.
     *
     * @param userId the id of the account that owns the address, when one exists; empty when not.
     *               Present so the page's "resend" action can work — the visitor already knows the
     *               address they typed, so this discloses nothing further.
     */
    void toPendingConfirmation(Optional<String> userId);
  }

  private final RegistrationIdentity identity;
  private final UserInformationService userInformationService;
  private final AuthAttemptThrottle throttle;

  public RegistrationSubmission(RegistrationIdentity identity,
      UserInformationService userInformationService,
      AuthAttemptThrottle throttle) {
    this.identity = identity;
    this.userInformationService = userInformationService;
    this.throttle = throttle;
  }

  /**
   * Handles one local registration submission.
   *
   * @param information what the visitor entered
   * @param navigator   where to send them
   * @return the failures to render, empty when the visitor was navigated. Returned rather than rendered
   *     so the view keeps control of presentation and this class stays free of Vaadin.
   */
  public List<RuntimeException> submitLocal(RegistrationForm information, Navigator navigator) {
    if (isThrottled(information.email())) {
      navigator.toPendingConfirmation(userIdOf(information.email()));
      return List.of();
    }
    ApplicationResponse response = identity.registerLocal(
        information.fullName(),
        information.userName(),
        information.email(),
        information.password().toCharArray());
    return handleResponse(response, information.email(), navigator);
  }

  /**
   * Handles one ORCID registration submission.
   *
   * @param information   what the visitor entered
   * @param oidcIssuer    the issuer of the ORCID assertion
   * @param oidcId        the subject of the ORCID assertion
   * @param existingLink  whether the assertion is already linked to an account; when it is, there is
   *                      nothing to register
   * @param navigator     where to send them
   * @return the failures to render, empty when the visitor was navigated
   */
  public List<RuntimeException> submitOidc(RegistrationForm information, String oidcIssuer,
      String oidcId, boolean existingLink, Navigator navigator) {
    if (existingLink) {
      return List.of();
    }
    if (isThrottled(information.email())) {
      navigator.toPendingConfirmation(userIdOf(information.email()));
      return List.of();
    }
    ApplicationResponse response = identity.registerOpenId(
        information.fullName(),
        information.userName(),
        information.email(),
        oidcIssuer,
        oidcId);
    return handleResponse(response, information.email(), navigator);
  }

  private List<RuntimeException> handleResponse(ApplicationResponse response, String email,
      Navigator navigator) {
    if (!response.hasFailures()) {
      navigator.toPendingConfirmation(userIdOf(email));
      return List.of();
    }
    if (RegistrationOutcome.decide(response.failures())
        == RegistrationOutcome.Action.UNIFORM_SUCCESS) {
      // Uniform outcome (AUTH-NFR-03). The owner is told by email; the visitor is told nothing.
      notifyAccountOwnerOfRepeatedRegistration(email);
      navigator.toPendingConfirmation(userIdOf(email));
      return List.of();
    }
    return response.failures();
  }

  /**
   * Whether this address has exhausted its registration allowance.
   *
   * <p>Uses the resend policy, which is the tighter of the two: registration sends mail, so it is held
   * to the mail-sending budget rather than the login budget.</p>
   */
  private boolean isThrottled(String email) {
    var outcome = throttle.recordAndReport(Scope.REGISTRATION, email, Instant.now());
    if (outcome.isAllowed()) {
      return false;
    }
    log.debug("Registration submission throttled");
    return true;
  }

  private Optional<String> userIdOf(String email) {
    return userInformationService.findByEmail(email).map(life.qbic.identity.api.UserInfo::id);
  }

  /**
   * Sends the owner of an existing account the standard reset notice, telling them it can be ignored.
   *
   * <p>Reuses the password-reset notification deliberately: it is the one existing message that gives
   * the owner a way forward (set a new password) without disclosing anything to the requester, and it is
   * what the previous error text already pointed at.</p>
   */
  private void notifyAccountOwnerOfRepeatedRegistration(String email) {
    var response = identity.requestPasswordReset(email);
    if (response.hasFailures()) {
      log.debug("Repeated registration for an existing account; notification not dispatched");
    }
  }

  /**
   * What a registration form produces, independent of which form it was.
   *
   * <p>{@code password} is empty for the ORCID flow, which does not collect one — see
   * {@code AUTH-R-07}, which will give those accounts a password through a separate invitation.</p>
   */
  public record RegistrationForm(String fullName, String email, String userName, String password) {

  }
}
