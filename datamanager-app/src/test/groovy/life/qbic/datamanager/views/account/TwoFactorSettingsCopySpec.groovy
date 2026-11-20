package life.qbic.datamanager.views.account

import life.qbic.identity.domain.model.mfa.MfaPolicyMode
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The two-factor settings page may not claim anything the deployment does not do.</b>
 *
 * <p>Every sentence that page shows is computed from three inputs — the posture the application was
 * started with, whether the account has a factor, and how many unused recovery codes are left. The
 * first version computed them in three independent setters, and two of the resulting sentences were
 * false of the page they appeared on:</p>
 *
 * <ul>
 *   <li>an enrolled account under {@code OFF} was told the app "is required the next time you sign in"
 *       while the next line said two-factor authentication is "not required at sign-in" —
 *       {@code MfaGate} returns {@code NotRequired} for {@code OFF} regardless of the factor;</li>
 *   <li>an account with no factor under {@code OPTIONAL} was told "you will be asked for a code,
 *       because this account has set up a factor".</li>
 * </ul>
 *
 * <p>These tests are the guard: the copy is a pure function of its inputs, so the truth table can be
 * asserted instead of reviewed by eye on a deployment.</p>
 */
class TwoFactorSettingsCopySpec extends Specification {

    def "the factor summary never promises enforcement, whatever the account has"() {
        expect: "whether the app is asked for is the policy note's job — the summary states facts only"
        (0..3).every { count ->
            !TwoFactorSettingsCopy.factorSummary(count).toLowerCase().contains("required")
        }
    }

    @Unroll
    def "the factor summary counts what is there: #count -> '#expected'"() {
        expect:
        TwoFactorSettingsCopy.factorSummary(count) == expected

        where:
        count | expected
        0     | "No authenticator app is set up for this account yet."
        1     | "One authenticator app is set up."
        2     | "2 authenticator apps are set up. Any of them can be used to sign in."
    }

    def "an enrolled account under OFF is told it will not be asked, not that it is required"() {
        when:
        def note = TwoFactorSettingsCopy.policyNote(MfaPolicyMode.OFF, true)

        then: "MfaGate returns NotRequired for OFF even with a factor — the text has to match that"
        note.contains("turned off")
        note.contains("will not be asked")
        !note.contains("required")
    }

    def "an account with no factor under OPTIONAL is not promised a challenge"() {
        when:
        def note = TwoFactorSettingsCopy.policyNote(MfaPolicyMode.OPTIONAL, false)

        then: "nothing is asked: without a factor there is no challenge to ask for"
        !note.contains("will be asked")
        !note.contains("has set up a factor")
        note.contains("password alone")
    }

    def "an enrolled account under OPTIONAL is told a code will be asked for"() {
        expect:
        TwoFactorSettingsCopy.policyNote(MfaPolicyMode.OPTIONAL, true).contains("will be asked")
    }

    def "under REQUIRED an unenrolled account is told about the grace period"() {
        when:
        def note = TwoFactorSettingsCopy.policyNote(MfaPolicyMode.REQUIRED, false)

        then: "switching enforcement on must never read as an immediate lockout"
        note.contains("required at sign-in for every account")
        note.contains("grace period")
    }

    def "under REQUIRED an enrolled account needs no grace period sentence"() {
        when:
        def note = TwoFactorSettingsCopy.policyNote(MfaPolicyMode.REQUIRED, true)

        then:
        note.contains("required")
        !note.contains("grace period")
    }

    def "recovery codes are described as coming after the app while no factor exists"() {
        when:
        def summary = TwoFactorSettingsCopy.recoverySummary(0, false)

        then: "a generated set could never be redeemed, so the page must not invite generating one"
        summary.contains("once an authenticator app is set up")
        TwoFactorSettingsCopy.recoveryActionLabel(0, false) == "Generate recovery codes"
    }

    @Unroll
    def "the recovery summary counts what is left: #codes codes with a factor"() {
        expect:
        TwoFactorSettingsCopy.recoverySummary(codes, true) == expected

        where:
        codes | expected
        0L    | "No unused recovery codes. Losing your phone without them means an administrator has to re-enrol you."
        1L    | "1 unused recovery code left. Generating a new set stops it working."
        10L   | "10 unused recovery codes left. Generating a new set stops the current codes working."
    }

    def "with a factor and codes left, the action offers a new set"() {
        expect:
        TwoFactorSettingsCopy.recoveryActionLabel(10, true) == "Generate a new set"
    }

    def "with a factor and no codes left, the action offers to generate a first set"() {
        expect:
        TwoFactorSettingsCopy.recoveryActionLabel(0, true) == "Generate recovery codes"
    }

    @Unroll
    def "no sentence advertises a reset by e-mail or by support (#mode, factor=#hasFactor)"() {
        given: "accounts are self-service and e-mail verified, so mailbox control must never be the way"
        def sentences = [
                TwoFactorSettingsCopy.factorSummary(1),
                TwoFactorSettingsCopy.policyNote(mode, hasFactor),
                TwoFactorSettingsCopy.recoverySummary(0, hasFactor),
                TwoFactorSettingsCopy.recoverySummary(3, hasFactor),
        ]

        expect: "the only help the copy names is re-enrolment by an administrator"
        sentences.every { sentence ->
            def lower = sentence.toLowerCase()
            !lower.contains("e-mail") && !lower.contains("email") && !lower.contains("support")
        }

        where:
        mode                      | hasFactor
        MfaPolicyMode.OFF         | false
        MfaPolicyMode.OFF         | true
        MfaPolicyMode.OPTIONAL    | false
        MfaPolicyMode.OPTIONAL    | true
        MfaPolicyMode.REQUIRED    | false
        MfaPolicyMode.REQUIRED    | true
    }
}
