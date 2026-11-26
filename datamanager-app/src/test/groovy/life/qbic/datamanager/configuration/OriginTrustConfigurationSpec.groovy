package life.qbic.datamanager.configuration

import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
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
    new OriginTrustConfiguration(false, 'none', new StandardEnvironment()) != null
    new OriginTrustConfiguration(false, 'framework', new StandardEnvironment()) != null
    new OriginTrustConfiguration(false, null, new StandardEnvironment()) != null
    new OriginTrustConfiguration(false, 'native', new StandardEnvironment()) != null
  }

  @Unroll
  def "origin scope with strategy '#strategy' starts"() {
    expect:
    new OriginTrustConfiguration(true, strategy, new StandardEnvironment()) != null

    where:
    strategy << ['native', 'NATIVE', 'Native', ' native ']
  }

  @Unroll
  def "origin scope with strategy '#strategy' refuses to start"() {
    when:
    new OriginTrustConfiguration(true, strategy, new StandardEnvironment())

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
    new OriginTrustConfiguration(true, 'framework', new StandardEnvironment())

    then:
    def failure = thrown(IllegalStateException)
    failure.message.contains('LEFTMOST')
    failure.message.contains('option forwardfor')
    failure.message.contains('native')
  }

  def "the refusal points at the runbook"() {
    when:
    new OriginTrustConfiguration(true, 'none', new StandardEnvironment())

    then:
    def failure = thrown(IllegalStateException)
    failure.message.contains('installation-and-administration.md')
  }

  // ---- the message must be actionable, not merely accurate ---------------------------------------
  //
  // Regression guard for a real report. Enabling origin scope without setting the strategy produced:
  //
  //   requires server.forward-headers-strategy=native, but it is currently 'none'.
  //
  // which reads as though someone configured `none`. An operator greps their configuration for it,
  // finds nothing, and is stuck — because the value is Boot's default, not a setting. The message has
  // to say where the value came from.

  def "when nothing sets the property, the message says the value is the default"() {
    // A real Environment with no such property, which is what a deployment that only set
    // AUTH_THROTTLE_LOGIN_ORIGIN_ENABLED=true looks like.
    given:
    def environment = new StandardEnvironment()
    environment.propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
    environment.propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)

    when:
    new OriginTrustConfiguration(true, 'none', environment)

    then: 'the operator is told not to go looking for a setting that does not exist'
    def failure = thrown(IllegalStateException)
    failure.message.contains("currently 'none' (the default; the property is not set anywhere)")
  }

  def "when the property IS set, the message names the key that supplied it"() {
    // Including the relaxed/upper-case form, because that is what an exported environment variable
    // looks like — and an operator would not recognise `SERVER_FORWARD_HEADERS_STRATEGY` if the
    // message only ever spoke about `server.forward-headers-strategy`.
    given:
    def environment = new StandardEnvironment()
    environment.propertySources.addFirst(
        new MapPropertySource('test-env', ['SERVER_FORWARD_HEADERS_STRATEGY': 'none']))

    when:
    new OriginTrustConfiguration(true, 'none', environment)

    then:
    def failure = thrown(IllegalStateException)
    failure.message.contains("from 'SERVER_FORWARD_HEADERS_STRATEGY'")
  }

  def "an explicitly set non-none value is reported as explicit, not as a default"() {
    when:
    new OriginTrustConfiguration(true, 'framework', new StandardEnvironment())

    then:
    def failure = thrown(IllegalStateException)
    failure.message.contains("currently 'framework' (set explicitly)")
  }

  def "a broken environment cannot stop the gate from firing"() {
    // The diagnostics are a courtesy. If the Environment throws — or is missing — the security refusal
    // must still happen, or a well-meaning message becomes a way to boot an insecure configuration.
    when:
    new OriginTrustConfiguration(true, 'none', null)

    then:
    def failure = thrown(IllegalStateException)
    failure.message.contains('server.forward-headers-strategy=native')
  }

  // ---- configuration files ------------------------------------------------------------------------

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

  def "the shipped configuration does not set the forward-headers strategy"() {
    // Setting it in the shipped file would apply it to every deployment at once, which is exactly the
    // all-or-nothing rollout #1691 stages away. It must remain an operator decision.
    expect:
    propertiesOf('datamanager-app/src/main/resources/application.properties')
        .getProperty('server.forward-headers-strategy') == null
  }

  @Unroll
  def "the SHIPPED configuration also leaves origin scope #what"() {
    // Regression guard for a defect that reached main. A commit that was supposed to comment out one
    // line swept up local test edits and shipped:
    //
    //     login.origin.enabled=${AUTH_THROTTLE_LOGIN_ORIGIN_ENABLED:true}
    //     login.origin.max-attempts=${AUTH_THROTTLE_LOGIN_ORIGIN_MAX_ATTEMPTS:3}
    //
    // Two consequences, both bad. `enabled=true` makes OriginTrustConfiguration refuse to boot in every
    // environment without server.forward-headers-strategy=native -- local development and CI included.
    // And an origin limit of 3 sits BELOW the per-account limit of 10, so one institution behind a NAT
    // is blocked after four failed logins from that network: the whole-population lockout this design
    // exists to prevent.
    //
    // The earlier spec only checked the template, which is why the shipped file could drift unnoticed.
    expect:
    defaultValueOf('datamanager-app/src/main/resources/application.properties', key) == value

    where:
    key                                          | value  | what
    'qbic.auth-throttle.login.origin.enabled'    | 'false' | 'switched off by default'
    'qbic.auth-throttle.login.origin.max-attempts' | '60'  | 'above the per-account login limit'
  }

  @Unroll
  def "the shipped configuration and the template agree on #key"() {
    // Two sources of truth for one default is how the drift above happened. They must not disagree.
    expect: 'the shipped file may use ${VAR:default}; what must match is the DEFAULT'
    defaultValueOf('datamanager-app/src/main/resources/application.properties', key) ==
        defaultValueOf('application.properties.template', key)

    where:
    key << [
        'qbic.auth-throttle.login.origin.enabled',
        'qbic.auth-throttle.login.origin.max-attempts',
        'qbic.auth-throttle.login.origin.window',
        'qbic.auth-throttle.login.origin.block-duration',
        'qbic.auth-throttle.mail-origin.max-attempts',
        'qbic.auth-throttle.mail-origin.window',
        'qbic.auth-throttle.mail-origin.block-duration',
        'qbic.auth-throttle.registration-origin.max-attempts',
        'qbic.auth-throttle.registration-origin.window',
        'qbic.auth-throttle.registration-origin.block-duration',
    ]
  }

  @Unroll
  def "the shipped configuration's origin limit stays above its account limit for #flow"() {
    // The invariant that makes an origin bucket survivable for a shared network. An origin limit at or
    // below the per-account limit means the origin blocks first, and it blocks everyone behind it.
    given:
    def account = defaultValueOf('datamanager-app/src/main/resources/application.properties',
        accountKey) as int
    def origin = defaultValueOf('datamanager-app/src/main/resources/application.properties',
        originKey) as int

    expect:
    origin > account

    where:
    flow         | accountKey                                | originKey
    'login'      | 'qbic.auth-throttle.login.max-attempts'   | 'qbic.auth-throttle.login.origin.max-attempts'
    'resend'     | 'qbic.auth-throttle.resend.max-attempts'  | 'qbic.auth-throttle.mail-origin.max-attempts'
    'registr.'   | 'qbic.auth-throttle.resend.max-attempts'  | 'qbic.auth-throttle.registration-origin.max-attempts'
  }

  def "the template ships origin scope switched off"() {
    // If this ever flips, every deployment that pulls the new configuration silently starts rewriting
    // absolute links. The issue stages that.
    expect:
    propertiesOf('application.properties.template')
        .getProperty('qbic.auth-throttle.login.origin.enabled') == 'false'
  }

  def "the template ships the safe strategy so following it verbatim cannot fail startup"() {
    // The template is the blueprint an operator copies. If it shipped `native` here, every deployment
    // built from it would enable the headers without the canary — and if it shipped `framework` or an
    // uncommented unsafe value, a deployment that also enabled origin scope would refuse to boot.
    expect:
    propertiesOf('application.properties.template')
        .getProperty('server.forward-headers-strategy') == 'none'
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

  /**
   * The effective default of a key, with a {@code ${ENV_VAR:default}} placeholder resolved.
   *
   * <p>The shipped {@code application.properties} expresses defaults as {@code ${VAR:default}} so a
   * deployment can override them from the environment, while the template carries literals. Comparing
   * the raw values would compare a placeholder against a number and fail for the wrong reason — which
   * is what the first version of these assertions did.</p>
   */
  private static String defaultValueOf(String path, String key) {
    String raw = propertiesOf(path).getProperty(key)
    if (raw == null) {
      return null
    }
    def matcher = raw =~ /^\$\{[^:}]+:(.*)\}$/
    return matcher.matches() ? matcher.group(1) : raw
  }

  private static Properties propertiesOf(String path) {
    def candidates = [new File(path), new File("../$path"), new File("../../$path")]
    def file = candidates.find { it.exists() }
    assert file != null : "$path not found from ${new File('.').absolutePath}"
    def properties = new Properties()
    file.withInputStream { properties.load(it) }
    return properties
  }
}
