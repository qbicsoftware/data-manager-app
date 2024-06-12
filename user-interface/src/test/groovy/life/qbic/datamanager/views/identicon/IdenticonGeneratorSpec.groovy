package life.qbic.datamanager.views.identicon

import spock.lang.Specification

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
class IdenticonGeneratorSpec extends Specification {
    def "fails for null input"() {
        when:
        IdenticonGenerator.generateIdenticon(null)
        then:
        thrown(IllegalArgumentException)
    }

    def "fails for empty input"() {
        when:
        IdenticonGenerator.generateIdenticon("")
        then:
        thrown(IllegalArgumentException)
    }

    def "works for other input"() {
        when:
        IdenticonGenerator.generateIdenticon(input)
        then:
        notThrown(IllegalArgumentException)
        where:
        input <<
                ["test",
                 "1ca88d1f-acdb-42fe-a7ab-2d841163de41",
                 UUID.randomUUID().toString(),
                 UUID.randomUUID().toString(),
                 UUID.randomUUID().toString(),
                 UUID.randomUUID().toString(),
                 UUID.randomUUID().toString(),
                 UUID.randomUUID().toString()]
    }
}
