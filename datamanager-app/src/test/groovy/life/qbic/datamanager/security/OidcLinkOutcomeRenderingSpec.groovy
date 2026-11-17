package life.qbic.datamanager.security

import life.qbic.identity.api.OidcLinkOutcomeMessage
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The link outcome must reach the user as a sentence, and never through the URL.</b>
 *
 * <p>Two defects motivate this spec.</p>
 *
 * <ol>
 *   <li><b>The message was a flag.</b> Every failure surfaced as "Linking your profile failed: 1.",
 *       because the redirect put a level token where the view expected a sentence.</li>
 *   <li><b>The message was injectable.</b> That sentence travelled in {@code ?reason=}, so anyone could
 *       craft {@code /settings/profile?error=1&reason=<their text>} and have the application render
 *       their words in a trusted notification on its own domain — a ready-made pretext for a phishing
 *       step.</li>
 * </ol>
 *
 * <p>The outcome is therefore an enum carried in the session, read-and-removed by the settings page, and
 * worded in the message bundle. These assertions pin all three parts, plus the retry behaviour that
 * makes the feature feel stuck if it is wrong.</p>
 */
class OidcLinkOutcomeRenderingSpec extends Specification {

  static final String MESSAGES = 'datamanager-app/src/main/resources/messages/toast-notifications.properties'
  static final String FILTER =
      'datamanager-app/src/main/java/life/qbic/datamanager/security/OidcLinkCompletionFilter.java'
  static final String VIEW =
      'datamanager-app/src/main/java/life/qbic/datamanager/views/account/UserProfileMain.java'
  static final String CONTROLLER =
      'datamanager-app/src/main/java/life/qbic/datamanager/security/OidcLinkController.java'

  private static String text(String relative) {
    def candidates = [new File(relative), new File("../$relative")]
    def file = candidates.find { it.exists() }
    assert file != null: "$relative not found from ${new File('.').absolutePath}"
    return file.text
  }

  private static Properties messages() {
    def properties = new Properties()
    def candidates = [new File(MESSAGES), new File("../$MESSAGES")]
    def file = candidates.find { it.exists() }
    assert file != null: "$MESSAGES not found"
    file.withInputStream { properties.load(it) }

    return properties
  }

  // ---- nothing user-visible travels in the URL ------------------------------------------------

  def "the filter puts no message text in the redirect"() {
    given: 'the filter with its comments stripped, so the assertions are about code and not prose'
    def code = text(FILTER)
        .readLines()
        .findAll { !it.strip().startsWith('*') && !it.strip().startsWith('//') }
        .join('\n')

    expect: 'no query parameter carries a message'
    !code.contains('reason=')
    !code.contains('append("&reason')

    and: 'the outcome is stored server-side instead'
    code.contains('OidcLinkController.OUTCOME')
  }

  def "the view no longer reads the outcome from query parameters"() {
    given:
    def source = text(VIEW)

    expect:
    !source.contains('getSingleParameter("reason")')
    !source.contains('getSingleParameter("error")')
    !source.contains('getSingleParameter("success")')
  }

  // ---- one message per outcome, worded in the bundle ------------------------------------------

  @Unroll
  def "every outcome has a message: #outcome"() {
    given:
    def properties = messages()

    expect: 'an outcome without wording would render an empty toast'
    properties.getProperty("profile.oidc.link.${outcome.messageKey()}.message.text") != null
    properties.getProperty("profile.oidc.link.${outcome.messageKey()}.level") != null

    and: 'the level matches whether the link actually exists'
    (properties.getProperty("profile.oidc.link.${outcome.messageKey()}.level") == 'success')
        == outcome.isSuccess()

    where:
    outcome << OidcLinkOutcomeMessage.values().toList()
  }

  def "messages are rendered as text, never as markup"() {
    given:
    def properties = messages()

    expect: 'a sentence must not be interpreted as HTML'
    OidcLinkOutcomeMessage.values().every {
      properties.getProperty("profile.oidc.link.${it.messageKey()}.message.type") == 'text'
    }
  }

  def "the dead-end refusal tells the user what to do next"() {
    given:
    def message = messages()
        .getProperty('profile.oidc.link.held_by_another_account.message.text')

    expect: 'this outcome is reachable, and a refusal with no way forward is a bug'
    message.contains('disconnect the iD there')
    message.contains('support@qbic.zendesk.com')
  }

  def "the success wording describes a record link, not a login method"() {
    given:
    def message = messages().getProperty('profile.oidc.link.linked.message.text')

    expect: 'the copy matches what the feature does (AUTH-R-07)'
    message.toLowerCase().contains('connected')
    !message.toLowerCase().contains('sign in')
    !message.toLowerCase().contains('openid')
  }

  // ---- the retry behaviour that makes it feel stuck -------------------------------------------

  def "starting a new attempt clears any outcome left from the previous one"() {
    // The exact scenario: a failed attempt, then a retry that succeeds. If the old outcome survives,
    // the user is told the failure again — or sees a success banner for an attempt still in flight.
    given:
    def source = text(CONTROLLER)

    expect:
    source.contains('session.removeAttribute(OUTCOME)')
  }

  def "the outcome is removed before it is rendered, so a refresh does not replay it"() {
    given:
    def source = text(VIEW)
    def readIndex = source.indexOf('getAttribute(OidcLinkController.OUTCOME)')
    def removeIndex = source.indexOf('setAttribute(OidcLinkController.OUTCOME, null)')

    expect:
    readIndex >= 0 && removeIndex > readIndex

    and: 'read-and-remove happens before the toast is built'
    removeIndex < source.indexOf('messageFactory.toast(key')
  }

  def "a missing message key cannot break the settings page"() {
    given:
    def source = text(VIEW)

    expect: 'the notification is guarded, because the account state it renders is still correct'
    source.contains('Could not render the ORCID link outcome')
  }
}
