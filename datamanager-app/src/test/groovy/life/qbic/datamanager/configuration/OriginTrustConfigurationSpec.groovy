package life.qbic.datamanager.configuration

import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>Origin scope must not start in a configuration that cannot enforce it.</b>
 *
 * <p>This is the code form of the issue's blocking check: <i>if {@code getRemoteAddr()} still shows a
 * proxy address after Phase C, stop.</i> A runbook line is only as reliable as the person reading it on
 * the node that is already unhealthy, so the pair of settings that makes origin scope correct is
 * enforced at startup instead.</p>
 *
 * <p>The two wrong configurations are both silent, which is why they are failures rather than warnings:</p>
 * <ul>
 *   <li>{@code framework} — {@code ForwardedHeaderFilter} takes the <b>leftmost</b>
 *       {@code X-Forwarded-For} entry with no trust check, and HAProxy's {@code option forwardfor}
 *       appends. The client therefore writes the entry that becomes the bucket key and can mint a fresh
 *       bucket per request: the throttle appears to work and is bypassable.</li>
 *   <li>{@code none} — the address stays the proxy, one bucket covers the whole deployment, and one
 *       attacker locks out everybody.</li>
 * </ul>
 */
class OriginTrustConfigurationSpec extends Specification {

  def "the default deployment (origin scope off) starts regardless of the strategy"() {
    // The shipped default. Enabling the headers also changes how absolute links and the OAuth2
    // redirect URI are built, so it is a separate, independently revertable operator step.
    expect:
    new OriginTrustConfiguration(false, 'none') != null
    new OriginTrustConfiguration(false, 'framework') != null
    new OriginTrustConfiguration(false, null) != null
    new OriginTrustConfiguration(false, 'native') != null
  }

  @Unroll
  def "origin scope with strategy '#strategy' starts"() {
    expect:
    new OriginTrustConfiguration(true, strategy) != null

    where:
    strategy << ['native', 'NATIVE', 'Native', ' native ']
  }

  @Unroll
  def "origin scope with strategy '#strategy' refuses to start"() {
    when:
    new OriginTrustConfiguration(true, strategy)

    then: 'a failed start is the only outcome that cannot be ignored'
    def failure = thrown(IllegalStateException)
    failure.message.contains('server.forward-headers-strategy=native')

    and: 'the message names the value actually found, so the fix is obvious from the log'
    failure.message.contains(strategy == null ? 'none' : strategy.strip())

    where:
    strategy << ['none', 'framework', 'FRAMEWORK', null, '', 'something-else']
  }

  def "the refusal explains why framework is unsafe rather than just rejecting it"() {
    // `framework` is what the operations documentation used to recommend, so an operator following an
    // older copy of it is the realistic way this goes wrong. "Invalid value" would not help them.
    when:
    new OriginTrustConfiguration(true, 'framework')

    then:
    def failure = thrown(IllegalStateException)
    failure.message.contains('LEFTMOST')
    failure.message.contains('option forwardfor')
    failure.message.contains('native')
  }

  def "the refusal points at the runbook"() {
    when:
    new OriginTrustConfiguration(true, 'none')

    then:
    def failure = thrown(IllegalStateException)
    failure.message.contains('installation-and-administration.md')
  }

  def "the configured property exists in the shipped configuration and the template"() {
    expect: 'the switch is discoverable without reading the code'
    propertiesOf('datamanager-app/src/main/resources/application.properties')
        .getProperty('qbic.auth-throttle.login.origin.enabled') != null
    propertiesOf('application.properties.template')
        .getProperty('qbic.auth-throttle.login.origin.enabled') != null
  }

  @Unroll
  def "the template documents #key next to the switch it belongs to"() {
    expect:
    propertiesOf('application.properties.template').getProperty(key) != null

    where:
    key << [
        'qbic.auth-throttle.login.origin.enabled',
        'qbic.auth-throttle.login.origin.max-attempts',
        'qbic.auth-throttle.login.origin.window',
        'qbic.auth-throttle.login.origin.block-duration',
        'server.forward-headers-strategy',
    ]
  }

  def "the template ships origin scope switched off"() {
    // If this ever flips, every deployment that pulls the new configuration silently starts rewriting
    // absolute links. The issue stages that.
    expect:
    propertiesOf('application.properties.template')
        .getProperty('qbic.auth-throttle.login.origin.enabled') == 'false'
  }

  def "the template's origin threshold is above the account threshold"() {
    // One origin may legitimately be a whole institution behind a NAT. An origin limit at or below the
    // per-account limit would make the origin the binding constraint for shared networks.
    given:
    def template = propertiesOf('application.properties.template')
    def account = template.getProperty('qbic.auth-throttle.login.max-attempts') as int
    def origin = template.getProperty('qbic.auth-throttle.login.origin.max-attempts') as int

    expect:
    origin > account
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
}
