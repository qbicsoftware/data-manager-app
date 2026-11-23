package life.qbic.datamanager.views.login

import life.qbic.datamanager.security.mfa.MfaLoginSuccessHandler
import spock.lang.Specification

/**
 * <b>The page an account lands on when it has no factor and is owed no deadline.</b>
 *
 * <p>Two of these assertions are about wiring that fails silently rather than loudly, which is why
 * they are here and not left to a visual check: a redirect to a route that does not exist is a 404
 * immediately after a correct password, and a page that reads the setup key differently from the
 * settings dialog sends the user into the app with a seed that does not match the QR code.</p>
 */
class RequiredEnrolmentViewSpec extends Specification {

    def "the login path and the route agree on where enrolment happens"() {
        expect: "the filter redirects to a constant; the view declares a route; nothing checks that"
        RequiredEnrolmentView.PATH == MfaLoginSuccessHandler.ENROL_PATH
    }

    def "the route is under /login, so it is reachable while no session may exist"() {
        expect:
        RequiredEnrolmentView.PATH.startsWith("/login/")
    }

    def "the setup key is read out of the provisioning URI"() {
        given:
        def uri = "otpauth://totp/QBiC%20Data%20Manager:ada@example.org" +
                "?secret=YBB4ASKALBVE2NAS6UQLDAQOD2QG2UMD&issuer=QBiC%20Data%20Manager" +
                "&algorithm=SHA1&digits=6&period=30"

        expect:
        RequiredEnrolmentView.setupKeyFrom(uri) == "YBB4ASKALBVE2NAS6UQLDAQOD2QG2UMD"
    }

    def "a URI without a secret is refused rather than rendered as an empty key"() {
        when:
        RequiredEnrolmentView.setupKeyFrom("otpauth://totp/QBiC:ada@example.org?issuer=QBiC")

        then: "an empty copyable field would be worse than an error: the user would type nothing"
        thrown(IllegalArgumentException)
    }

    def "the factor carries a label that reads in a list of several devices"() {
        expect:
        !RequiredEnrolmentView.DISPLAY_NAME.isBlank()
        RequiredEnrolmentView.DISPLAY_NAME.length() < 40
    }
}
