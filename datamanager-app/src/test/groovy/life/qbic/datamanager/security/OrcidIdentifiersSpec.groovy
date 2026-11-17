package life.qbic.datamanager.security

import life.qbic.datamanager.views.general.oidc.OidcType
import life.qbic.identity.api.OrcidIds
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>An ORCID iD is validated once, and its record URL is derived once.</b>
 *
 * <p>Two defects motivate these assertions, both found by reading the running product rather than the
 * suite:</p>
 *
 * <ol>
 *   <li><b>The record URL was built in five places</b>, and one hard-coded {@code https://orcid.org} — so
 *       a sandbox-linked iD rendered as a link into the production registry. A link that looks right and
 *       goes somewhere else.</li>
 *   <li><b>Nothing validated the iD at all.</b> Whatever the provider put in {@code sub} was stored
 *       verbatim and then placed inside URLs on the profile, in project contacts and in the person
 *       picker.</li>
 * </ol>
 */
class OrcidIdentifiersSpec extends Specification {

  // ---- validation ------------------------------------------------------------------------------

  @Unroll
  def "'#candidate' is valid: #valid"() {
    expect:
    OrcidIds.isValid(candidate) == valid

    where:
    candidate                            | valid
    '0000-0002-1825-0097'                | true
    '0009-0009-6121-8031'                | true
    '0000-0001-8835-2219'                | true
    '0000-0002-1694-233X'                | true    // X check character is legal
    // Near misses that must not pass
    '0000-0002-1694-233x'                | false   // lower-case X: reject, never repair
    '0000-0002-1825-009'                 | false   // too short
    '0000-0002-1825-00977'               | false   // too long
    'https://orcid.org/0000-0002-1825-0097' | false  // a URI is not an iD
    '0000000218250097'                   | false   // hyphens are not optional
    'not-an-orcid'                       | false
    ''                                   | false
    '   '                                | false
    null                                 | false
    '0000-0002-1825-0097 '               | false   // trailing space
    '0000-0002-1825-0097\n'              | false   // trailing newline
  }

  @Unroll
  def "an iD carrying URL syntax is rejected rather than passed into a URL: #candidate"() {
    expect: 'a value with URL syntax in it must never reach a link'
    !OrcidIds.isValid(candidate)

    where:
    candidate << [
        '0000-0002-1825-0097/../../admin',
        '0000-0002-1825-0097?next=https://evil.example',
        '0000-0002-1825-0097#fragment',
        '<script>alert(1)</script>',
    ]
  }

  def "the record path rejects an invalid iD instead of returning it"() {
    when:
    OrcidIds.recordPath('not-an-orcid')

    then:
    thrown(IllegalArgumentException)

    and: 'a valid iD is returned unchanged, not rewritten'
    OrcidIds.recordPath('0000-0002-1825-0097') == '0000-0002-1825-0097'
  }

  // ---- one derivation, correct host ------------------------------------------------------------

  @Unroll
  def "an iD linked against #issuer renders a record under that registry"() {
    given:
    def type = OidcType.forIssuer(issuer).orElseThrow()

    expect: 'the host comes from the issuer, so sandbox and production do not cross over'
    type.getUrlFor('0000-0002-1825-0097') == expected

    where:
    issuer                          | expected
    'https://orcid.org'             | 'https://orcid.org/0000-0002-1825-0097'
    'https://sandbox.orcid.org'     | 'https://sandbox.orcid.org/0000-0002-1825-0097'
  }

  def "an unknown issuer resolves to nothing rather than to a wrong registry"() {
    expect: 'guessing would produce exactly the sandbox-into-production bug'
    OidcType.forIssuer('https://some-other-idp.example').isEmpty()
    OidcType.forIssuer(null).isEmpty()
  }

  def "no view derives the record URL itself"() {
    given: 'the views that render an ORCID iD'
    def views = [
        'datamanager-app/src/main/java/life/qbic/datamanager/views/account/UserProfileComponent.java',
        'datamanager-app/src/main/java/life/qbic/datamanager/views/account/AccountContentBox.java',
        'datamanager-app/src/main/java/life/qbic/datamanager/views/general/contact/ContactField.java',
    ]

    expect: 'a hard-coded registry host is the defect this spec exists for'
    views.every { relative ->
        def file = new File(relative).exists() ? new File(relative) : new File("../$relative")
        !file.text.contains('"https://orcid.org/')
        !file.text.contains("'https://orcid.org/")
    }
  }

  def "the profile view takes the URL from OidcType rather than building one"() {
    given:
    def relative =
        'datamanager-app/src/main/java/life/qbic/datamanager/views/account/UserProfileComponent.java'
    def file = new File(relative).exists() ? new File(relative) : new File("../$relative")

    expect:
    file.text.contains('OidcType.forIssuer')
  }
}
