package life.qbic.identity.domain.model.token

import spock.lang.Specification

import java.security.SecureRandom
import java.util.stream.Collectors

class PersonalAccessTokenEncoderSpec extends Specification {

    final int validIterationCount = 100_000
    final String validSalt = "000102030405060708090a0b0c0d0e0f" //16 bytes -> 128bit

    def "when a token is encoded then the encoded token is not the original input"() {
        given:

        PersonalAccessTokenEncoder tokenEncoderUnderTest = new PersonalAccessTokenEncoder(validSalt, validIterationCount)
        var secret = "Hello World!".toCharArray()

        when: "a token is encoded"
        var encodedToken = tokenEncoderUnderTest.encode(secret)

        then: "the encoded token is not the original input"
        encodedToken.toCharArray() != secret
    }

    def "expect two different tokens to lead to different encoded outputs"() {
        given:
        PersonalAccessTokenEncoder tokenEncoderUnderTest = new PersonalAccessTokenEncoder(validSalt, validIterationCount)
        var token = "Hello World!"

        expect:
        tokenEncoderUnderTest.encode(token.toCharArray()) != tokenEncoderUnderTest.encode(otherToken.toCharArray())

        where:
        otherToken << ["2222",
                       "abslekwjr",
                       "1234 ",
                       " 1234",
                       "01234"]
    }

    def "when a token is encoded then the encoded token matches the token pattern"() {
        given:
        PersonalAccessTokenEncoder tokenEncoderUnderTest = new PersonalAccessTokenEncoder(validSalt, validIterationCount)
        var secret = "Hello World!".toCharArray()

        when: "a token is encoded"
        var encodedToken = tokenEncoderUnderTest.encode(secret)

        then: "the encoded token matches the token pattern"
        encodedToken.matches("^" + validIterationCount + ":.+:.+\$")

    }

    def "when a token is encrypted and compared to its raw form then the comparison succeeds"() {
        given:
        PersonalAccessTokenEncoder tokenEncoderUnderTest = new PersonalAccessTokenEncoder(validSalt, validIterationCount)

        when: "a token is encrypted and compared to its raw form"
        var encodedToken = tokenEncoderUnderTest.encode(secret)

        then: "the comparison succeeds"
        tokenEncoderUnderTest.matches(secret, encodedToken)

        where:
        secret << ["Hello World".toCharArray(),
                   "1234slwlekrjwlekgjö".toCharArray(),
                   "ABC123abc123".toCharArray()]
    }

    def "expect the comparison to fail for non-matching tokens"() {
        given:
        PersonalAccessTokenEncoder tokenEncoderUnderTest = new PersonalAccessTokenEncoder(validSalt, validIterationCount)
        var secret = "Hello World!".toCharArray()

        expect:
        !tokenEncoderUnderTest.matches(tokenEncoderUnderTest.encode(secret).toCharArray(), encodedToken)

        where:
        encodedToken << ["900:31323334:7a85f3f3c352af5964bb75750fbb98f7e3febd5d00b1793c5d0382c9d59d644e",
                         "10000:31323000:7a85f3f3c352af5964bb75750fbb98f7e3febd5d00b1793c5d0382c9d59d644e",
                         "10000:31323334:0005f3f3c352af5964bb75750fbb98f7e3febd5d00b1793c5d0382c9d59d6000"]
    }

    def "when no salt is provided then fail"() {
        when: "no salt is provided"
        new PersonalAccessTokenEncoder(null, validIterationCount)
        then: "fail"
        thrown(RuntimeException)
    }

    def "when the salt has #numberOfBytes bytes then fail"() {
        when: "the salt has less than 128 bit"
        new PersonalAccessTokenEncoder(salt, validIterationCount)

        then: "fail"
        thrown(IllegalArgumentException)

        where:
        numberOfBytes << [
                0,
                1,
                12,
                13,
                14,
                15,
                PersonalAccessTokenEncoder.EXPECTED_MIN_SALT_BYTES - 1
        ]

        salt = generateRandomSalt(numberOfBytes)
    }

    def "when the salt length is exactly the required length then succeed"() {
        when: "the salt length is exactly the required length"
        new PersonalAccessTokenEncoder(salt, validIterationCount)

        then: "succeed"
        noExceptionThrown()

        where:
        salt = generateRandomSalt(PersonalAccessTokenEncoder.EXPECTED_MIN_SALT_BYTES)

    }

    def "when the salt is longer the required length then succeed"() {
        when: "the salt length is exactly the required length"
        new PersonalAccessTokenEncoder(salt, validIterationCount)

        then: "succeed"
        noExceptionThrown()

        where:
        salt = generateRandomSalt(PersonalAccessTokenEncoder.EXPECTED_MIN_SALT_BYTES + 1)
    }

    def "expect minimum salt bytes to be at least 16"() {
        expect:
        PersonalAccessTokenEncoder.EXPECTED_MIN_SALT_BYTES >= 16
    }

    def "when the salt has at least the minimum expected bits then succeed"() {
        when: "the salt has at least the minimum expected bits"
        new PersonalAccessTokenEncoder(validSalt, validIterationCount)

        then: "succeed"
        noExceptionThrown()
    }

    def "when no iteration count is provided then fail"() {
        when: "no salt is provided"
        var ignored = new PersonalAccessTokenEncoder("1234", count)
        then: "fail"
        thrown(RuntimeException)
        where:
        count << [-100, -1, 0]
    }

    def "when the iteration count is less than the expected minimal iteration count then throw IllegalArgumentException"() {
        when: "the iteration count is less than the expected minimal iteration count"
        new PersonalAccessTokenEncoder("1234", count)
        then: "fail"
        thrown(IllegalArgumentException)
        where:
        count << [-100,
                  0,
                  100,
                  1_000,
                  10_000,
                  99_999,
                  PersonalAccessTokenEncoder.EXPECTED_MIN_ITERATION_COUNT - 1]
    }

    def "when the iteration count is exactly the expected minimal iteration count then no IllegalArgumentException is thrown"() {
        when: "the iteration count is exactly the expected minimal iteration count"
        new PersonalAccessTokenEncoder(validSalt, count)

        then: "all good"
        noExceptionThrown()

        where:
        count = PersonalAccessTokenEncoder.EXPECTED_MIN_ITERATION_COUNT
    }

    def "when the iteration count is greater than the expected minimal iteration count then no IllegalArgumentException is thrown"() {
        when: "the iteration count is greater than the expected minimal iteration count"
        new PersonalAccessTokenEncoder(validSalt, count)

        then: "no IllegalArgumentException is thrown"
        noExceptionThrown()

        where:
        count = PersonalAccessTokenEncoder.EXPECTED_MIN_ITERATION_COUNT + 1
    }

    def "expect the minimal iteration count to be at least 100_000"() {
        expect:
        PersonalAccessTokenEncoder.EXPECTED_MIN_ITERATION_COUNT >= 100_000
    }

    String generateRandomSalt(int byteCount) {
        var random = new SecureRandom()
        byte[] bytes = new byte[byteCount]
        random.nextBytes(bytes)
        return Arrays.stream(bytes)
                .map(b -> HexFormat.of().toHexDigits(b as byte))
                .collect(Collectors.joining())
    }
}
