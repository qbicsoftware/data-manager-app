package life.qbic.projectmanagement.infrastructure.external.invenio

import spock.lang.Specification

/**
 * Unit tests for {@link InvenioRdmClient.SearchParams} with the
 * {@code accessFilter} field.
 *
 * @since 1.12.0
 */
class InvenioRdmClientSearchParamsSpec extends Specification {

  def "SearchParams backward-compatible constructor sets accessFilter to null"() {
    when:
    def params = new InvenioRdmClient.SearchParams("test", 1, 10)

    then:
    params.query() == "test"
    params.page() == 1
    params.size() == 10
    params.accessFilter() == null
  }

  def "SearchParams full constructor preserves accessFilter"() {
    when:
    def params = new InvenioRdmClient.SearchParams("test", 1, 10, "restricted")

    then:
    params.accessFilter() == "restricted"
  }

  def "SearchParams full constructor accepts null accessFilter"() {
    when:
    def params = new InvenioRdmClient.SearchParams("test", 1, 10, null)

    then:
    params.accessFilter() == null
  }

  def "SearchParams clamps size to 25"() {
    when:
    def params = new InvenioRdmClient.SearchParams("test", 1, 100, "restricted")

    then:
    params.size() == 25
  }

  def "SearchParams rejects page less than 1"() {
    when:
    new InvenioRdmClient.SearchParams("test", 0, 10, "restricted")

    then:
    thrown(IllegalArgumentException)
  }

  def "SearchParams rejects non-positive size"() {
    when:
    new InvenioRdmClient.SearchParams("test", 1, 0, "restricted")

    then:
    thrown(IllegalArgumentException)
  }
}
