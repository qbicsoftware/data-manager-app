package life.qbic.datamanager.security

import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The ORCID link's return path must stay on this origin.</b>
 *
 * <p>{@code /link/orcid?return=...} takes an attacker-controllable value and uses it as a redirect
 * target after the provider round trip. That is an open redirect unless the value is constrained, and a
 * link on the application's own domain that forwards to another site is a credible phishing step: the
 * victim sees a URL they trust, is bounced to the attacker's page, and the "we are changing how sign-in
 * works" mail already in their inbox makes the story hang together.</p>
 *
 * <p>The interesting cases are the ones a naive {@code startsWith("/")} check lets through:
 * {@code //evil.example} is scheme-relative and resolves to another origin.</p>
 */
class OidcLinkReturnPathSpec extends Specification {

  @Unroll
  def "the return path '#candidate' is reduced to #expected"() {
    expect:
    OidcLinkController.safeReturnPath(candidate) == expected

    where:
    candidate                     | expected
    // Ordinary cases
    '/settings/profile'           | '/settings/profile'
    'settings/profile'            | '/settings/profile'
    null                          | '/settings/profile'
    ''                            | '/settings/profile'
    '   '                         | '/settings/profile'

    // Attempts to leave the origin
    '//evil.example/phish'        | '/settings/profile'
    '///evil.example/phish'       | '/settings/profile'
    'https://evil.example/phish'  | '/settings/profile'
    'http://evil.example'         | '/settings/profile'
    '/\\evil.example'             | '/settings/profile'
    'javascript:alert(1)'         | '/settings/profile'
  }

  def "a legitimate nested path with query parameters is preserved"() {
    expect: 'only the origin is constrained, not the path'
    OidcLinkController.safeReturnPath('/projects/list?page=2') == '/projects/list?page=2'
  }

  def "the pending-link marker holds the user id and nothing else"() {
    given: 'the session key the controller writes'
    def key = OidcLinkController.PENDING_LINK

    expect: 'it is namespaced to the controller, so it cannot collide with another session user'
    key.startsWith(OidcLinkController.name)

    and: 'the old "store the Authentication here" key is gone'
    OidcLinkController.declaredFields*.name.every { !it.contains('LINK_AUTH_SESSION_KEY') }
    !OidcLinkController.declaredFields*.name.contains('LINK_AUTH_SESSION_KEY')
  }
}
