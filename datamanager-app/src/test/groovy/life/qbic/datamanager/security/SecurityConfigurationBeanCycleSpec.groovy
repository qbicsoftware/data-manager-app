package life.qbic.datamanager.security

import spock.lang.Specification

import java.lang.reflect.Modifier

/**
 * <b>A {@code @Configuration} class must not depend on a bean it declares itself.</b>
 *
 * <p>This is a hard startup failure, not a warning: Spring Boot 4 prohibits circular references by
 * default, so the application refuses to boot with</p>
 *
 * <pre>
 * The dependencies of some of the beans in the application context form a cycle:
 * ┌──->──┐
 * |  securityConfiguration
 * └──<-──┘
 * </pre>
 *
 * <p>That is exactly what happened when {@code loginThrottleFilter()} needed a {@code PasswordEncoder}
 * and the constructor was given one to inject: {@code SecurityConfiguration} both <em>declared</em>
 * that bean and <em>required</em> it.</p>
 *
 * <h3>Why this is a unit spec and not an integration test</h3>
 * <p>There is no {@code @SpringBootTest} in this module — a full context needs MariaDB, Artemis, an
 * SMTP host, an OpenBIS instance and the vault. So the cycle was first found by a real boot, i.e. by
 * CI or by hand, and neither is a fast feedback loop. This spec checks the property directly from the
 * bytecode, which needs none of that infrastructure.</p>
 *
 * @since 1.0.0
 */
class SecurityConfigurationBeanCycleSpec extends Specification {

  def "the configuration class does not require any bean it declares"() {
    given:
    def type = SecurityConfiguration

    and: 'every type this class publishes as a bean'
    def declaredBeanTypes = type.declaredMethods
        .findAll { it.isAnnotationPresent(org.springframework.context.annotation.Bean) }
        .collect { it.returnType }
        .toSet()

    and: 'every type its constructor requires'
    def constructorParameterTypes = type.declaredConstructors
        .findAll { !it.synthetic }
        .collectMany { it.parameterTypes as List }
        .toSet()

    expect: 'the class actually declares beans, so the spec cannot pass vacuously'
    !declaredBeanTypes.isEmpty()

    and: 'and no constructor parameter is one of them — that is the cycle'
    def overlapping = constructorParameterTypes.intersect(declaredBeanTypes)
    overlapping.isEmpty()

    and: 'the message names what went wrong, if it ever does'
    assert !overlapping.any(), "SecurityConfiguration declares ${overlapping} as @Bean and also " +
        'requires it in its constructor — Spring Boot 4 will refuse to start with a bean cycle. ' +
        'Construct the value locally instead of injecting a bean this class defines.'
  }

  def "the constructor still takes the collaborators it genuinely needs"() {
    // Guards the opposite mistake: "fixing" a cycle by silently dropping a dependency and then
    // failing at runtime with a null. The throttle is the one this PR added, and it must be there.
    given:
    def parameterTypes = SecurityConfiguration.declaredConstructors
        .findAll { !it.synthetic }
        .collectMany { it.parameterTypes as List }

    expect:
    parameterTypes.contains(life.qbic.identity.application.throttle.AuthAttemptThrottle)
    parameterTypes.contains(life.qbic.identity.application.user.IdentityService)
    parameterTypes.contains(com.vaadin.flow.spring.security.VaadinDefaultRequestCache)
  }

  def "the password encoder is constructed locally, not injected"() {
    given:
    def encoderFields = SecurityConfiguration.declaredFields.findAll {
      !Modifier.isStatic(it.modifiers)
          && org.springframework.security.crypto.password.PasswordEncoder
              .isAssignableFrom(it.type)
    }

    expect: 'exactly one field holds the encoder'
    encoderFields.size() == 1

    and: 'it is initialised at declaration, so the constructor cannot require it'
    Modifier.isFinal(encoderFields.first().modifiers)
  }
}
