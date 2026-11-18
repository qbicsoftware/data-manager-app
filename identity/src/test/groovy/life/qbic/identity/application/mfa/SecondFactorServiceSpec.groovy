package life.qbic.identity.application.mfa

import life.qbic.identity.domain.model.mfa.ProtectedSecret
import life.qbic.identity.domain.model.mfa.SecondFactor
import life.qbic.identity.domain.repository.SecondFactorRepository
import spock.lang.Specification

import java.time.Instant

/**
 * <b>Ownership is the rule that matters here (AUTH-R-02).</b>
 *
 * <p>A factor id arrives from the UI. "It exists" must never be enough to disable it, and a factor of
 * another account must be indistinguishable from one that does not exist — otherwise the settings page
 * becomes a cross-account existence oracle.</p>
 */
class SecondFactorServiceSpec extends Specification {

    private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z")
    private static final ProtectedSecret SECRET =
            ProtectedSecret.ofBase64(Base64.encoder.encodeToString(new byte[32]))

    FakeRepository repository
    SecondFactorService service

    def setup() {
        repository = new FakeRepository()
        service = new SecondFactorService(repository)
    }

    def "active factors of the account are listed, disabled ones are not"() {
        given:
        def active = repository.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", NOW))
        def removed = repository.save(SecondFactor.enrolTotp("u-1", SECRET, "Old phone", NOW))
        removed.disable(NOW.plusSeconds(1))
        repository.save(SecondFactor.enrolTotp("u-2", SECRET, "Someone else", NOW))

        expect:
        service.activeFactors("u-1")*.displayName() == ["iPhone"]
    }

    def "a user can disable their own factor"() {
        given:
        def factor = repository.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", NOW))

        when:
        def disabled = service.disable("u-1", factor.id(), NOW.plusSeconds(5))

        then:
        disabled
        !repository.findById(factor.id()).orElseThrow().isActive()
    }

    def "a factor of another account is refused and left untouched"() {
        given:
        def foreign = repository.save(SecondFactor.enrolTotp("u-2", SECRET, "iPhone", NOW))

        when:
        def disabled = service.disable("u-1", foreign.id(), NOW.plusSeconds(5))

        then: "indistinguishable from 'no such factor', so ids cannot be probed"
        !disabled
        repository.findById(foreign.id()).orElseThrow().isActive()
    }

    def "an unknown factor id is refused"() {
        expect:
        !service.disable("u-1", 42L, NOW)
    }

    def "disabling twice reports false the second time"() {
        given:
        def factor = repository.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", NOW))
        service.disable("u-1", factor.id(), NOW)

        expect:
        !service.disable("u-1", factor.id(), NOW.plusSeconds(60))
    }

    def "missing arguments are refused"() {
        when:
        service.disable(null, 1L, NOW)

        then:
        thrown(NullPointerException)

        when:
        service.disable("u-1", 1L, null)

        then:
        thrown(NullPointerException)
    }

    static class FakeRepository implements SecondFactorRepository {

        final Map<Long, SecondFactor> byId = [:]
        long nextId = 1

        @Override
        SecondFactor save(SecondFactor factor) {
            if (factor.id() == null) {
                // SecondFactor.id() is JPA-owned; emulate the surrogate key for the fake.
                def field = SecondFactor.getDeclaredField("id")
                field.setAccessible(true)
                field.set(factor, nextId++)
            }
            byId[factor.id()] = factor
            return factor
        }

        @Override
        Optional<SecondFactor> findById(long id) {
            Optional.ofNullable(byId[id])
        }

        @Override
        List<SecondFactor> findAllByUserId(String userId) {
            byId.values().findAll { it.userId() == userId }
        }

        @Override
        List<SecondFactor> findActiveByUserId(String userId) {
            byId.values().findAll { it.userId() == userId && it.isActive() }
        }

        @Override
        boolean hasActiveFactor(String userId) {
            !findActiveByUserId(userId).isEmpty()
        }
    }
}
