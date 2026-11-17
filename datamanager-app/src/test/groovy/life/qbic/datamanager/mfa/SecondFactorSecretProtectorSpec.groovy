package life.qbic.datamanager.mfa

import life.qbic.datamanager.security.mfa.AesGcmSecondFactorSecretProtector
import life.qbic.identity.application.mfa.SecondFactorSecretUnavailableException
import life.qbic.identity.domain.model.mfa.ProtectedSecret
import spock.lang.Specification
import spock.lang.Unroll

import javax.crypto.spec.SecretKeySpec

/**
 * <b>Seed encryption at rest (AUTH-NFR-01, ADR 0011 F1).</b>
 *
 * <p>The one property that matters: what reaches the database is ciphertext that
 * {@link ProtectedSecret} accepts, and a seed that cannot be decrypted fails closed instead of turning
 * into "no factor enrolled".</p>
 *
 * <p>Uses the production AES-256-GCM encryptor with a fixed test key — the vault is what supplies the
 * key, not what performs the cryptography, so a real key is all this needs.</p>
 */
class SecondFactorSecretProtectorSpec extends Specification {

    private static final byte[] KEY = (0..31).collect { (byte) it } as byte[]
    private static final String SEED = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

    private static AesGcmSecondFactorSecretProtector protector(byte[] key = KEY) {
        new AesGcmSecondFactorSecretProtector(
                { new SecretKeySpec(key, "AES") } as java.util.function.Supplier,
                "second-factor-master-key")
    }

    def "a seed round-trips"() {
        when:
        def protectedSeed = protector().protect(SEED.toCharArray())

        then:
        new String(protector().reveal(protectedSeed)) == SEED
    }

    def "what is stored is a ProtectedSecret-shaped blob, not the seed"() {
        when:
        def protectedSeed = protector().protect(SEED.toCharArray())

        then: "the domain type would refuse it otherwise, which is the whole guard against plaintext"
        ProtectedSecret.ofBase64(protectedSeed).encoded() == protectedSeed
        !protectedSeed.contains(SEED)
    }

    def "the same seed encrypts to a different blob every time"() {
        given:
        def first = protector().protect(SEED.toCharArray())
        def second = protector().protect(SEED.toCharArray())

        expect: "a fresh nonce per call — reuse would be a GCM key-stream catastrophe"
        first != second
        new String(protector().reveal(first)) == SEED
        new String(protector().reveal(second)) == SEED
    }

    def "a tampered blob is refused rather than decrypted into garbage"() {
        given:
        def protectedSeed = protector().protect(SEED.toCharArray())
        def bytes = Base64.decoder.decode(protectedSeed)
        bytes[bytes.length - 1] = (byte) (bytes[bytes.length - 1] ^ 0x01)

        when:
        protector().reveal(Base64.encoder.encodeToString(bytes))

        then: "the GCM tag fails, and that must never be reported as 'no factor'"
        thrown(SecondFactorSecretUnavailableException)
    }

    def "a different master key cannot decrypt"() {
        given:
        def protectedSeed = protector().protect(SEED.toCharArray())
        def otherKey = (32..63).collect { (byte) it } as byte[]

        when: "the vault key was rotated or restored from the wrong backup"
        protector(otherKey).reveal(protectedSeed)

        then:
        thrown(SecondFactorSecretUnavailableException)
    }

    @Unroll
    def "a stored value that is not ciphertext fails closed: #description"() {
        when:
        protector().reveal(value)

        then:
        thrown(SecondFactorSecretUnavailableException)

        where:
        value                     | description
        "not base64 at all"       | "garbage"
        Base64.encoder.encodeToString(new byte[4]) | "too short to hold a nonce and a tag"
    }

    def "a missing vault entry is reported as an unavailable factor, not as a startup failure"() {
        given: "the alias was never provisioned in this deployment"
        def withoutKey = new AesGcmSecondFactorSecretProtector(
                { throw new IllegalStateException("Vault entry not found") } as java.util.function.Supplier,
                "second-factor-master-key")

        when:
        withoutKey.protect(SEED.toCharArray())

        then: "the operation fails closed; the application itself keeps running (M4-1 makes it eager)"
        def e = thrown(SecondFactorSecretUnavailableException)
        e.message.contains("second-factor-master-key")
    }

    def "a key of the wrong size is refused"() {
        when:
        protector((1..16).collect { (byte) it } as byte[]).protect(SEED.toCharArray())

        then:
        thrown(SecondFactorSecretUnavailableException)
    }
}
