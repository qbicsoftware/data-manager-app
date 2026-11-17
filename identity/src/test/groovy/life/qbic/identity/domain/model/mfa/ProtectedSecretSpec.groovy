package life.qbic.identity.domain.model.mfa

import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The gate that stops a plaintext seed from being stored (AUTH-NFR-01, ADR 0011 F1/F2).</b>
 *
 * <p>The domain cannot encrypt, so it cannot prove confidentiality. What it can do is refuse anything
 * that is not shaped like AES-GCM ciphertext — which is what a seed that was never encrypted looks
 * like, including the most likely implementation mistake: passing the raw TOTP seed through because
 * the encryptor was not wired yet.</p>
 */
class ProtectedSecretSpec extends Specification {

    // 12-byte nonce + 16-byte tag
    private static String blob(int payloadBytes) {
        Base64.encoder.encodeToString(new byte[28 + payloadBytes])
    }

    def "an AES-GCM blob is accepted and returned byte-for-byte"() {
        given:
        def encoded = blob(16)

        when:
        def secret = ProtectedSecret.ofBase64(encoded)

        then:
        secret.encoded() == encoded
    }

    def "surrounding whitespace is stripped"() {
        expect:
        ProtectedSecret.ofBase64("  ${blob(1)}  ").encoded() == blob(1)
    }

    @Unroll
    def "a value that is not ciphertext is refused: #description"() {
        when:
        ProtectedSecret.ofBase64(value)

        then:
        thrown(expected)

        where:
        expected                  | value                                       | description
        NullPointerException      | null                                        | "null"
        IllegalArgumentException  | ""                                          | "empty"
        IllegalArgumentException  | "   "                                       | "blank"
        IllegalArgumentException  | "JBSWY3DPEHPK3PXP"                          | "a raw base32 seed"
        IllegalArgumentException  | "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP"          | "a longer raw base32 seed"
        IllegalArgumentException  | "not a blob, and not base64 either"         | "arbitrary text"
        IllegalArgumentException  | Base64.encoder.encodeToString(new byte[27]) | "one byte short of nonce plus tag"
    }

    def "a secret never prints its material"() {
        expect: "this value reaches logs and exception messages"
        ProtectedSecret.ofBase64(blob(1)).toString() == "ProtectedSecret[redacted]"
    }

    def "equality is by stored value, so a factor round-trip compares equal"() {
        expect:
        ProtectedSecret.ofBase64(blob(1)) == ProtectedSecret.ofBase64(blob(1))
        ProtectedSecret.ofBase64(blob(1)) != ProtectedSecret.ofBase64(blob(2))
        ProtectedSecret.ofBase64(blob(1)).hashCode() == ProtectedSecret.ofBase64(blob(1)).hashCode()
    }
}
