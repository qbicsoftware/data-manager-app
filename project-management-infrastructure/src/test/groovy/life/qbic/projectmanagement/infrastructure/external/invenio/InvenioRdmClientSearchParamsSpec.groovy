package life.qbic.projectmanagement.infrastructure.external.invenio

import life.qbic.projectmanagement.application.associated_dataset.DatasetAccessFilter
import spock.lang.Specification

/**
 * Unit tests for {@link InvenioRdmClient.SearchParams} with the
 * {@code accessFilter} field and its wire-value translation.
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
    params.accessFilterWireValue() == null
  }

  def "SearchParams full constructor preserves accessFilter"() {
    when:
    def params = new InvenioRdmClient.SearchParams(
        "test", 1, 10, DatasetAccessFilter.RESTRICTED)

    then:
    params.accessFilter() == DatasetAccessFilter.RESTRICTED
  }

  def "SearchParams full constructor accepts null accessFilter"() {
    when:
    def params = new InvenioRdmClient.SearchParams("test", 1, 10, null)

    then:
    params.accessFilter() == null
  }

  def "accessFilterWireValue maps RESTRICTED to restricted and PUBLIC to open"() {
    expect:
    new InvenioRdmClient.SearchParams(
        "test", 1, 10, DatasetAccessFilter.RESTRICTED).accessFilterWireValue() == "restricted"
    new InvenioRdmClient.SearchParams(
        "test", 1, 10, DatasetAccessFilter.PUBLIC).accessFilterWireValue() == "open"
  }

  def "SearchParams clamps size to 25"() {
    when:
    def params = new InvenioRdmClient.SearchParams(
        "test", 1, 100, DatasetAccessFilter.RESTRICTED)

    then:
    params.size() == 25
  }

  def "SearchParams rejects page less than 1"() {
    when:
    new InvenioRdmClient.SearchParams(
        "test", 0, 10, DatasetAccessFilter.RESTRICTED)

    then:
    thrown(IllegalArgumentException)
  }

  def "SearchParams rejects non-positive size"() {
    when:
    new InvenioRdmClient.SearchParams(
        "test", 1, 0, DatasetAccessFilter.RESTRICTED)

    then:
    thrown(IllegalArgumentException)
  }
}