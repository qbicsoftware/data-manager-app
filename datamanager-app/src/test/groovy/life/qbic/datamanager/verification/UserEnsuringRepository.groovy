package life.qbic.datamanager.verification

import jakarta.persistence.EntityManagerFactory
import life.qbic.identity.domain.model.verification.TokenPurpose
import life.qbic.identity.domain.model.verification.VerificationToken
import life.qbic.identity.domain.repository.VerificationTokenRepository

/**
 * Repository that guarantees the owning user row exists before persisting a token.
 *
 * <p>Only needed because the real migration DDL declares
 * {@code FK_user_verification_token_user REFERENCES users(id)}. H2's generated schema omits the FK,
 * so this is invisible until the specs run against MariaDB. It is a test fixture, not a production
 * concern — in production the user always exists, since a token is only ever issued for a
 * registered account.</p>
 */
class UserEnsuringRepository implements VerificationTokenRepository {

    private final PerCallRepository delegate
    private final Closure ensureUser

    UserEnsuringRepository(EntityManagerFactory emf, Closure ensureUser) {
        this.delegate = new PerCallRepository(emf)
        this.ensureUser = ensureUser
    }

    @Override
    VerificationToken save(VerificationToken token) {
        ensureUser.call(token.userId())
        return delegate.save(token)
    }

    @Override
    void lockAccount(String userId) {
        delegate.lockAccount(userId)
    }

    @Override
    Optional<VerificationToken> findByTokenHash(String tokenHash) {
        delegate.findByTokenHash(tokenHash)
    }

    @Override
    VerificationTokenRepository.ConsumeResult consumeIfUsable(String tokenHash, TokenPurpose purpose, Instant now) {
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
