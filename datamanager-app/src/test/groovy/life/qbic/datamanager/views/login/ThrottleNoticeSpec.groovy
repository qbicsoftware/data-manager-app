package life.qbic.datamanager.views.login

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

/**
 * <b>Regression: a throttled user must be told, and told something they can act on (MFA-68, UX)</b>
 *
 * <p>The first version of the throttle was silent by design: it reused the challenge's uniform "that
 * code was not accepted" sentence, on the theory that distinguishing a rate limit from a wrong code
 * helps an attacker (AUTH-NFR-03). That reasoning belongs to the <em>password</em> login, which is
 * reachable anonymously. Here it protects nothing — reaching the limit requires this account's correct
 * password — and it costs the user everything: they keep typing codes from an app that is working
 * perfectly, and are told each time that the code is wrong. That is not a rate limit experienced as a
 * rate limit; it is a broken page.</p>
 *
 * <p>So the refusal is described plainly, with a duration and without alarming the user into thinking
 * their account is gone. These cases pin the three things the sentence must carry, plus the one thing
 * it must not.</p>
 */
class ThrottleNoticeSpec extends Specification {

  // ---------------------------------------------------------------- the sentence

  def "the notice says what happened and how long it lasts"() {
    given:
    def message = SecondFactorView.throttledMessage(Duration.ofMinutes(15))

    expect: "a rate limit stated as a rate limit"
    message.toLowerCase().contains("too many attempts")

    and: "a duration a person can read, not an ISO-8601 token or a raw second count"
    message.contains("15 minutes")
    !message.contains("PT")
    !message.contains("900")
  }

  def "the notice says the account is not permanently locked out"() {
    given:
    def message = SecondFactorView.throttledMessage(Duration.ofMinutes(15)).toLowerCase()

    expect: "the two things a worried user needs to hear"
    message.contains("still work")
    message.contains("try again")
  }

  def "the notice does not imply the credentials were wrong"() {
    given:
    def message = SecondFactorView.throttledMessage(Duration.ofMinutes(15)).toLowerCase()

    expect: "the authenticator is not blamed for a limit the account hit"
    !message.contains("invalid code")
    !message.contains("not accepted")
    !message.contains("wrong code")
  }

  @Unroll
  def "a #duration block is described as '#expected'"() {
    expect:
    SecondFactorView.describeDuration(duration) == expected

    where:
    duration                | expected
    Duration.ofMinutes(1)   | "1 minute"
    Duration.ofMinutes(15)  | "15 minutes"
    Duration.ofMinutes(59)  | "59 minutes"
    Duration.ofMinutes(60)  | "1 hour"
    Duration.ofHours(2)     | "2 hours"
    Duration.ofMinutes(90)  | "1 hour 30 minutes"
    Duration.ofMinutes(120) | "2 hours"
  }

  def "a duration shorter than a minute is still described as something"() {
    expect: "never '0 minutes', which reads as 'no wait at all'"
    SecondFactorView.describeDuration(Duration.ofSeconds(30)) == "1 minute"
  }
}
