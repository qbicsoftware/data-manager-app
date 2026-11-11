package life.qbic.datamanager.configuration

import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The distribution template must stay a working blueprint.</b>
 *
 * <p>{@code application.properties.template} is the single source of truth for a deployment
 * configuration — administrators set values there directly rather than through environment
 * variables. That makes it load-bearing, and it drifted: this release renamed two routing keys and
 * added three lifetime keys, and the template still carried the old names. A deployment built from
 * the stale template would have started cleanly and ignored every one of those settings, because
 * Spring Boot does not fail on unknown properties.</p>
 *
 * <p>These assertions are deliberately about <em>key presence and value shape</em>, not behaviour —
 * behaviour is covered elsewhere. The point is to fail the build when template and code disagree.</p>
 */
class ConfigurationTemplateSpec extends Specification {

    private static Properties loadTemplate() {
        def candidates = [new File("../application.properties.template"),
                          new File("application.properties.template")]
        def file = candidates.find { it.exists() }
        assert file != null : "application.properties.template not found from " +
                new File('.').absolutePath
        def properties = new Properties()
        file.withInputStream { properties.load(it) }
        return properties
    }

    @Unroll
    def "the template configures #key"() {
        expect: "every key the application reads is present in the blueprint"
        loadTemplate().getProperty(key) != null

        where:
        key << [
                'routing.email-confirmation.endpoint',
                'routing.email-confirmation.token-parameter',
                'routing.password-reset.endpoint',
                'routing.password-reset.token-parameter',
                'qbic.verification-token.confirmation-lifetime',
                'qbic.verification-token.reset-lifetime',
                'qbic.verification-token.set-password-lifetime',
        ]
    }

    @Unroll
    def "the template no longer carries the removed key #key"() {
        expect: "a stale key silently does nothing at runtime, which is worse than an error"
        loadTemplate().getProperty(key) == null

        where:
        key << [
                'routing.email-confirmation.confirmation-parameter',
                'routing.password-reset.reset-parameter',
        ]
    }

    def "the template pins the confirmation link to the route that consumes tokens"() {
        expect: "not /login — that route no longer confirms anything"
        loadTemplate().getProperty('routing.email-confirmation.endpoint') == '/registration/confirm'
    }

    def "the template uses the token parameter name the application expects"() {
        expect:
        loadTemplate().getProperty('routing.email-confirmation.token-parameter') == 'token'
        loadTemplate().getProperty('routing.password-reset.token-parameter') == 'token'
    }

    @Unroll
    def "the template's #key default is within the AUTH-R-05 bound"() {
        given: "the defaults the template ships must be valid, or a fresh deployment cannot start"
        def value = java.time.Duration.parse(loadTemplate().getProperty(key))

        expect:
        value <= max
        !value.isZero() && !value.isNegative()

        where:
        key                                             | max
        'qbic.verification-token.confirmation-lifetime' | java.time.Duration.ofHours(1)
        'qbic.verification-token.reset-lifetime'        | java.time.Duration.ofMinutes(15)
        'qbic.verification-token.set-password-lifetime' | java.time.Duration.ofHours(1)
    }
}
