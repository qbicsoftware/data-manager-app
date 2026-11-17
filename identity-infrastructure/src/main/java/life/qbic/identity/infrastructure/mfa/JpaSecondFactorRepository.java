package life.qbic.identity.infrastructure.mfa;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.mfa.SecondFactor;
import life.qbic.identity.domain.repository.SecondFactorRepository;
import org.springframework.stereotype.Repository;

/**
 * <b>JPA second factor repository</b>
 *
 * <p>Persistence adapter for {@link SecondFactor}.</p>
 *
 * <p>Written against {@link EntityManager} rather than Spring Data for the same reason as the
 * verification-token adapter: the caller supplies the transaction (enrolment must commit the factor
 * and its recovery codes together), and several reads here are shaped by intent rather than by a
 * derived query name.</p>
 *
 * @since 1.0.0
 */
@Repository
public class JpaSecondFactorRepository implements SecondFactorRepository {

  private final EntityManager entityManager;

  public JpaSecondFactorRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public SecondFactor save(SecondFactor factor) {
    if (factor.id() == null) {
      entityManager.persist(factor);
      return factor;
    }
    // merge() returns the MANAGED copy and leaves the argument detached. Discarding the return
    // value means a later mutation on `factor` would silently not be flushed.
    return entityManager.merge(factor);
  }

  @Override
  public Optional<SecondFactor> findById(long id) {
    return Optional.ofNullable(entityManager.find(SecondFactor.class, id));
  }

  @Override
  public List<SecondFactor> findAllByUserId(String userId) {
    return entityManager
        .createQuery("""
            SELECT f FROM SecondFactor f
             WHERE f.userId = :userId
             ORDER BY f.createdAt DESC, f.id DESC
            """, SecondFactor.class)
        .setParameter("userId", userId)
        .getResultList();
  }

  @Override
  public List<SecondFactor> findActiveByUserId(String userId) {
    return entityManager
        .createQuery("""
            SELECT f FROM SecondFactor f
             WHERE f.userId = :userId
               AND f.disabledAt IS NULL
             ORDER BY f.createdAt DESC, f.id DESC
            """, SecondFactor.class)
        .setParameter("userId", userId)
        .getResultList();
  }

  @Override
  public boolean hasActiveFactor(String userId) {
    // count rather than getResultList().isEmpty(): the login path asks this on every password
    // success, and it must not pull a factor row (and with it a ciphertext) to answer yes/no.
    Long count = entityManager
        .createQuery("""
            SELECT COUNT(f) FROM SecondFactor f
             WHERE f.userId = :userId
               AND f.disabledAt IS NULL
            """, Long.class)
        .setParameter("userId", userId)
        .getSingleResult();
    return count > 0;
  }
}
