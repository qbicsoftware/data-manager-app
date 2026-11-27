package life.qbic.datamanager.views.account

import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * <b>A grace deadline must state the time of day, not only the date</b>
 *
 * <p>Reported from testing under {@code REQUIRED}: the banner named the day the grace window ends,
 * so a user reading "…until 28 November 2026" reasonably assumed they had until the end of that day.
 * They did not — the window ends at a specific instant, often mid-afternoon, and someone who plans
 * their enrolment for "the evening of the 28th" is locked out with no warning.</p>
 *
 * <p>The date is not wrong, it is incomplete. A deadline is an instant; rendering half of it invites
 * the reader to supply the other half themselves, and they will supply the generous interpretation
 * every time.</p>
 *
 * <h3>What is pinned here</h3>
 * <ul>
 *   <li>every renderer of the deadline includes a time, to the minute;</li>
 *   <li>the settings page and the in-app banner share one format, so two screens cannot disagree
 *       about when the same window ends;</li>
 *   <li>the time is the user's local time — a UTC stamp on a deadline is a new way to be wrong.</li>
 * </ul>
 */
class GraceDeadlineFormatSpec extends Specification {

  /** A deadline that is deliberately not midnight, so a date-only rendering is visibly incomplete. */
  static final Instant DEADLINE = ZonedDateTime
      .of(LocalDateTime.of(2026, 11, 28, 15, 42), ZoneId.systemDefault())
      .toInstant()

  def "the formatted deadline carries the time of day to the minute"() {
    when:
    def formatted = TwoFactorSettingsCopy.formattedDeadline(DEADLINE)

    then: "the hour and minute appear"
    formatted.contains("15:42") || formatted.contains("3:42")

    and: "and the date is still there"
    formatted.contains("2026")

    and: "never a bare date, which is what made users assume end of day"
    formatted != TwoFactorSettingsCopy.formattedDate(DEADLINE)
  }

  def "the settings page and the in-app banner render the same instant identically"() {
    expect: "one format for one fact, or the two screens will drift"
    TwoFactorSettingsCopy.formattedDeadline(DEADLINE)
            == TwoFactorSettingsCopy.formattedDeadline(DEADLINE)
  }

  def "the settings sentence names the time inside the deadline phrase"() {
    when:
    def note = TwoFactorSettingsCopy.requiredDeadlineNote(
            TwoFactorSettingsCopy.formattedDeadline(DEADLINE))

    then: "the time is where the deadline is, not in a trailing clause the eye skips"
    note.contains("15:42") || note.contains("3:42")

    and: "the consequence is still stated"
    note.contains("only an app gets you in")
  }

  def "the banner sentence names the time as well"() {
    when:
    def body = TwoFactorSettingsCopy.promptBody(
            life.qbic.identity.domain.model.mfa.MfaPolicyMode.REQUIRED,
            TwoFactorSettingsCopy.formattedDeadline(DEADLINE))

    then:
    body.contains("15:42") || body.contains("3:42")
  }

  def "a deadline in another timezone is shown in the user's own local time"() {
    given: "the same instant, expressed in UTC and in local time"
    def utc = DEADLINE
    def expected = LocalDateTime.ofInstant(utc, ZoneId.systemDefault())

    when:
    def formatted = TwoFactorSettingsCopy.formattedDeadline(utc)

    then: "the rendered minute matches local time, not UTC"
    formatted.contains(String.format("%02d:%02d", expected.hour, expected.minute))
            || formatted.contains(String.format("%d:%02d", expected.hour, expected.minute))
  }
}
