package life.qbic.projectmanagement.infrastructure.query

import spock.lang.Specification

/**
 * Covers the read model's split of the aggregated analysis-method column.
 *
 * <p>The query aggregates a measurement's methods into a delimited string
 * ({@code WGS,RNA-SEQ}), because a measurement made from samples carrying different analysis
 * methods is attributed to each of them (ADR 0010 decision 4). Splitting that string is the only
 * place the view parses SQL output, so it is pinned here rather than left to the JDBC row mapper —
 * which cannot be exercised without a database.</p>
 */
class MethodListSpec extends Specification {

  def "a single method becomes a one-element list"() {
    expect:
    ProjectPageReadRepositoryImpl.methodList("WGS") == ["WGS"]
  }

  def "several methods are split and kept in the order the query wrote them"() {
    expect:
    ProjectPageReadRepositoryImpl.methodList("WGS,RNA-SEQ") == ["WGS", "RNA-SEQ"]
    ProjectPageReadRepositoryImpl.methodList("WES,16S,RNA-SEQ") == ["WES", "16S", "RNA-SEQ"]
  }

  /** The aggregate is a SQL string, so its spacing is whatever the query produced, not a contract. */
  def "surrounding whitespace on each method is trimmed"() {
    expect:
    ProjectPageReadRepositoryImpl.methodList("WGS , RNA-SEQ") == ["WGS", "RNA-SEQ"]
  }

  /**
   * A measurement with no method annotation is a real state, and it must not become a list holding
   * one empty string — the UI would then render a blank method cell rather than no method.
   */
  def "empty segments are dropped rather than kept as blank methods"() {
    expect:
    ProjectPageReadRepositoryImpl.methodList("WGS,,RNA-SEQ") == ["WGS", "RNA-SEQ"]
    ProjectPageReadRepositoryImpl.methodList("WGS,") == ["WGS"]
    ProjectPageReadRepositoryImpl.methodList(",WGS") == ["WGS"]
  }

  def "no aggregated column at all yields no methods"() {
    expect:
    ProjectPageReadRepositoryImpl.methodList(null) == []
    ProjectPageReadRepositoryImpl.methodList("") == []
    ProjectPageReadRepositoryImpl.methodList("   ") == []
  }

  /** Methods are ontology-free controlled vocabulary terms, so only the delimiter splits them. */
  def "a hyphenated or numeric method label stays one value"() {
    expect:
    ProjectPageReadRepositoryImpl.methodList("RNA-SEQ") == ["RNA-SEQ"]
    ProjectPageReadRepositoryImpl.methodList("16S") == ["16S"]
  }
}
