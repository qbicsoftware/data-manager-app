package life.qbic.projectmanagement.infrastructure.sample.openbis

import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.function.LongSupplier

/**
 * Behaviour of the openBIS retry <em>loop</em>, with a controllable clock and sleeper.
 *
 * <p>Both the original defect (#1700: ~511 s of sleeping behind a 300 s UI timeout) and its first
 * incomplete fix (bounding sleeps while ignoring the per-call read timeout) lived in this loop.
 * Neither was covered by a test, because reaching it required a live openBIS server.
 */
class OpenbisRetryExecutionSpec extends Specification {

  /** A clock that advances only when the sleeper is used, so tests run instantly. */
  static class FakeTime implements LongSupplier {

    AtomicLong nanos = new AtomicLong(0)

    @Override
    long getAsLong() {
      return nanos.get()
    }

    void advance(long millis) {
      nanos.addAndGet(millis * 1_000_000L)
    }
  }

  FakeTime clock = new FakeTime()
  List<Long> sleeps = []
  List<Integer> failedAttempts = []
  Random random = Mock()

  def setup() {
    // deterministic: minimum jitter
    random.nextInt(_ as Integer) >> 0
  }

  def "a succeeding attempt is executed exactly once and never retried"() {
    given:
    def calls = new AtomicInteger(0)

    when:
    OpenbisRetryPolicy.execute({ calls.incrementAndGet() }, random, clock, this::sleep, this::record)

    then:
    calls.get() == 1
    sleeps.isEmpty()
    failedAttempts.isEmpty()
  }

  def "a transient failure is retried and then succeeds"() {
    given: "the first two attempts fail"
    def calls = new AtomicInteger(0)

    when:
    OpenbisRetryPolicy.execute({
      if (calls.incrementAndGet() <= 2) {
        throw new RuntimeException("transient")
      }
    }, random, clock, this::sleep, this::record)

    then: "three attempts were made and two failures reported"
    calls.get() == 3
    failedAttempts == [1, 2]

    and: "only the two retries waited"
    sleeps.size() == 2
  }

  def "the loop stops when the attempts are exhausted, keeping the last cause"() {
    given: "an operation that always fails"
    def calls = new AtomicInteger(0)

    when:
    OpenbisRetryPolicy.execute({
      calls.incrementAndGet()
      throw new IllegalStateException("always fails")
    }, random, clock, this::sleep, this::record)

    then: "the failure surfaces with its cause preserved"
    def e = thrown(OpenbisRetryPolicy.RetriesExhaustedException)
    e.cause.message == "always fails"

    and: "no more than the allowed number of attempts were made"
    calls.get() <= OpenbisRetryPolicy.MAX_ATTEMPTS
  }

  def "a stalled call consumes the budget so no further attempt is started"() {
    given: "every attempt consumes the whole operation budget before failing, as a read timeout does"
    def calls = new AtomicInteger(0)
    def stallMillis = OpenbisRetryPolicy.MAX_OPERATION_DURATION.toMillis()

    when:
    OpenbisRetryPolicy.execute({
      calls.incrementAndGet()
      clock.advance(stallMillis)
      throw new RuntimeException("read timeout")
    }, random, clock, this::sleep, this::record)

    then: "the failure is reported"
    thrown(OpenbisRetryPolicy.RetriesExhaustedException)

    and: "exactly one attempt was made: the stalled call used the entire budget"
    calls.get() == 1

    and: "nothing was slept after it"
    sleeps.isEmpty()
  }

  def "a slow but not exhausted operation stops before the deadline"() {
    given: "each failed attempt burns a quarter of the budget"
    def calls = new AtomicInteger(0)
    def perCallMillis = (OpenbisRetryPolicy.MAX_OPERATION_DURATION.toMillis() / 4) as long

    when:
    OpenbisRetryPolicy.execute({
      calls.incrementAndGet()
      clock.advance(perCallMillis)
      throw new RuntimeException("slow failure")
    }, random, clock, this::sleep, this::record)

    then: "the failure is reported"
    thrown(OpenbisRetryPolicy.RetriesExhaustedException)

    and: "the operation stopped within its budget plus one in-flight call"
    clock.getAsLong() / 1_000_000L <=
        OpenbisRetryPolicy.MAX_OPERATION_DURATION.toMillis() + perCallMillis

    and: "it did not run all ten attempts"
    calls.get() < OpenbisRetryPolicy.MAX_ATTEMPTS
  }

  def "the total time never exceeds the budget plus one in-flight call"() {
    given: "attempts that burn half a budget each"
    def perCallMillis = (OpenbisRetryPolicy.MAX_OPERATION_DURATION.toMillis() / 2) as long

    when:
    OpenbisRetryPolicy.execute({
      clock.advance(perCallMillis)
      throw new RuntimeException("failure")
    }, random, clock, this::sleep, this::record)

    then:
    thrown(OpenbisRetryPolicy.RetriesExhaustedException)

    and: "wall-clock time is bounded by the budget plus the last call"
    clock.getAsLong() / 1_000_000L <=
        OpenbisRetryPolicy.MAX_OPERATION_DURATION.toMillis() + perCallMillis
  }

  def "no wait is started that would overrun the deadline"() {
    given: "the backoffs sum to more than the budget would allow"
    def calls = new AtomicInteger(0)

    when:
    OpenbisRetryPolicy.execute({
      calls.incrementAndGet()
      throw new RuntimeException("failure")
    }, random, clock, this::sleep, this::record)

    then:
    thrown(OpenbisRetryPolicy.RetriesExhaustedException)

    and: "every sleep that was performed fitted the budget"
    (sleeps ? sleeps.sum() : 0L) <= OpenbisRetryPolicy.MAX_OPERATION_DURATION.toMillis()
  }

  private void sleep(long millis) {
    sleeps << millis
    clock.advance(millis)
  }

  private void record(RuntimeException failure, int attemptNumber) {
    failedAttempts << attemptNumber
  }
}
