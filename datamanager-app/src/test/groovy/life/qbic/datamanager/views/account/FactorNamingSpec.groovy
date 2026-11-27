package life.qbic.datamanager.views.account

import life.qbic.identity.domain.model.mfa.SecondFactor
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>Naming a factor: the label is optional to type, never optional to have.</b>
 *
 * <p>One failure mode is worth a spec of its own. Clearing the optional name field must not produce an
 * empty label: the domain refuses blank, so an empty submission would fail <em>after</em> the code was
 * accepted and the QR code scanned — the user would have to start over to learn that a label is required.
 * An empty field therefore means the default.</p>
 *
 * <p>The rule lives in {@code TwoFactorSettingsCopy.factorName}, a pure function, so it can be asserted
 * directly instead of by constructing a Vaadin dialog and reflecting into its fields.</p>
 */
class FactorNamingSpec extends Specification {

    def "an empty or blank name falls back to the default"() {
        expect:
        TwoFactorSettingsCopy.factorName(typed) == TwoFactorSettingsCopy.DEFAULT_FACTOR_NAME

        where:
        typed << [null, "", "   ", "\t\n", " \u00A0 "]
    }

    @Unroll
    def "a typed name is used as it is, stripped: '#typed'"() {
        expect:
        TwoFactorSettingsCopy.factorName(typed) == expected

        where:
        typed                | expected
        "iPhone (1Password)" | "iPhone (1Password)"
        "  Work phone  "     | "Work phone"
        "Yubi"               | "Yubi"
        "Mein Handy \u2013 Arbeit" | "Mein Handy \u2013 Arbeit"
    }

    def "the default is the label every factor carries until the user says otherwise"() {
        expect: "changing this string changes what existing accounts show in a dialog, so it is pinned"
        TwoFactorSettingsCopy.DEFAULT_FACTOR_NAME == "Authenticator app"
    }

    def "the field cannot exceed the column the label is stored in"() {
        expect: "a form allowing more than varchar(255) fails on submit, after the QR code was scanned"
        SecondFactor.MAX_DISPLAY_NAME_LENGTH == 255
    }

    def "the dialog's default is the shared constant, not a second copy of the string"() {
        expect: "two literals drift; one reference cannot"
        TwoFactorEnrolmentMain.DEFAULT_DISPLAY_NAME == TwoFactorSettingsCopy.DEFAULT_FACTOR_NAME
    }
}
