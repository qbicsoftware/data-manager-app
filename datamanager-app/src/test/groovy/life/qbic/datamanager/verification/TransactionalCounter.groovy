package life.qbic.datamanager.verification

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import life.qbic.identity.application.verification.AuthAttemptThrottle
import life.qbic.identity.application.verification.AuthAttemptThrottle.AttemptCounter
import life.qbic.identity.infrastructure.verification.JpaAttemptCounter

import java.time.Duration
import java.time.Instant

/**
 * {@link AttemptCounter} that opens a transaction per call.
 *
 * <p>{@link JpaAttemptCounter} issues native update statements, which Hibernate refuses outside a
 * transaction ({@code TransactionRequiredException}). In production the transaction comes from a
 * Spring {@code @Transactional} boundary on the login/reset path; here it is opened explicitly so
 * the specs exercise the same statements.</p>
 */
class TransactionalCounter implements AttemptCounter {

    private final EntityManagerFactory emf
    private final int maxAttempts

    TransactionalCounter(EntityManagerFactory emf, int maxAttempts) {
        this.emf = emf
        this.maxAttempts = maxAttempts
    }

    private def tx(Closure c) {
        EntityManager em = emf.createEntityManager()
        def tx = em.transaction
        tx.begin()
        try {
            def result = c(new JpaAttemptCounter(em, maxAttempts,
                    Duration.ofMinutes(10), Duration.ofMinutes(5)))
            tx.commit()
            return result
        } catch (Throwable t) {
            if (tx.active) tx.rollback()
            throw t
        } finally {
            em.close()
        }
    }

    @Override
    AttemptCounter.Counter increment(AuthAttemptThrottle.Scope scope, String key, Instant now) {
        tx { JpaAttemptCounter counter -> counter.increment(scope, key, now) }
    }

    @Override
    void clear(AuthAttemptThrottle.Scope scope, String key) {
        tx { JpaAttemptCounter counter -> counter.clear(scope, key) }
    }
}
