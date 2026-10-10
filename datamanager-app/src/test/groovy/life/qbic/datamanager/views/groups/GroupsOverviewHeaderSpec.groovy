package life.qbic.datamanager.views.groups

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit tests for the {@link GroupsOverviewHeader} identity block at the top of the Groups hub.
 *
 * <p>The header renders plain DOM (no Vaadin service is required), so its structure and the live
 * count line can be asserted directly. The count is informational and reflects the caller's own
 * memberships only.</p>
 */
class GroupsOverviewHeaderSpec extends Specification {

  @Unroll
  def "count line reads '#expected' for total=#total owned=#owned"() {
    expect:
    GroupsOverviewHeader.countText(total, owned) == expected

    where:
    total | owned || expected
    0     | 0     || "No groups"
    1     | 0     || "1 group"
    1     | 1     || "1 group \u00b7 1 owned"
    3     | 0     || "3 groups"
    3     | 1     || "3 groups \u00b7 1 owned"
    2     | 2     || "2 groups \u00b7 2 owned"
  }

  def "the header exposes the title, the count and a labelled group"() {
    given:
    def header = new GroupsOverviewHeader(3, 1)

    expect: "the block is announced as one labelled group"
    header.element.getAttribute("role") == "group"
    header.element.getAttribute("aria-label") == "Groups"

    and: "the title and live count are present"
    header.titleForTest().text == "Groups"
    header.countForTest().text == "3 groups \u00b7 1 owned"
  }

  def "setCounts updates the live count in place"() {
    given:
    def header = new GroupsOverviewHeader(0, 0)

    when:
    header.setCounts(2, 0)

    then:
    header.countForTest().text == "2 groups"
  }

  def "a negative count is rejected"() {
    when:
    new GroupsOverviewHeader(-1, 0)

    then:
    thrown(IllegalArgumentException)
  }

  def "the owned count cannot exceed the total"() {
    when:
    new GroupsOverviewHeader(1, 2)

    then:
    thrown(IllegalArgumentException)
  }
}