package life.qbic.datamanager.security

import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The ORCID registry must be one value, and it must be stated.</b>
 *
 * <p>A single deployment came to hold {@code users.oidcIssuer} values from <em>two</em> registries —
 * {@code https://orcid.org} on some rows and {@code https://sandbox.orcid.org} on others — because the
 * OAuth provider defaulted to sandbox while the person-search integration defaulted to production, and
 * the value stamped on a link was a third setting with a silent sandbox fallback.</p>
 *
 * <p>That is not cosmetic. The issuer is half of the link's uniqueness key, so the same researcher's iD
 * on two registries is two different links: neither blocks the other, and a user can end up unable to
 * attach an iD because a stale row under the other registry still holds it.</p>
 *
 * <p>These assertions are about configuration, not behaviour — because the failure was configuration.</p>
 */
class OrcidRegistryConfigurationSpec extends Specification {

  private static Properties load(String path) {
    def candidates = [new File(path), new File("../$path")]
    def file = candidates.find { it.exists() }
    assert file != null: "$path not found from ${new File('.').absolutePath}"
    def properties = new Properties()
    file.withInputStream { properties.load(it) }
    return properties
  }

  @Unroll
  def "the issuer is declared in #path rather than left to a code default"() {
    given:
    def properties = load(path)

    expect: 'the key exists, so the registry is a deployment decision'
    properties.getProperty('qbic.oidc.orcid.issuer') != null

    where:
    path << ['datamanager-app/src/main/resources/application.properties',
             'application.properties.template']
  }

  def "no code default picks a registry on the operator's behalf"() {
    given: 'the security configuration, read as source'
    def source = new File('datamanager-app/src/main/java/life/qbic/datamanager/security/'
        + 'SecurityConfiguration.java')
    def text = source.exists()
        ? source.text
        : new File('../datamanager-app/src/main/java/life/qbic/datamanager/security/'
            + 'SecurityConfiguration.java').text

    expect: 'the issuer is injected without a fallback value'
    text.contains('${qbic.oidc.orcid.issuer}')

    and: 'no sandbox or production registry is hard-coded as a default'
    !text.contains('${qbic.oidc.orcid.issuer:')
  }

  @Unroll
  def "the person-search issuer is derived from the account issuer in #path"() {
    given:
    def properties = load(path)

    expect: 'one fact, one setting — a searched researcher and a linked account must agree'
    properties.getProperty('qbic.external-service.person-search.orcid.issuer')
        .contains('qbic.oidc.orcid.issuer')

    where:
    path << ['datamanager-app/src/main/resources/application.properties',
             'application.properties.template']
  }

  @Unroll
  def "the registry is derived from the authorization URI: #uri -> #expected"() {
    expect:
    SecurityConfiguration.registryOf(uri).orElse(null) == expected

    where:
    uri                                              | expected
    'https://orcid.org/oauth/authorize'              | 'https://orcid.org'
    'https://sandbox.orcid.org/oauth/authorize'      | 'https://sandbox.orcid.org'
    'https://orcid.example:8443/oauth/authorize'     | 'https://orcid.example:8443'
    null                                             | null
    ''                                               | null
    'not a uri'                                      | null
    '/relative/authorize'                            | null
  }
}
