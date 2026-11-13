package life.qbic.datamanager.configuration

import org.springframework.mail.javamail.JavaMailSenderImpl
import spock.lang.Specification

/**
 * <b>The startup gate must actually be part of the application context.</b>
 *
 * <p>This spec exists because {@link MailConnectionCheck} was, for one revision, the most dangerous
 * kind of broken: correct in isolation and absent at runtime.</p>
 *
 * <p>It carried {@code @ConditionalOnSingleCandidate(JavaMailSenderImpl.class)}, copied from Boot's
 * own {@code MailSenderValidatorAutoConfiguration}, where that condition is correct because an
 * auto-configuration is ordered after user beans. In a component-scanned {@code @Configuration} it is
 * an <em>ordering assumption</em>: conditions are evaluated while scanning registers candidates,
 * before the application's {@code @Bean} methods have run, so no {@code JavaMailSenderImpl} existed
 * and the condition never matched. The application started cleanly with a stale mail password, and
 * the only symptom was a password-reset job failing minutes later with {@code 535 5.7.8}.</p>
 *
 * <p>Every unit spec of the check passed throughout, because they construct the class directly. What
 * was missing was an assertion that the class is <em>in</em> the context — which is what these cases
 * add. The pattern is general: a conditional annotation on a scanned configuration fails open, so
 * "the bean exists" has to be asserted separately from "the bean works".</p>
 */
class MailConnectionCheckWiringSpec extends Specification {

  def "the check declares no condition that depends on bean-registration order"() {
    given: 'the annotations on the class'
    def conditions = MailConnectionCheck.annotations
        .collect { it.annotationType().name }
        .findAll { it.contains('Conditional') }

    expect: 'it is gated on the opt-out property, which is a value and not an ordering assumption'
    conditions.any { it.endsWith('ConditionalOnBooleanProperty') }

    and: 'nothing conditions it on a bean that component scanning has not created yet'
    conditions.every { !it.contains('ConditionalOnSingleCandidate') }
    conditions.every { !it.contains('ConditionalOnBean') }
  }

  def "the constructor tolerates a missing sender by failing, not by doing nothing silently"() {
    given: 'a provider that has no sender to offer'
    def emptyProvider = new StaticObjectProvider(null)

    when: 'the check is created anyway'
    new MailConnectionCheck(emptyProvider)

    then: 'it fails closed, so the gate cannot be defeated by a wiring mistake'
    def failure = thrown(IllegalStateException)
    failure.message.contains('no JavaMailSenderImpl bean is available')
  }

  def "the opt-out property exists in the shipped configuration and the template"() {
    expect: 'the gate is on by default, and the escape hatch is documented next to it'
    propertiesOf('datamanager-app/src/main/resources/application.properties')
        .getProperty('qbic.mail.verify-connection-on-startup') != null
    propertiesOf('application.properties.template')
        .getProperty('qbic.mail.verify-connection-on-startup') != null
  }

  // ---- helpers ----------------------------------------------------------------------------------

  private static Properties propertiesOf(String path) {
    def candidates = [new File(path), new File("../$path"), new File("../../$path")]
    def file = candidates.find { it.exists() }
    assert file != null : "$path not found from ${new File('.').absolutePath}"
    def properties = new Properties()
    file.withInputStream { properties.load(it) }
    return properties
  }

  /**
   * A minimal {@link ObjectProvider} that always answers with the same value.
   *
   * <p>Hand-written because the provider's only use here is {@code getIfAvailable()}; mocking the
   * Spring interface would need a real context to be meaningful, which is what the wiring case above
   * cannot have in this module.</p>
   */
  private static class StaticObjectProvider implements org.springframework.beans.factory.ObjectProvider<JavaMailSenderImpl> {
    private final JavaMailSenderImpl value

    StaticObjectProvider(JavaMailSenderImpl value) { this.value = value }

    @Override JavaMailSenderImpl getObject() { return value }
    @Override JavaMailSenderImpl getObject(Object... args) { return value }
    @Override JavaMailSenderImpl getIfAvailable() { return value }
    @Override JavaMailSenderImpl getIfUnique() { return value }
    @Override void ifAvailable(java.util.function.Consumer<JavaMailSenderImpl> consumer) { if (value != null) consumer.accept(value) }
    @Override void ifUnique(java.util.function.Consumer<JavaMailSenderImpl> consumer) { ifAvailable(consumer) }
    @Override Iterator<JavaMailSenderImpl> iterator() { value == null ? Collections.emptyIterator() : [value].iterator() }
    @Override java.util.stream.Stream<JavaMailSenderImpl> stream() { value == null ? java.util.stream.Stream.empty() : java.util.stream.Stream.of(value) }
    @Override java.util.stream.Stream<JavaMailSenderImpl> orderedStream() { stream() }
  }
}
