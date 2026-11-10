package life.qbic.datamanager.verification

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import life.qbic.identity.domain.model.verification.TokenPurpose
import life.qbic.identity.domain.model.verification.VerificationToken
import life.qbic.identity.domain.repository.VerificationTokenRepository
import life.qbic.identity.infrastructure.verification.JpaVerificationTokenRepository

/**
 * Repository that opens a fresh EntityManager and transaction per call.
 *
 * <p><b>Spike simplification, not a production recommendation.</b> It mirrors a request-scoped
 * EntityManager closely enough to prove persistence behaviour, but real wiring must put consumption
 * and the state change it authorises (confirming an address, setting a password) in ONE
 * transaction — otherwise a crash between "token consumed" and "email confirmed" strands the user
 * with a dead link and an unconfirmed account.</p>
 */
class PerCallRepository implements VerificationTokenRepository {

    private final EntityManagerFactory emf

    PerCallRepository(EntityManagerFactory emf) {
        this.emf = emf
    }

    private def tx(Closure c) {
        EntityManager em = emf.createEntityManager()
        def tx = em.transaction
        tx.begin()
        try {
            def result = c(new JpaVerificationTokenRepository(em))
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
    void save(VerificationToken token) {
        tx { JpaVerificationTokenRepository repo -> repo.save(token) }
    }

    @Override
    Optional<VerificationToken> findByTokenHash(String tokenHash) {
        tx { JpaVerificationTokenRepository repo -> repo.findByTokenHash(tokenHash) }
    }

    @Override
    VerificationTokenRepository.ConsumeResult consumeIfUsable(String tokenHash, TokenPurpose purpose, Instant now) {
        tx { JpaVerificationTokenRepository repo -> repo.consumeIfUsable(tokenHash, purpose, now) }
    }

    @Override
    List<VerificationToken> findAllByUserIdAndPurpose(String userId, TokenPurpose purpose) {
        tx { JpaVerificationTokenRepository repo -> repo.findAllByUserIdAndPurpose(userId, purpose) }
    }

    @Override
    int supersedeAllFor(String userId, TokenPurpose purpose, String keepTokenHash, Instant now) {
        tx { JpaVerificationTokenRepository repo ->
            repo.supersedeAllFor(userId, purpose, keepTokenHash, now)
        }
    }

    @Override
    int deleteExpiredOrConsumedBefore(Instant cutoff) {
        tx { JpaVerificationTokenRepository repo -> repo.deleteExpiredOrConsumedBefore(cutoff) }
    }
}
