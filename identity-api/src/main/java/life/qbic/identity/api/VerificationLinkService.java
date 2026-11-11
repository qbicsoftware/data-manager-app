package life.qbic.identity.api;

import java.time.Instant;

/**
 * <b>Verification link service</b>
 *
 * <p>Issues and consumes the single-use tokens behind email confirmation and password reset links
 * (AUTH-R-05). Lives in {@code identity-api} so the UI layer can depend on the contract without
 * reaching into the identity domain model — per AGENTS.md, views must not import domain classes.</p>
 *
 * <p><b>Only a hash is stored.</b> {@link Issued#rawToken()} is the single moment the raw token
 * exists; it goes into the outgoing email and can never be read back, by anyone.</p>
 *
 * <p><b>All failures are the same failure.</b> {@link #consume} deliberately does not tell the
 * caller <em>why</em> a token was rejected (unknown, expired, already used, wrong flow). AUTH-NFR-03
 * requires that an unauthenticated response does not disclose account state, so the caller must
 * render one message for every rejection.</p>
 *
 * @since 1.0.0
 */
public interface VerificationLinkService {

  /**
   * Issues a token, invalidating any previous token for the same user and flow.
   *
   * @param userId   the account
   * @param purpose  the flow the link authorises
   * @param issuedAt the issuing instant
   * @return the raw token and its expiry. Store nothing: the raw token cannot be recovered later.
   */
  Issued issue(String userId, Purpose purpose, Instant issuedAt);

  /**
   * Consumes a token.
   *
   * @param rawToken the raw token from the link
   * @param purpose  the flow attempting to consume it
   * @param now      the consumption instant
   * @return the owning user id, or empty for <em>any</em> rejection
   */
  java.util.Optional<String> consume(String rawToken, Purpose purpose, Instant now);

  /**
   * The flow a link authorises. A closed set, so a confirmation link can never be replayed at the
   * password-reset endpoint.
   */
  enum Purpose {
    EMAIL_CONFIRMATION,
    PASSWORD_RESET,
    SET_PASSWORD
  }

  /**
   * @param rawToken the raw token, to be placed in the link
   * @param expiresAt when it stops working
   */
  record Issued(String rawToken, Instant expiresAt) {

  }
}
