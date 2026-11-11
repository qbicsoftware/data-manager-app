package life.qbic.identity.domain.model.verification;

/**
 * <b>Why a token could not be consumed.</b>
 *
 * <p>This lives in the domain, not the application layer, because the decision is made atomically
 * inside the persistence transaction and has to travel back out with it — see
 * {@link life.qbic.identity.domain.repository.VerificationTokenRepository#consumeIfUsable}.</p>
 *
 * <p><b>None of these values may reach the user.</b> AUTH-NFR-03 requires that an unauthenticated
 * response does not disclose account state, so "expired" and "unknown" must produce identical
 * wording. The distinction exists for server-side logs and tests only.</p>
 *
 * @since 1.0.0
 */
public enum ConsumeFailure {

  /** The raw token was absent or blank. */
  MALFORMED,

  /** No token with this hash exists. Indistinguishable from {@link #EXPIRED} to the user. */
  UNKNOWN,

  /** The token exists but was issued for a different flow. */
  WRONG_PURPOSE,

  /** The token was already used. */
  ALREADY_CONSUMED,

  /** A newer token was issued for the same (user, purpose). */
  SUPERSEDED,

  /** The token aged out. */
  EXPIRED
}
