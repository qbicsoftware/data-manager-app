package life.qbic.projectmanagement.infrastructure.external

import spock.lang.Specification

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

/**
 * Unit tests for {@link AesGcmCredentialEncryptor}.
 *
 * @since 1.12.0
 */
class AesGcmCredentialEncryptorSpec extends Specification {

    /** 32-byte raw master key — simulates what ops stores in the PKCS12 vault */
    static final SecretKey MASTER_KEY = new SecretKeySpec(
        "aaaa1111bbbb2222cccc3333dddd4444".getBytes(StandardCharsets.UTF_8),
        "AES")

    def "encrypt then decrypt roundtrip preserves plaintext"() {
        given:
        def encryptor = buildEncryptor(MASTER_KEY)
        def plaintext = "my-secret-invenio-token-abc123".toCharArray()

        when:
        def encrypted = encryptor.encrypt(plaintext)
        def decrypted = encryptor.decrypt(encrypted)

        then:
        decrypted == plaintext
    }

    def "different encryptions of the same plaintext produce different ciphertexts"() {
        given:
        def encryptor = buildEncryptor(MASTER_KEY)
        def plaintext = "same-token-every-time".toCharArray()

        when:
        def encrypted1 = encryptor.encrypt(plaintext)
        def encrypted2 = encryptor.encrypt(plaintext)

        then: "nonce is random each time, so ciphertexts differ"
        encrypted1 != encrypted2

        and: "but both decrypt to the same plaintext"
        encryptor.decrypt(encrypted1) == plaintext
        encryptor.decrypt(encrypted2) == plaintext
    }

    def "encrypt then decrypt works with empty token"() {
        given:
        def encryptor = buildEncryptor(MASTER_KEY)
        def plaintext = "".toCharArray()

        when:
        def encrypted = encryptor.encrypt(plaintext)
        def decrypted = encryptor.decrypt(encrypted)

        then:
        decrypted == plaintext
    }

    def "encrypt then decrypt works with unicode characters"() {
        given:
        def encryptor = buildEncryptor(MASTER_KEY)
        def plaintext = "tökén-ünïcödë-日本語".toCharArray()

        when:
        def encrypted = encryptor.encrypt(plaintext)
        def decrypted = encryptor.decrypt(encrypted)

        then:
        decrypted == plaintext
    }

    def "decryption with wrong key fails"() {
        given:
        def encryptor1 = buildEncryptor(MASTER_KEY)
        def wrongKey = new SecretKeySpec(
            "zzzz9999yyyy8888xxxx7777wwww6666".getBytes(StandardCharsets.UTF_8),
            "AES")
        def encryptor2 = buildEncryptor(wrongKey)
        def plaintext = "test-token".toCharArray()

        when:
        def encrypted = encryptor1.encrypt(plaintext)
        encryptor2.decrypt(encrypted)

        then:
        thrown(AesGcmCredentialEncryptor.ExternalCredentialEncryptorException)
    }

    def "decryption of corrupted data throws meaningful exception"() {
        given:
        def encryptor = buildEncryptor(MASTER_KEY)

        when:
        def encrypted = encryptor.encrypt("token".toCharArray())
        encrypted[15] = (byte) (encrypted[15] ^ 0xFF)
        encryptor.decrypt(encrypted)

        then:
        def e = thrown(AesGcmCredentialEncryptor.ExternalCredentialEncryptorException)
        e.message.contains("Decryption failed")
    }

    def "encrypted data shorter than nonce plus tag fails fast"() {
        given:
        def encryptor = buildEncryptor(MASTER_KEY)

        when:
        encryptor.decrypt(new byte[10])

        then:
        def e = thrown(AesGcmCredentialEncryptor.ExternalCredentialEncryptorException)
        e.message.contains("too short")
    }

    def "encrypt output does not contain plaintext as readable text"() {
        given:
        def encryptor = buildEncryptor(MASTER_KEY)
        def plaintext = "secret-token-value".toCharArray()

        when:
        def encrypted = encryptor.encrypt(plaintext)
        def asUtf8 = new String(encrypted, StandardCharsets.UTF_8)

        then:
        !asUtf8.contains("secret-token-value")
    }

    def "null master key fails at construction"() {
        when:
        new AesGcmCredentialEncryptor(null)

        then:
        thrown(NullPointerException)
    }

    def "non-AES algorithm key fails at construction"() {
        given:
        // DES has 8-byte keys — valid for DES but wrong algorithm for our encryptor
        def desKey = new SecretKeySpec(
            "12345678".getBytes(StandardCharsets.UTF_8), "DES")

        when:
        new AesGcmCredentialEncryptor(desKey)

        then:
        thrown(IllegalArgumentException)
    }

    def "AES key shorter than 32 bytes fails at construction"() {
        given: "a 16-byte AES key (AES-128, not AES-256)"
        def shortKey = new SecretKeySpec(
            "1234567890123456".getBytes(StandardCharsets.UTF_8), "AES")

        when:
        new AesGcmCredentialEncryptor(shortKey)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("256 bits")
        e.message.contains("32 bytes")
    }

    def "AES key longer than 32 bytes fails at construction"() {
        given: "a 48-byte AES key (not a valid AES key size)"
        def longKey = new SecretKeySpec(
            "123456789012345678901234567890123456789012345678".getBytes(StandardCharsets.UTF_8),
            "AES")

        when:
        new AesGcmCredentialEncryptor(longKey)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("256 bits")
        e.message.contains("32 bytes")
    }

    // ── Helpers ────────────────────────────────────────────────────

    private static AesGcmCredentialEncryptor buildEncryptor(
        SecretKey key) {
        new AesGcmCredentialEncryptor(key, new SecureRandom())
    }
}
