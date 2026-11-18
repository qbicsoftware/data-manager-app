package life.qbic.identity.application.mfa

import life.qbic.identity.domain.model.mfa.MfaGateDecision
import life.qbic.identity.domain.model.mfa.MfaGrace
import life.qbic.identity.domain.model.mfa.MfaPolicyMode
import life.qbic.identity.domain.model.mfa.ProtectedSecret
import life.qbic.identity.domain.model.mfa.SecondFactor
import life.qbic.identity.domain.repository.MfaGraceRepository
import life.qbic.identity.domain.repository.SecondFactorRepository
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>What the login path does after the password is accepted.</b>
 *
 * <p>The interesting behaviour is not the decision (that is {@code MfaGateSpec}) but the write: a
 * granted window must be persisted exactly once, and an expired one must never be renewed by logging
 * in again.</p>
 */
class MfaLoginGuardSpec extends Specification {

    private static final Instant NOW = Instant.parse("2026-11-18T09:00:00Z")
    private static final Duration GRACE = Duration.ofHours(48)
    private static final ProtectedSecret SECRET =
            ProtectedSecret.ofBase64(Base64.encoder.encodeToString(new byte[32]))

    FakeFactors factors = new FakeFactors()
    FakeGraces graces = new FakeGraces()

    private MfaLoginGuard guard(MfaPolicyMode mode) {
        new MfaLoginGuard(factors, graces, new MfaPolicy(mode.name(), GRACE))
    }

    // ---------------------------------------------------------------- the decision is delegated

    @Unroll
    def "#mode with enrolled=#enrolled asks for a factor: #expected"() {
        given:
        if (enrolled) {
            factors.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", NOW))
        }

        expect:
        guard(mode).decide("u-1", NOW).getClass() == expected

        where:
        mode                     | enrolled | expected
        MfaPolicyMode.OFF        | true     | MfaGateDecision.NotRequired
        MfaPolicyMode.OFF        | false    | MfaGateDecision.NotRequired
        MfaPolicyMode.OPTIONAL   | true     | MfaGateDecision.Challenge
        MfaPolicyMode.OPTIONAL   | false    | MfaGateDecision.NotRequired
        MfaPolicyMode.REQUIRED   | true     | MfaGateDecision.Challenge
    }

    // ---------------------------------------------------------------- grace persistence

    def "REQUIRED without a factor grants a window and persists it"() {
        when:
        def decision = guard(MfaPolicyMode.REQUIRED).decide("u-1", NOW)

        then:
        decision instanceof MfaGateDecision.GraceGranted
        (decision as MfaGateDecision.GraceGranted).graceUntil() == NOW.plus(GRACE)

        and: "stored once, so the next login resolves to GraceActive instead of granting again"
        graces.stored.size() == 1
        graces.stored["u-1"].graceUntil() == NOW.plus(GRACE)
        graces.stored["u-1"].startedAt() == NOW
    }

    def "a second login inside the window does not move the deadline"() {
        given:
        guard(MfaPolicyMode.REQUIRED).decide("u-1", NOW)

        when: "the user logs in a day later"
        def decision = guard(MfaPolicyMode.REQUIRED).decide("u-1", NOW.plus(Duration.ofDays(1)))

        then: "no second write, and the deadline is unchanged"
        graces.starts == 1
        decision instanceof MfaGateDecision.GraceActive
        (decision as MfaGateDecision.GraceActive).graceUntil() == NOW.plus(GRACE)
    }

    def "an expired window denies and is not extended by logging in"() {
        given:
        graces.stored["u-1"] = MfaGrace.start("u-1", NOW.minus(Duration.ofDays(10)), GRACE)

        when:
        def decision = guard(MfaPolicyMode.REQUIRED).decide("u-1", NOW)

        then:
        decision instanceof MfaGateDecision.GraceExpired
        graces.starts == 0
    }

    def "enrolling ends the grace discussion, even inside the window"() {
        given:
        guard(MfaPolicyMode.REQUIRED).decide("u-1", NOW)
        factors.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", NOW))

        expect:
        guard(MfaPolicyMode.REQUIRED).decide("u-1", NOW.plusSeconds(60))
                instanceof MfaGateDecision.Challenge
    }

    def "OFF never writes a grace row"() {
        when:
        guard(MfaPolicyMode.OFF).decide("u-1", NOW)

        then:
        graces.starts == 0
        graces.stored.isEmpty()
    }

    def "a missing argument is refused"() {
        when:
        guard(MfaPolicyMode.REQUIRED).decide(null, NOW)

        then:
        thrown(NullPointerException)

        when:
        guard(MfaPolicyMode.REQUIRED).decide("u-1", null)

        then:
        thrown(NullPointerException)
    }

    // ---------------------------------------------------------------- fakes

    static class FakeFactors implements SecondFactorRepository {
        final List<SecondFactor> saved = []

        @Override
        SecondFactor save(SecondFactor factor) {
            saved << factor
            return factor
        }

        @Override
        Optional<SecondFactor> findById(long id) {
            Optional.empty()
        }

        @Override
        List<SecondFactor> findAllByUserId(String userId) {
            saved.findAll { it.userId() == userId }
        }

        @Override
        List<SecondFactor> findActiveByUserId(String userId) {
            saved.findAll { it.userId() == userId && it.isActive() }
        }

        @Override
        boolean hasActiveFactor(String userId) {
            !findActiveByUserId(userId).isEmpty()
        }
    }

    static class FakeGraces implements MfaGraceRepository {
        final Map<String, MfaGrace> stored = [:]
        int starts = 0

        @Override
        Optional<MfaGrace> findByUserId(String userId) {
            Optional.ofNullable(stored[userId])
        }

        @Override
        MfaGrace startIfAbsent(MfaGrace candidate) {
            starts++
            if (!stored.containsKey(candidate.userId())) {
                stored[candidate.userId()] = candidate
            }
            return stored[candidate.userId()]
        }
    }
}
