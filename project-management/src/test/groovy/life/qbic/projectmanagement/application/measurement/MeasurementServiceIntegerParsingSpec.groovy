package life.qbic.projectmanagement.application.measurement

import spock.lang.Specification

/**
 * Tests for the MeasurementService helper methods that parse measurement metadata values.
 *
 * <p>These tests verify that values extracted from Excel files (which may be stored as
 * numeric cells) are correctly converted to the expected Java types.</p>
 */
class MeasurementServiceIntegerParsingSpec extends Specification {

  def "parseIntegerOrNull handles plain integer strings"() {
    when:
    def result = invokeParseIntegerOrNull("120")

    then:
    result == 120
  }

  def "parseIntegerOrNull handles Excel numeric formatting with .0 suffix"() {
    // Excel stores whole numbers in numeric cells; XLSXParser reads them as
    // Double.toString(cell.getNumericCellValue()) which yields "120.0"
    when:
    def result = invokeParseIntegerOrNull("120.0")

    then:
    result == 120
  }

  def "parseIntegerOrNull rejects true decimal values"() {
    when:
    def result = invokeParseIntegerOrNull("120.5")

    then:
    result == null
  }

  def "parseIntegerOrNull returns null for blank input"() {
    when:
    def result = invokeParseIntegerOrNull("")

    then:
    result == null
  }

  def "parseIntegerOrNull returns null for non-numeric input"() {
    when:
    def result = invokeParseIntegerOrNull("not-a-number")

    then:
    result == null
  }

  private static Integer invokeParseIntegerOrNull(String value) {
    def method = MeasurementService.getDeclaredMethod("parseIntegerOrNull", String)
    method.setAccessible(true)
    return method.invoke(null, value) as Integer
  }
}
