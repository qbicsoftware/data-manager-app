package life.qbic.usergroups.domain.model

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests for the {@link GroupName} value object.
 */
class GroupNameSpec extends Specification {

  @Unroll
  def "A blank, null or whitespace-only group name is rejected"() {
    when:
    GroupName.from(name)

    then:
    thrown(GroupName.GroupNameValidationException)

    where:
    name << [null, "", "   ", "\t\n"]
  }

  @Unroll
  def "A group name longer than 80 characters is rejected"() {
    when:
    GroupName.from(name)

    then:
    thrown(GroupName.GroupNameValidationException)

    where:
    name = "x" * 81
  }

  def "A valid group name is trimmed and preserved"() {
    expect:
    GroupName.from("  NGS Lab  ").value() == "NGS Lab"
  }

  def "A group name of exactly 80 characters is accepted"() {
    given:
    String name80 = "x" * 80

    expect:
    GroupName.from(name80).value() == name80
  }

  def "Two group names with equal values are equal and have the same hash"() {
    expect:
    GroupName.from("NGS Lab") == GroupName.from("NGS Lab")
    GroupName.from("NGS Lab").hashCode() == GroupName.from("NGS Lab").hashCode()
  }

  def "Two group names with different values are not equal"() {
    expect:
    GroupName.from("NGS Lab") != GroupName.from("Proteomics Lab")
  }
}