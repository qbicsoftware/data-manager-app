package life.qbic.datamanager.verification

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import life.qbic.identity.application.verification.VerificationTokenServiceImpl
import life.qbic.identity.domain.model.verification.TokenPurpose
import life.qbic.identity.domain.model.verification.VerificationToken
import life.qbic.identity.domain.repository.VerificationTokenRepository
import life.qbic.identity.infrastructure.verification.JpaVerificationTokenRepository

import java.time.Duration

/**
 * Runs the service with <b>production transaction semantics</b>: one transaction spans the whole
 * service call, and the repository shares that EntityManager.
 *
 * <p>This matters. A per-call transaction harness makes supersede-then-insert look correct when it
 * is not, or broken when it is, because the two statements no longer commit together. Production
 * has {@code @Transactional} on the service method, so a faithful spec must too — otherwise the spec
 * measures the harness instead of the code.</p>
 */
class ServiceFactory {

    private final EntityManagerFactory emf

    ServiceFactory(EntityManagerFactory emf) {
        this.emf = emf
    }

    /** @return a function that runs a closure inside one transaction with a bound service. */
    def inTransaction(Closure<Object> action) {
        EntityManager em = emf.createEntityManager()
        def tx = em.transaction
        tx.begin()
        try {
            def repository = new SharedRepository(new JpaVerificationTokenRepository(em))
            def service = new VerificationTokenServiceImpl(repository,
                    Duration.ofHours(1), Duration.ofMinutes(5), Duration.ofHours(1))
            def result = action.call(service)
            tx.commit()
            return result
        } catch (Throwable t) {
            if (tx.active) tx.rollback()
            throw t
        } finally {
            em.close()
        }
    }

    /**
     * A repository that uses the caller's EntityManager and does <b>not</b> open transactions, so the
     * surrounding transaction decides when things commit — exactly like the real {@code @Repository}
     * bean under {@code @Transactional}.
     */
    static class SharedRepository implements VerificationTokenRepository {

        private final JpaVerificationTokenRepository delegate

        SharedRepository(JpaVerificationTokenRepository delegate) {
            this.delegate = delegate
        }

        @Override
        void lockAccount(String userId) {
            delegate.lockAccount(userId)
        }

        @Override
        VerificationToken save(VerificationToken token) {
            return delegate.save(token)
        }

        @Override
        Optional<VerificationToken> findByTokenHash(String tokenHash) {
            delegate.findByTokenHash(tokenHash)
        }

        @Override
        VerificationTokenRepository.ConsumeResult consumeIfUsable(
                String tokenHash, TokenPurpose purpose, Instant now) {
            delegate.consumeIfUsable(tokenHash, purpose, now)
        }

        @Override
        List<VerificationToken> findAllByUserIdAndPurpose(String userId, TokenPurpose purpose) {
            delegate.findAllByUserIdAndPurpose(userId, purpose)
        }

        @Override
        int supersedeAllFor(String userId, TokenPurpose purpose, String keepTokenHash, Instant now) {
            delegate.supersedeAllFor(userId, purpose, keepTokenHash, now)
        }

        @Override
        int deleteExpiredOrConsumedBefore(Instant cutoff) {
            delegate.deleteExpiredOrConsumedBefore(cutoff)
        }
    }
}
