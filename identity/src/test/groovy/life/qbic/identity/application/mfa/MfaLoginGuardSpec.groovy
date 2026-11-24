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
    /** The guard resolves the account through UserId, so the ids must be real UUIDs. */
    private static final String NEW_ACCOUNT = "3f1a7c2e-0b6d-4a51-9c8e-2d7f4b6a1c33"
    private static final String LEGACY_ACCOUNT = "8c2b5d19-4e77-4f02-b3aa-9d61c0e2f4a7"

    private static final ProtectedSecret SECRET =
            ProtectedSecret.ofBase64(Base64.encoder.encodeToString(new byte[32]))

    FakeFactors factors = new FakeFactors()
    FakeGraces graces = new FakeGraces()

    /** Every account is owed a window unless a test says otherwise (see "first login" below). */
    FakeEligibility eligibility = new FakeEligibility()

    private MfaLoginGuard guard(MfaPolicyMode mode) {
        new MfaLoginGuard(factors, graces, new MfaPolicy(mode.name(), GRACE, ""), eligibility)
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

    // ---------------------------------------------------------------- first login (registration path)

    def "an account created after the cut-off enrols at its first login instead of getting a window"() {
        given: "the posture was switched on before this account was created"
        eligibility.notEligible << NEW_ACCOUNT

        when:
        def decision = guard(MfaPolicyMode.REQUIRED).decide(NEW_ACCOUNT, NOW)

        then: "no countdown, and nothing written that could later expire"
        decision instanceof MfaGateDecision.EnrolmentDue
        decision.secondFactorRequired()
        !decision.loginMayProceedWithoutFactor()
        graces.starts == 0
        graces.stored.isEmpty()
    }

    def "a single login does not drift the eligibility, so the answer is the same every time"() {
        given:
        eligibility.notEligible << NEW_ACCOUNT

        expect: "an unenrolled new account is asked to enrol on each login, never granted a deadline"
        (0..3).every {
            guard(MfaPolicyMode.REQUIRED).decide(NEW_ACCOUNT, NOW.plus(Duration.ofDays(it)))
                    instanceof MfaGateDecision.EnrolmentDue
        }
        graces.starts == 0
    }

    def "an account that existed before the cut-off keeps the grace window"() {
        when:
        def decision = guard(MfaPolicyMode.REQUIRED).decide(LEGACY_ACCOUNT, NOW)

        then:
        decision instanceof MfaGateDecision.GraceGranted
        graces.starts == 1
    }

    def "a window granted to a new account is honoured while it runs"() {
        given: "an operator granted a window deliberately, to an account that is not owed one"
        eligibility.notEligible << NEW_ACCOUNT
        graces.stored[NEW_ACCOUNT] = MfaGrace.start(NEW_ACCOUNT, NOW, GRACE)

        when:
        def decision = guard(MfaPolicyMode.REQUIRED).decide(NEW_ACCOUNT, NOW.plus(Duration.ofHours(1)))

        then: "eligibility governs granting, not revoking"
        decision instanceof MfaGateDecision.GraceActive
    }

    def "an unreadable account record does not create a stricter path"() {
        given: "the eligibility question could not be answered, so the answer is the forgiving one"
        eligibility.failing = true

        when:
        def decision = guard(MfaPolicyMode.REQUIRED).decide(LEGACY_ACCOUNT, NOW)

        then: "a data gap is not evidence that the account is new"
        decision instanceof MfaGateDecision.GraceGranted
    }

    def "an enrolled new account is challenged like any other, cut-off or not"() {
        given:
        eligibility.notEligible << NEW_ACCOUNT
        factors.save(SecondFactor.enrolTotp(NEW_ACCOUNT, SECRET, "iPhone", NOW))

        expect:
        guard(MfaPolicyMode.REQUIRED).decide(NEW_ACCOUNT, NOW) instanceof MfaGateDecision.Challenge
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

    /** Answers the one question the guard asks; tests opt accounts out of the window explicitly. */
    static class FakeEligibility implements GraceEligibility {
        final Set<String> notEligible = [] as Set
        boolean failing = false

        @Override
        boolean isEligible(String userId) {
            if (failing) {
                // Mirrors UserGraceEligibility: an unreadable account is eligible, never stricter.
                return true
            }
            !notEligible.contains(userId)
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

        @Override
        List<life.qbic.identity.domain.model.mfa.MfaGrace> findActiveWithOutstandingReminder(
                Instant now) {
            stored.values().findAll { it.graceUntil().isAfter(now) }
        }

        @Override
        void markDueReminderSent(String userId, Instant now) {
            // Not modelled: these specs are about the login decision, not about the reminder job.
        }

        @Override
        void markFinalReminderSent(String userId, Instant now) {
            // Not modelled: see markDueReminderSent.
        }
    }
}
