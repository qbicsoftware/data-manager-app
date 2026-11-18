package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.List;
import life.qbic.identity.domain.model.mfa.RecoveryCodes;

/**
 * <b>A freshly generated set of recovery codes, in plaintext for the one time they are shown</b>
 *
 * <p>This object exists for exactly one screen: the moment after generation, when the user must copy
 * the codes down. It is intentionally short-lived and must never be persisted, logged, put in a
 * redirect URL or kept in the session after it has been displayed — only the hashes live in the
 * database (AUTH-R-03: stored so they cannot be reconstructed).</p>
 *
 * @param userId      the account the codes belong to
 * @param codes       the unformatted codes, in display order
 * @param generatedAt when the set was generated
 * @since 1.0.0
 */
public record RecoveryCodeSet(String userId, List<String> codes, Instant generatedAt) {

  public RecoveryCodeSet {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(codes, "codes must not be null");
    requireNonNull(generatedAt, "generatedAt must not be null");
    if (codes.isEmpty()) {
      throw new IllegalArgumentException("a recovery-code set must not be empty");
    }
    codes = List.copyOf(codes);
  }

  /**
   * @return the codes grouped for reading or printing, e.g. {@code A2B3-C4D5-E6F7}
   */
  public List<String> formattedCodes() {
    return codes.stream().map(RecoveryCodes::format).toList();
  }

  /**
   * Deliberately prints only the account: this object is the one place the plaintext codes exist, and
   * a log line is exactly where they must not end up.
   */
  @Override
  public String toString() {
    return "RecoveryCodeSet[userId=" + userId + ", codes=" + codes.size() + ", generatedAt="
        + generatedAt + "]";
  }
}
