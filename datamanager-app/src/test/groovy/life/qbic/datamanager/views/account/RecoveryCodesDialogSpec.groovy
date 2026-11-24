package life.qbic.datamanager.views.account

import life.qbic.identity.application.mfa.RecoveryCodeSet
import spock.lang.Specification

import java.time.Instant

/**
 * <b>The one-time recovery-code sheet, as text.</b>
 *
 * <p>These codes are the only reset-free way back into an account, so the sheet that leaves the
 * application — copied or downloaded — is a security artefact, not a convenience view. The tests
 * pin the properties that matter: every code appears exactly once, the printed (grouped) form is
 * the form on the sheet, the account and the UTC timestamp are on it, and the file is plain ASCII
 * so it prints and opens the same everywhere.</p>
 */
class RecoveryCodesDialogSpec extends Specification {

    private static final String EMAIL = "ada@example.org"

    /** 2026-11-19T14:32:00Z — a fixed instant, so the assertions are not date-dependent. */
    private static final Instant GENERATED_AT = Instant.parse("2026-11-19T14:32:00Z")

    private static final List<String> CODES =
            "ABCDEFGHIJ".toList().collect { prefix -> prefix + "2B3C4D5E6F7" }

    private static RecoveryCodeSet codeSet() {
        return new RecoveryCodeSet("user-1", CODES, GENERATED_AT)
    }

    def "every code appears exactly once, in its printed form"() {
        given:
        def sheet = RecoveryCodesDialog.codesAsText(codeSet(), EMAIL)

        expect: "each grouping is on the sheet once..."
        codeSet().formattedCodes().every { code -> sheet.count(code) == 1 }

        and: "...and the ungrouped secret is not, so a copied sheet cannot be mistaken for a raw list"
        CODES.every { raw -> !sheet.contains(raw) }
    }

    def "the sheet names the account and the generation time in UTC"() {
        expect:
        RecoveryCodesDialog.codesAsText(codeSet(), EMAIL).contains("Account: " + EMAIL)
        RecoveryCodesDialog.codesAsText(codeSet(), EMAIL).contains("Generated: 2026-11-19 14:32 UTC")
    }

    def "the sheet says that the file itself is sensitive and that codes are single use"() {
        given:
        def sheet = RecoveryCodesDialog.codesAsText(codeSet(), EMAIL)

        expect: "the two facts a user must act on are stated, not implied"
        sheet.contains("works once")
        sheet.contains("Anyone who has a code can sign in")
        sheet.contains("delete it once the codes are stored")
    }

    def "the sheet is plain ASCII with Unix line endings, so it prints and opens everywhere"() {
        given:
        def sheet = RecoveryCodesDialog.codesAsText(codeSet(), EMAIL)

        expect:
        sheet.chars().allMatch { int character -> character < 128 }
        !sheet.contains("\r")
    }

    def "the file name is dated, so a second set does not silently overwrite a kept first one"() {
        expect:
        RecoveryCodesDialog.fileName(codeSet()) == "data-manager-recovery-codes-2026-11-19.txt"
    }

    def "a null code set or account is refused instead of rendering an empty sheet"() {
        when:
        RecoveryCodesDialog.codesAsText(null, EMAIL)

        then:
        thrown(NullPointerException)

        when:
        RecoveryCodesDialog.codesAsText(codeSet(), null)

        then:
        thrown(NullPointerException)
    }

    def "the first-factor warning states the lockout, the in-person check, and that e-mail is not a way in"() {
        when:
        def warning = RecoveryCodesDialog.firstFactorLockoutWarning()
        def lower = warning.toLowerCase()

        then: "losing both means no self-service way in"
        lower.contains("cannot sign in by yourself")

        and: "the helpdesk helps, but only after verifying in person"
        lower.contains("in person")

        and: "no e-mail link skips the step — the constraint the whole milestone rests on"
        lower.contains("no e-mail link")
        lower.contains("skips the second step")

        and: "it must not read as 'support will reset it for you'"
        !lower.contains("reset")
    }
}
