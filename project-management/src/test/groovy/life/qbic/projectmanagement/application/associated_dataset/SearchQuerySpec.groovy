package life.qbic.projectmanagement.application.associated_dataset

import spock.lang.Specification

/**
 * Unit tests for {@link SearchQuery}.
 *
 * <p>Verifies the accessFilter field, backward-compatible constructors,
 * and factory methods.</p>
 *
 * @since 1.12.0
 */
class SearchQuerySpec extends Specification {

  def "backward-compatible constructor sets accessFilter to null"() {
    when:
    def query = new SearchQuery("proteomics", 0, 10)

    then:
    query.query() == "proteomics"
    query.page() == 0
    query.pageSize() == 10
    query.accessFilter() == null
  }

  def "full constructor preserves accessFilter value"() {
    when:
    def query = new SearchQuery("proteomics", 0, 10, "restricted")

    then:
    query.accessFilter() == "restricted"
  }

  def "full constructor accepts null accessFilter"() {
    when:
    def query = new SearchQuery("test", 0, 25, null)

    then:
    query.accessFilter() == null
  }

  def "effectiveQuery returns empty string for null query"() {
    given:
    def query = new SearchQuery(null, 0, 10, "restricted")

    expect:
    query.effectiveQuery() == ""
  }

  def "effectiveQuery returns empty string for blank query"() {
    given:
    def query = new SearchQuery("   ", 0, 10, "restricted")

    expect:
    query.effectiveQuery() == ""
  }

  def "effectiveQuery returns query as-is when non-blank"() {
    given:
    def query = new SearchQuery("proteomics", 0, 10, "restricted")

    expect:
    query.effectiveQuery() == "proteomics"
  }

  def "listAll factory creates query with no filter"() {
    when:
    def query = SearchQuery.listAll(0, 25)

    then:
    query.query() == ""
    query.page() == 0
    query.pageSize() == 25
    query.accessFilter() == null
  }

  def "listAll factory with accessFilter creates filtered query"() {
    when:
    def query = SearchQuery.listAll(0, 25, "restricted")

    then:
    query.query() == ""
    query.page() == 0
    query.pageSize() == 25
    query.accessFilter() == "restricted"
  }

  def "constructor rejects negative page"() {
    when:
    new SearchQuery("test", -1, 10)

    then:
    thrown(IllegalArgumentException)
  }

  def "constructor rejects non-positive pageSize"() {
    when:
    new SearchQuery("test", 0, 0)

    then:
    thrown(IllegalArgumentException)
  }

  def "constructor with accessFilter rejects negative page"() {
    when:
    new SearchQuery("test", -1, 10, "restricted")

    then:
    thrown(IllegalArgumentException)
  }
}
