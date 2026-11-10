package life.qbic.identity.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.verification.ConsumeFailure;
import life.qbic.identity.domain.model.verification.TokenPurpose;
import life.qbic.identity.domain.model.verification.VerificationToken;

/**
 * <b>Verification token repository</b>
 *
 * <p>Persistence port for {@link VerificationToken}. Implemented in
 * {@code identity-infrastructure}.</p>
 *
 * @since 1.0.0
 */
public interface VerificationTokenRepository {

  /**
   * Stores a token.
   *
   * @throws org.springframework.dao.DataIntegrityViolationException if the hash is already present.
   *     A duplicate hash is expected under concurrent consumption and must be handled as "already
   *     used", not as an internal error.
   */
  void save(VerificationToken token);

  Optional<VerificationToken> findByTokenHash(String tokenHash);

  /**
   * Consumes a token atomically: lock the row, evaluate usability, mark it used, commit.
   *
   * <p><b>This must be one transaction.</b> A "read, then decide, then write" sequence split across
   * transactions cannot be made single-use by any lock, because the lock is released when the
   * reading transaction commits and the entity handed to the caller is then detached. That failure
   * was measured, not theorised: with a per-call transaction, all 8 of 8 concurrent consumers of
   * the same link succeeded, and a unique index does not help because it guards inserts, not
   * updates.</p>
   *
   * <p>The usability rules stay on the aggregate — the adapter owns the transaction and calls
   * {@link VerificationToken#isUsableAt} / {@link VerificationToken#consume}; it does not
   * re-implement them.</p>
   *
   * @param tokenHash the hash to consume
   * @param purpose   the flow attempting consumption
   * @param now       the consumption instant
   * @return the consumed token if it was usable, or empty if it was not (for any reason)
   */
  ConsumeResult consumeIfUsable(String tokenHash, TokenPurpose purpose, Instant now);

  /**
   * Outcome of {@link #consumeIfUsable}: either the consumed token or the reason it was refused.
   *
   * <p>Returned as a value rather than an exception because refusal is an expected path, not an
   * error — AGENTS.md §5 forbids using exceptions for control flow.</p>
   */
  sealed interface ConsumeResult permits Consumed, Refused {

  }

  /** The token was usable and has been marked consumed. */
  record Consumed(VerificationToken token) implements ConsumeResult {

  }

  /** The token was not usable. The failure is for logging; it must never reach the user. */
  record Refused(ConsumeFailure failure) implements ConsumeResult {

  }

  /**
   * @param userId  the account
   * @param purpose the flow
   * @return every token ever issued for this (user, purpose), newest first
   */
  List<VerificationToken> findAllByUserIdAndPurpose(String userId, TokenPurpose purpose);

  /**
   * Invalidates every still-usable token for this (user, purpose) except the one given.
   *
   * <p>Implements supersede-on-reissue: requesting a new link must kill the old one, otherwise a
   * user could accumulate live links by pressing "resend".</p>
   *
   * @param userId         the account
   * @param purpose        the flow
   * @param keepTokenHash  the hash of the token that stays valid; may be {@code null}
   * @param now            the instant of supersession
   * @return the number of tokens superseded
   */
  int supersedeAllFor(String userId, TokenPurpose purpose, String keepTokenHash, Instant now);

  /**
   * Deletes consumed or expired tokens older than the given instant.
   *
   * <p>Retention is a compliance decision owned by the security and privacy note (#1669); this
   * method exists so the scheduled job can call it, not to fix a retention period here.</p>
   */
  int deleteExpiredOrConsumedBefore(Instant cutoff);
}
