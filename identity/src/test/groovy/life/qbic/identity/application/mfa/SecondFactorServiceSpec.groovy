package life.qbic.identity.application.mfa

import life.qbic.identity.domain.model.mfa.MfaPolicyMode
import life.qbic.identity.domain.model.mfa.ProtectedSecret
import life.qbic.identity.domain.model.mfa.SecondFactor
import life.qbic.identity.domain.model.mfa.SecondFactorRemoval.Outcome
import life.qbic.identity.domain.repository.RecoveryCodeRepository
import life.qbic.identity.domain.repository.SecondFactorRepository
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

/**
 * <b>Ownership, and the last factor (AUTH-R-02 / AUTH-R-03).</b>
 *
 * <p>A factor id arrives from the UI. "It exists" must never be enough to remove it, and a factor of
 * another account must be indistinguishable from one that does not exist — otherwise the settings page
 * becomes a cross-account existence oracle.</p>
 *
 * <p>The second half of this spec is the rule that was missing until 2026-11-20: an account must not
 * drop below one active factor by accident. Under {@code REQUIRED} the last factor is protected;
 * under {@code OPTIONAL} removing it is the explicit "turn two-factor authentication off" action,
 * which also retires the recovery codes.</p>
 */
class SecondFactorServiceSpec extends Specification {

    private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z")
    private static final ProtectedSecret SECRET =
            ProtectedSecret.ofBase64(Base64.encoder.encodeToString(new byte[32]))

    FakeRepository repository
    FakeRecoveryCodes recoveryCodes
    SecondFactorService service

    def setup() {
        repository = new FakeRepository()
        recoveryCodes = new FakeRecoveryCodes()
        service = serviceFor(MfaPolicyMode.OPTIONAL)
    }

    private SecondFactorService serviceFor(MfaPolicyMode mode) {
        new SecondFactorService(repository, recoveryCodes, new MfaPolicy(mode.name(), MfaPolicy.DEFAULT_GRACE))
    }

    // ---------------------------------------------------------------- reads + ownership

    def "active factors of the account are listed, disabled ones are not"() {
        given:
        repository.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", NOW))
        def removed = repository.save(SecondFactor.enrolTotp("u-1", SECRET, "Old phone", NOW))
        removed.disable(NOW.plusSeconds(1))
        repository.save(SecondFactor.enrolTotp("u-2", SECRET, "Someone else", NOW))

        expect:
        service.activeFactors("u-1")*.displayName() == ["iPhone"]
    }

    def "the service reports the posture it was started with"() {
        expect:
        serviceFor(MfaPolicyMode.REQUIRED).policyMode() == MfaPolicyMode.REQUIRED
    }

    def "a factor of another account is refused and left untouched"() {
        given:
        def foreign = repository.save(SecondFactor.enrolTotp("u-2", SECRET, "iPhone", NOW))

        when:
        def outcome = service.remove("u-1", foreign.id(), NOW.plusSeconds(5))

        then: "indistinguishable from 'no such factor', so ids cannot be probed"
        outcome == Outcome.NOT_REMOVABLE
        repository.findById(foreign.id()).orElseThrow().isActive()
    }

    def "an unknown factor id is refused"() {
        expect:
        service.remove("u-1", 42L, NOW) == Outcome.NOT_REMOVABLE
    }

    def "removing twice reports NOT_REMOVABLE the second time"() {
        given:
        def factor = repository.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", NOW))
        repository.save(SecondFactor.enrolTotp("u-1", SECRET, "iPad", NOW))
        service.remove("u-1", factor.id(), NOW)

        expect: "a removal that did not happen must not be reported as one"
        service.remove("u-1", factor.id(), NOW.plusSeconds(60)) == Outcome.NOT_REMOVABLE
    }

    def "missing arguments are refused"() {
        when:
        service.remove(null, 1L, NOW)

        then:
        thrown(NullPointerException)

        when:
        service.remove("u-1", 1L, null)

        then:
        thrown(NullPointerException)
    }

    // ---------------------------------------------------------------- the last-factor rule

    @Unroll
    def "mode #mode with #activeCount factor(s) decides #expected"() {
        given:
        def factors = (1..activeCount).collect {
            repository.save(SecondFactor.enrolTotp("u-1", SECRET, "Phone " + it, NOW))
        }
        def target = factors.first().id()
        def subject = serviceFor(mode)

        expect: "asking and doing use the same decision, so the button and the call cannot disagree"
        subject.removalOutcome("u-1", target) == expected
        subject.remove("u-1", target, NOW.plusSeconds(5)) == expected

        where:
        mode                      | activeCount | expected
        MfaPolicyMode.OFF         | 1           | Outcome.ALLOWED
        MfaPolicyMode.OFF         | 2           | Outcome.ALLOWED
        MfaPolicyMode.OPTIONAL    | 2           | Outcome.ALLOWED
        MfaPolicyMode.OPTIONAL    | 1           | Outcome.TURNS_OFF_TWO_FACTOR
        MfaPolicyMode.REQUIRED    | 2           | Outcome.ALLOWED
        MfaPolicyMode.REQUIRED    | 1           | Outcome.LAST_FACTOR_PROTECTED
    }

    def "under REQUIRED the last factor survives the attempt"() {
        given: "the account is enrolled with exactly one factor under REQUIRED"
        def only = repository.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", NOW))
        def subject = serviceFor(MfaPolicyMode.REQUIRED)

        when:
        subject.remove("u-1", only.id(), NOW.plusSeconds(5))

        then: "nothing was disabled — the account cannot be left without a second factor"
        repository.findById(only.id()).orElseThrow().isActive()
        !recoveryCodes.invalidatedFor
    }

    def "under OPTIONAL the last factor can go, and the recovery codes go with it"() {
        given: "codes that only mean something while a factor exists"
        def only = repository.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", NOW))
        def subject = serviceFor(MfaPolicyMode.OPTIONAL)

        when:
        subject.remove("u-1", only.id(), NOW.plusSeconds(5))

        then: "a live code behind an account with no factor would be an invisible second way in"
        recoveryCodes.invalidatedFor == ["u-1"]
        !repository.findById(only.id()).orElseThrow().isActive()
    }

    def "removing a factor while another one remains leaves the recovery codes alone"() {
        given:
        def first = repository.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", NOW))
        repository.save(SecondFactor.enrolTotp("u-1", SECRET, "iPad", NOW))

        when:
        service.remove("u-1", first.id(), NOW.plusSeconds(5))

        then: "two-factor authentication is still on, so the break-glass path must stay"
        !recoveryCodes.invalidatedFor
    }

    def "the deprecated disable() still reports whether anything was removed"() {
        given:
        def first = repository.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", NOW))
        def second = repository.save(SecondFactor.enrolTotp("u-1", SECRET, "iPad", NOW))

        expect:
        service.disable("u-1", first.id(), NOW)
        service.disable("u-1", first.id(), NOW.plusSeconds(5)) == false
        second.isActive()
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

    /** Only the methods the service uses are implemented; the rest would be untested surface. */
    static class FakeRecoveryCodes implements RecoveryCodeRepository {

        final List<String> invalidatedFor = []

        @Override
        int invalidateAllFor(String userId, Instant now) {
            invalidatedFor << userId
            return 10
        }

        @Override
        List<life.qbic.identity.domain.model.mfa.RecoveryCode> saveAll(List codes) {
            throw new UnsupportedOperationException("not used by this spec")
        }

        @Override
        List<life.qbic.identity.domain.model.mfa.RecoveryCode> findAllByUserId(String userId) {
            throw new UnsupportedOperationException("not used by this spec")
        }

        @Override
        long countUsableByUserId(String userId) {
            throw new UnsupportedOperationException("not used by this spec")
        }

        @Override
        Optional<life.qbic.identity.domain.model.mfa.RecoveryCode> consumeIfUsable(
                String codeHash, Instant now) {
            throw new UnsupportedOperationException("not used by this spec")
        }

        @Override
        Optional<life.qbic.identity.domain.model.mfa.RecoveryCode> consumeIfUsableForUser(
                String codeHash, String userId, Instant now) {
            throw new UnsupportedOperationException("not used by this spec")
        }
    }
}
