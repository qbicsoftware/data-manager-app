package life.qbic.identity.domain.model.token

import spock.lang.Specification

class PersonalAccessTokenEncoderTest extends Specification {

    def "when a token is encoded then the encoded token is not the original input"() {
        given:
        String salt = "1234"
        var iterationCount = 10_000
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
        var iterationCount = 10_000
        PersonalAccessTokenEncoder tokenEncoderUnderTest = new PersonalAccessTokenEncoder(salt, iterationCount)
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
        var iterationCount = 10_000
        PersonalAccessTokenEncoder tokenEncoderUnderTest = new PersonalAccessTokenEncoder(salt, iterationCount)
        var secret = "Hello World!".toCharArray()

        when: "a token is encoded"
        var encodedToken = tokenEncoderUnderTest.encode(secret)

        then: "the encoded token matches the token pattern"
        encodedToken.matches("^" + iterationCount + ":.+:.+\$")

    }

    def "when a token is encrypted and compared to its raw form then the comparison succeeds"() {
        given:
        String salt = "1234"
        var iterationCount = 10_000
        PersonalAccessTokenEncoder tokenEncoderUnderTest = new PersonalAccessTokenEncoder(salt, iterationCount)

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
        var iterationCount = 10_000
        PersonalAccessTokenEncoder tokenEncoderUnderTest = new PersonalAccessTokenEncoder(salt, iterationCount)
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
        new PersonalAccessTokenEncoder(null, 10_000)
        then: "fail"
        thrown(RuntimeException)
    }

    def "when an empty salt is provided then fail"() {
        when: "no salt is provided"
        new PersonalAccessTokenEncoder("", 10_000)
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
}
