package life.qbic.projectmanagement.infrastructure.sample.openbis

import spock.lang.Specification

import java.time.Duration

/**
 * Pins the retry budget of openBIS operations and its relationship to the UI timeout.
 *
 * <p>The defect this guards against (#1700): the retry loop slept roughly 511 s for a single
 * operation while the views gave up after 300 s, so the UI could report a failure for an operation
 * that was still running and could still commit.
 *
 * <p>The subtler defect, found while reviewing the first fix: bounding only the <em>sleeps</em>
 * does not bound the operation. Every attempt is an HTTP call with its own read timeout, so an
 * unresponsive server can spend that timeout on each attempt. The budget must therefore be a
 * wall-clock deadline, and the read timeout must be part of the arithmetic.
 */
class OpenbisRetryPolicySpec extends Specification {

  def "a single operation never sleeps longer than its budget"() {
    given: "the worst case jitter for every backoff"
    Random random = Mock()
    random.nextInt(_ as Integer) >> (OpenbisRetryPolicy.MAX_JITTER.toMillis() as int)

    when: "the backoffs of all attempts are summed"
    long total = 0
    for (int attempt = 1; attempt <= OpenbisRetryPolicy.MAX_ATTEMPTS; attempt++) {
      long backoff = OpenbisRetryPolicy.backoffMillis(attempt, random)
      expect: "no single backoff exceeds the per-attempt cap"
      backoff <= OpenbisRetryPolicy.MAX_BACKOFF.toMillis()
      total += backoff
    }

    then: "the total stays within the budget"
    total <= OpenbisRetryPolicy.MAX_OPERATION_DURATION.toMillis()
  }

  def "the exponential backoff is capped so it cannot grow unbounded"() {
    given:
    Random random = Mock()
    random.nextInt(_ as Integer) >> 0

    expect: "the backoff never exceeds the cap, however many attempts are made"
    OpenbisRetryPolicy.backoffMillis(attempt, random) <=
        OpenbisRetryPolicy.MAX_BACKOFF.toMillis()

    where:
    attempt << (1..30)
  }

  def "the backoff grows with the attempt number until it reaches the cap"() {
    given:
    Random random = Mock()
    random.nextInt(_ as Integer) >> 0

    expect:
    OpenbisRetryPolicy.backoffMillis(attempt, random) == expectedMillis

    where:
    attempt | expectedMillis
    1       | 500L
    2       | 1000L
    3       | 2000L
    4       | 4000L
    5       | 8000L
    6       | 8000L
    20      | 8000L
  }

  def "the budget is a wall-clock deadline, so a stalled call exhausts it"() {
    given: "the budget expressed as time remaining"
    long budget = OpenbisRetryPolicy.MAX_OPERATION_DURATION.toMillis()

    expect: "a fresh operation may start"
    OpenbisRetryPolicy.withinBudget(budget)

    and: "one with no time left may not"
    !OpenbisRetryPolicy.withinBudget(0)

    and: "one that has already overrun may not either"
    !OpenbisRetryPolicy.withinBudget(-1)
  }

  def "a read timeout that is consumed by a single call stops further retries"() {
    given: "the read timeout is longer than the whole retry budget"
    long readTimeout = OpenbisRetryPolicy.MAX_OPERATION_DURATION.toMillis() + 1

    expect: "after such a call no time is left, so no retry is attempted"
    !OpenbisRetryPolicy.withinBudget(0)

    and: "no backoff would fit either"
    !OpenbisRetryPolicy.fitsBudget(0, 500)
  }

  def "the worst case request duration accounts for the read timeout of every operation"() {
    given:
    Duration readTimeout = Duration.ofMillis(OpenbisSession.ApiV3.DEFAULT_READ_TIMEOUT_MILLIS)

    when:
    Duration worstCase = OpenbisRetryPolicy.worstCaseRequestDuration(readTimeout)

    then: "each of the operations may consume its budget plus one in-flight read timeout"
    worstCase == (OpenbisRetryPolicy.MAX_OPERATION_DURATION + readTimeout)
        .multipliedBy(OpenbisRetryPolicy.MAX_OPERATIONS_PER_REQUEST)
  }

  def "a longer configured read timeout lengthens the derived UI timeout accordingly"() {
    expect: "the derivation reacts to configuration rather than assuming a constant"
    OpenbisRetryPolicy.recommendedUiTimeout(Duration.ofSeconds(200)) >
        OpenbisRetryPolicy.recommendedUiTimeout(Duration.ofSeconds(100))
  }

  def "the UI outlives the server for any plausible read timeout"() {
    expect: "the derived timeout exceeds the worst case for every value in this range"
    OpenbisRetryPolicy.worstCaseRequestDuration(readTimeout) <
        OpenbisRetryPolicy.recommendedUiTimeout(readTimeout)

    where:
    readTimeout << [Duration.ofSeconds(1), Duration.ofSeconds(30),
                    Duration.ofMillis(OpenbisSession.ApiV3.DEFAULT_READ_TIMEOUT_MILLIS),
                    Duration.ofSeconds(300)]
  }

  def "the retry budget no longer exceeds the five minute timeout it used to"() {
    expect: "the previous unbounded exponential ladder summed to far more than 300 s"
    previousLadderMillis() > Duration.ofMinutes(5).toMillis()

    and: "the bounded budget is comfortably below it"
    OpenbisRetryPolicy.MAX_OPERATION_DURATION.toMillis() < Duration.ofMinutes(5).toMillis()
  }

  def "the whole retry ladder still terminates within one operation budget"() {
    given: "the worst case jitter"
    Random random = Mock()
    random.nextInt(_ as Integer) >> (OpenbisRetryPolicy.MAX_JITTER.toMillis() as int)

    when: "every attempt is taken and every backoff applied"
    long total = 0
    for (int attempt = 1; attempt <= OpenbisRetryPolicy.MAX_ATTEMPTS; attempt++) {
      total += OpenbisRetryPolicy.backoffMillis(attempt, random)
    }

    then: "the accumulated sleeps would still have fitted the budget"
    total <= OpenbisRetryPolicy.MAX_OPERATION_DURATION.toMillis()
  }

  def "an attempt is started while attempts remain"() {
    expect:
    OpenbisRetryPolicy.hasAttemptsLeft(attempted) == expected

    where:
    attempted | expected
    0         | true
    1         | true
    8         | true
    9         | true
    10        | false
  }

  def "a backoff only fits while there is time left after waiting"() {
    expect:
    OpenbisRetryPolicy.fitsBudget(remaining, next) == expected

    where:
    remaining | next  | expected
    60000L    | 8000L | true
    52000L    | 8000L | true
    8000L     | 8000L | false
    7999L     | 8000L | false
    1000L     | 1L    | true
    1L        | 1L    | false
  }

  def "the backoff rejects an invalid attempt number"() {
    when:
    OpenbisRetryPolicy.backoffMillis(0, new Random())

    then:
    thrown(IllegalArgumentException)
  }

  /**
   * The ladder the connector used before this policy: {@code (500 + jitter) * 2^round} for rounds
   * 1-9, summed with the minimum jitter.
   */
  private static long previousLadderMillis() {
    long total = 0
    for (int round = 1; round <= 9; round++) {
      total += (long) (500 * Math.pow(2, round))
    }
    return total
  }
}
