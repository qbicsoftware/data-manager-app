package life.qbic.identity.domain.model.token

import spock.lang.Specification

class PersonalAccessTokenEncoderSpec extends Specification {

    final int validIterationCount = 100_000

    def "when a token is encoded then the encoded token is not the original input"() {
        given:
        String salt = "1234"
        var iterationCount = validIterationCount
        PersonalAccessTokenEncoder tokenEncoderUnderTest = new PersonalAccessTokenEncoder(salt, iterationCount)
        var secret = "Hello World!".toCharArray()

        when: "a token is encoded"
        var encodedToken = tokenEncoderUnderTest.encode(secret)

        then: "the encoded token is not the original input"
        encodedToken.toCharArray() != secret
    }

    def "expect two different tokens to lead to different encoded outputs"() {
        given:
        String salt = "1234"
        PersonalAccessTokenEncoder tokenEncoderUnderTest = new PersonalAccessTokenEncoder(salt, validIterationCount)
        var token = "Hello World!"

        expect:
        tokenEncoderUnderTest.encode(token.toCharArray()) != tokenEncoderUnderTest.encode(otherToken.toCharArray())

        where:
        otherToken << [
                "2222",
                "abslekwjr",
                "1234 ",
                " 1234",
                "01234"
        ]
    }

    def "when a token is encoded then the encoded token matches the token pattern"() {
        given:
        String salt = "1234"
        PersonalAccessTokenEncoder tokenEncoderUnderTest = new PersonalAccessTokenEncoder(salt, validIterationCount)
        var secret = "Hello World!".toCharArray()

        when: "a token is encoded"
        var encodedToken = tokenEncoderUnderTest.encode(secret)

        then: "the encoded token matches the token pattern"
        encodedToken.matches("^" + validIterationCount + ":.+:.+\$")

    }

    def "when a token is encrypted and compared to its raw form then the comparison succeeds"() {
        given:
        String salt = "1234"
        PersonalAccessTokenEncoder tokenEncoderUnderTest = new PersonalAccessTokenEncoder(salt, validIterationCount)

        when: "a token is encrypted and compared to its raw form"
        var encodedToken = tokenEncoderUnderTest.encode(secret)

        then: "the comparison succeeds"
        tokenEncoderUnderTest.matches(secret, encodedToken)

        where:
        secret << [
                "Hello World".toCharArray(),
                "1234slwlekrjwlekgjö".toCharArray(),
                "ABC123abc123".toCharArray()
        ]
    }

    def "expect the comparison to fail for non-matching tokens"() {
        given:
        String salt = "1234"
        PersonalAccessTokenEncoder tokenEncoderUnderTest = new PersonalAccessTokenEncoder(salt, validIterationCount)
        var secret = "Hello World!".toCharArray()

        expect:
        !tokenEncoderUnderTest.matches(tokenEncoderUnderTest.encode(secret).toCharArray(), encodedToken)

        where:
        encodedToken << [
                "900:31323334:7a85f3f3c352af5964bb75750fbb98f7e3febd5d00b1793c5d0382c9d59d644e",
                "10000:31323000:7a85f3f3c352af5964bb75750fbb98f7e3febd5d00b1793c5d0382c9d59d644e",
                "10000:31323334:0005f3f3c352af5964bb75750fbb98f7e3febd5d00b1793c5d0382c9d59d6000"
        ]
    }

    def "when no salt is provided then fail"() {
        when: "no salt is provided"
        new PersonalAccessTokenEncoder(null, validIterationCount)
        then: "fail"
        thrown(RuntimeException)
    }

    def "when an empty salt is provided then fail"() {
        when: "no salt is provided"
        new PersonalAccessTokenEncoder("", validIterationCount)
        then: "fail"
        thrown(RuntimeException)
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
        count << [
                -100,
                0,
                100,
                1_000,
                10_000,
                99_999,
                PersonalAccessTokenEncoder.EXPECTED_MIN_ITERATION_COUNT - 1
        ]
    }

    def "when the iteration count is exactly the expected minimal iteration count then no IllegalArgumentException is thrown"() {
        when: "the iteration count is exactly the expected minimal iteration count"
        new PersonalAccessTokenEncoder("1234", count)

        then: "all good"
        noExceptionThrown()

        where:
        count = PersonalAccessTokenEncoder.EXPECTED_MIN_ITERATION_COUNT
    }

    def "when the iteration count is greater than the expected minimal iteration count then no IllegalArgumentException is thrown"() {
        when: "the iteration count is greater than the expected minimal iteration count"
        new PersonalAccessTokenEncoder("1234", count)

        then: "no IllegalArgumentException is thrown"
        noExceptionThrown()

        where:
        count = PersonalAccessTokenEncoder.EXPECTED_MIN_ITERATION_COUNT + 1
    }

    def "expect the minimal iteration count to be at least 100_000"() {
        expect:
        PersonalAccessTokenEncoder.EXPECTED_MIN_ITERATION_COUNT >= 100_000
    }
}
