package life.qbic.identity.domain.model.mfa

import spock.lang.Specification

/**
 * <b>The provisioning URI the QR code encodes</b>
 *
 * <p>A URI whose parameters do not match what the verifier expects is the classic reason a freshly
 * enrolled factor never works, so the parameters are pinned here: same issuer, same digits, same
 * period, same algorithm as {@link Totp}.</p>
 */
class OtpauthUriSpec extends Specification {

    private static final String SEED = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

    def "the URI carries the seed and the parameters the verifier uses"() {
        when:
        def uri = OtpauthUri.forTotp("ada@example.org", SEED)

        then:
        uri.startsWith("otpauth://totp/QBiC%20Data%20Manager:ada%40example.org?")
        uri.contains("secret=" + SEED)
        uri.contains("issuer=QBiC%20Data%20Manager")
        uri.contains("algorithm=SHA1")
        uri.contains("digits=" + Totp.DIGITS)
        uri.contains("period=" + Totp.PERIOD_SECONDS)
    }

    def "the label separator is a literal colon, not %3A"() {
        when:
        def uri = OtpauthUri.forTotp("ada@example.org", SEED)

        then: "an app that splits the label before decoding it must still find the issuer"
        uri.contains("QBiC%20Data%20Manager:ada%40example.org")
        !uri.contains("%3A")
    }

    def "the issuer is spelled the way the product is spelled"() {
        expect: "the entry in the authenticator app is the only place a user sees this name"
        OtpauthUri.ISSUER == "QBiC Data Manager"
    }

    def "the issuer is percent-encoded with %20, never a plus sign"() {
        expect: "form encoding would render literally as 'QBIC+Data+Manager' in some apps"
        !OtpauthUri.forTotp("ada@example.org", SEED).contains("+")
    }

    def "a label with a space is encoded, so the account name survives the scan"() {
        expect:
        OtpauthUri.forTotp("ada lovelace", SEED)
                .startsWith("otpauth://totp/QBiC%20Data%20Manager:ada%20lovelace?")
    }

    def "blank input is refused"() {
        when:
        OtpauthUri.forTotp(label, seed)

        then:
        thrown(IllegalArgumentException)

        where:
        label             | seed
        " "               | SEED
        ""                | SEED
        "ada@example.org" | " "
    }

    def "missing input is refused"() {
        when:
        OtpauthUri.forTotp(null, SEED)

        then:
        thrown(NullPointerException)
    }
}
