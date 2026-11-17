package life.qbic.identity.domain.repository;

import java.util.Optional;
import life.qbic.identity.domain.model.mfa.MfaGrace;

/**
 * <b>MFA grace window repository</b>
 *
 * <p>Persistence port for {@link MfaGrace}. Implemented in {@code identity-infrastructure}.</p>
 *
 * @since 1.0.0
 */
public interface MfaGraceRepository {

  /**
   * @param userId the account
   * @return the stored window, or empty if the account has never been subject to
   *     {@code REQUIRED}. Empty means "grant one", not "deny" — see {@code MfaGate}.
   */
  Optional<MfaGrace> findByUserId(String userId);

  /**
   * Opens a window if the account has none, and does nothing otherwise.
   *
   * <p><b>First write wins, atomically.</b> Two logins of the same account at the same moment must not
   * both grant a window: a read-then-write would let the second one overwrite the first deadline,
   * and — worse — the naive "delete and re-insert" variant would let anyone extend their own window by
   * logging in twice, turning the one deadline the whole enforcement depends on into an
   * indefinitely renewable one. The adapter therefore performs an insert-if-absent in a single
   * statement and returns whatever is stored afterwards.</p>
   *
   * @param candidate the window to store if none exists
   * @return the stored window — the candidate if this call was the first, otherwise the existing one
   */
  MfaGrace startIfAbsent(MfaGrace candidate);
}
