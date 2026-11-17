package life.qbic.identity.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.mfa.RecoveryCode;

/**
 * <b>Recovery code repository</b>
 *
 * <p>Persistence port for {@link RecoveryCode}. Implemented in {@code identity-infrastructure}.</p>
 *
 * <p>The lookup is by hash alone, without a user id, on purpose: the recovery path exists for a user
 * who cannot log in, so the account must be derived <em>from</em> the code, not supplied alongside it.
 * That is also why {@code user_recovery_code.codeHash} is globally unique — two accounts sharing a
 * hash would make an ambiguous code look valid for whoever is asked first.</p>
 *
 * @since 1.0.0
 */
public interface RecoveryCodeRepository {

  /**
   * Stores a freshly generated set.
   *
   * <p>Callers must retire the previous set in the same transaction
   * ({@link #invalidateAllFor}); a partial write here would leave two live sets, which doubles the
   * number of usable break-glass credentials without anyone noticing.</p>
   *
   * @param codes the codes to store
   * @return the persisted, managed instances
   */
  List<RecoveryCode> saveAll(List<RecoveryCode> codes);

  /**
   * @param userId the account
   * @return every code ever generated for the account, including spent and retired ones (audit)
   */
  List<RecoveryCode> findAllByUserId(String userId);

  /**
   * @param userId the account
   * @return how many codes are still usable, for the "you have N codes left" hint (M3-3)
   */
  long countUsableByUserId(String userId);

  /**
   * Retires every usable code of an account because a new set was generated (M3-4).
   *
   * @param userId the account
   * @param now    the regeneration instant
   * @return the number of codes retired
   */
  int invalidateAllFor(String userId, Instant now);

  /**
   * Consumes a code atomically: lock the row, evaluate usability, mark it used.
   *
   * <p><b>This must be one transaction</b>, for the same reason verification-token consumption must
   * be: a read-then-write split lets two concurrent submissions of the same code both see
   * {@code usedAt = NULL} and both succeed, turning one recovery code into two logins. The unique
   * index on {@code codeHash} does not help — it guards inserts, not updates.</p>
   *
   * @param codeHash the hash of the presented code
   * @param now      the consumption instant
   * @return the consumed code if it was usable, otherwise empty — for <em>any</em> reason. The
   *     distinction between "unknown", "already used" and "retired" must not reach the user
   *     (AUTH-NFR-03) and is therefore not modelled as a result type here
   */
  Optional<RecoveryCode> consumeIfUsable(String codeHash, Instant now);
}
