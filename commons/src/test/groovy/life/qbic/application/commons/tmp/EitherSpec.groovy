package life.qbic.application.commons.tmp

import spock.lang.Specification

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
class EitherSpec extends Specification {
    def "construct from value"() {
        when:
        var result = Either.fromValue(5)
        Either<Integer, String> typedResult = Either.fromValue(5)
        then:
        result instanceof Either<Integer, ?>
        typedResult instanceof Either<Integer, String>
    }

    def "construct from error"() {
        when:
        var result = Either.fromError("Oh no!")
        Either<Integer, String> typedResult = Either.fromError("Oh no!")
        then:
        result instanceof Either<?, Integer>
        typedResult instanceof Either<Integer, String>
    }
}
