package life.qbic.projectmanagement.infrastructure.template.provider.openxml.column

import spock.lang.Specification

/**
 * Test to ensure all IPMeasurementRegisterColumn descriptions fit within POI's
 * 255 character limit for XSSFDataValidation prompt boxes.
 *
 * This prevents future description additions from causing template download crashes.
 */
class IPMeasurementRegisterColumnDescriptionLengthSpec extends Specification {

  def "all column descriptions are within POI's 255 character limit"() {
    when: "all descriptions are collected"
    def violations = IPMeasurementRegisterColumn.values().findAll { col ->
      def desc = col.getFillHelp().map { it.description() }.orElse("")
      desc.length() > 255
    }

    then: "no description exceeds 255 characters"
    violations.isEmpty()
  }

  def "each column description length can be verified explicitly"() {
    expect: "every description is 255 chars or less"
    for (IPMeasurementRegisterColumn column : IPMeasurementRegisterColumn.values()) {
      def desc = column.getFillHelp().map { it.description() }.orElse("")
      assert desc.length() <= 255 :
          "Description for ${column.name()} is ${desc.length()} chars (max 255): ${desc}"
    }
  }
}