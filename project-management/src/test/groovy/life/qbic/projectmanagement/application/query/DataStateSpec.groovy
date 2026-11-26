package life.qbic.projectmanagement.application.query

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Pins the one state vocabulary for data completeness.
 *
 * <p>This spec exists because the project page drifted into five dialects for one condition --
 * "Waiting for data", "Not complete yet", "Not attached", a percentage ring and "Registered" -- each
 * individually defensible and collectively unreadable. The enum is now the single source; this spec
 * pins the mapping so a surface cannot quietly reintroduce a fourth reading of the same axis.</p>
 *
 * @see DataState
 */
class DataStateSpec extends Specification {

  @Unroll
  def "#withData of #total measurements is #expected"() {
    expect:
    DataState.of(withData, total) == expected

    where:
    withData | total || expected
    0        | 0     || DataState.WAITING
    0        | 17    || DataState.WAITING
    5        | 17    || DataState.PARTIAL
    16       | 17    || DataState.PARTIAL
    17       | 17    || DataState.COMPLETE
    1        | 1     || DataState.COMPLETE
  }

  def "a dataset with no measurements is waiting, not complete"() {
    // Completeness is a property of data that exists. Calling an empty dataset complete is the
    // vacuous truth that makes an empty project look finished.
    expect:
    DataState.of(0, 0) == DataState.WAITING
    !DataState.of(0, 0).isComplete()
    DataState.of(0, 0).isWaiting()
  }

  def "partial is neither complete nor waiting"() {
    given:
    def partial = DataState.of(5, 17)

    expect:
    !partial.isComplete()
    !partial.isWaiting()
    !partial.isComplete() && !partial.isWaiting()
  }

  def "more data than measurements does not fall out of the vocabulary"() {
    // The read model counts distinct measurement records for coverage and distinct (type, id) pairs
    // for totals; a drift between them must not produce a state that has no name.
    expect:
    DataState.of(20, 17) == DataState.COMPLETE
  }

  def "at risk is reserved and is not produced by any count"() {
    // It exists so amber has a meaning that has not been spent on "not arrived yet". If a count ever
    // maps to it, that is a decision to be taken deliberately, not as a side effect.
    expect:
    (0..25).every { withData ->
      (1..25).every { total ->
        DataState.of(withData, total) != DataState.AT_RISK
      }
    }
  }

  def "every state has a distinct name"() {
    expect:
    DataState.values().toList().toSet().size() == DataState.values().length
  }
}
