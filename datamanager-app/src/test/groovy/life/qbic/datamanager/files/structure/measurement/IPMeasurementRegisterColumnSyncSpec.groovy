package life.qbic.datamanager.files.structure.measurement

import life.qbic.datamanager.files.structure.ExampleProvider.Helper
import spock.lang.Specification

/**
 * Test to ensure both IPMeasurementRegisterColumn enums (in datamanager-app and
 * project-management-infrastructure) remain in sync after any changes.
 *
 * Since the two modules cannot import each other's enums at compile time, this test
 * uses reflection to load the infrastructure enum class from the classpath and compare
 * it with the parsing enum in this module.
 */
class IPMeasurementRegisterColumnSyncSpec extends Specification {

  def "both IPMeasurementRegisterColumn enums are in sync"() {
    given: "both enum classes loaded"
    def parsingEnum = IPMeasurementRegisterColumn.values()

    // Load infrastructure enum via reflection - it should be available on classpath
    def infrastructureEnumClass = Class.forName(
        "life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.IPMeasurementRegisterColumn"
    )
    def templateEnum = infrastructureEnumClass.getEnumConstants()

    expect: "same number of enum values"
    parsingEnum.length == templateEnum.length

    and: "all enum values match in headerName, index, mandatory, and readOnly"
    for (int i = 0; i < parsingEnum.length; i++) {
      def parsing = parsingEnum[i]
      def template = templateEnum[i]

      assert parsing.headerName() == template.headerName():
          "Mismatch in headerName at index ${i}: parsing=${parsing.headerName()}, template=${template.headerName()}"

      assert parsing.index() == template.index():
          "Mismatch in index() at index ${i}: parsing=${parsing.index()}, template=${template.index()}"

      assert parsing.isMandatory() == template.isMandatory():
          "Mismatch in isMandatory() for ${parsing.name()}: parsing=${parsing.isMandatory()}, template=${template.isMandatory()}"

      assert parsing.isReadOnly() == template.isReadOnly():
          "Mismatch in isReadOnly() for ${parsing.name()}: parsing=${parsing.isReadOnly()}, template=${template.isReadOnly()}"
    }

    and: "example values and descriptions match for all columns"
    for (int i = 0; i < parsingEnum.length; i++) {
      def parsing = parsingEnum[i]
      def template = templateEnum[i]

      def parsingHelper = getFillHelpViaReflection(parsing)
      def templateHelper = getFillHelpViaReflection(template)

      assert parsingHelper.exampleValue() == templateHelper.exampleValue():
          "Mismatch in exampleValue() for ${parsing.name()}: parsing=${parsingHelper.exampleValue()}, template=${templateHelper.exampleValue()}"

      assert parsingHelper.description() == templateHelper.description():
          "Mismatch in description() for ${parsing.name()}: parsing='${parsingHelper.description()}', template='${templateHelper.description()}'"
    }
  }

  /**
   * Helper method to invoke getFillHelp() via reflection since the return types
   * are from different packages (different Helper classes).
   */
  private Helper getFillHelpViaReflection(Object enumValue) {
    try {
      def method = enumValue.getClass().getMethod("getFillHelp")
      def result = method.invoke(enumValue)
      // The Optional contains a Helper, but it's from a different package
      // We need to extract the values via reflection
      if (result instanceof Optional) {
        def optionalClass = result.getClass()
        def isPresentMethod = optionalClass.getMethod("isPresent")
        def isPresent = isPresentMethod.invoke(result)
        if (isPresent) {
          def getMethod = optionalClass.getMethod("get")
          def helper = getMethod.invoke(result)
          // Now extract exampleValue and description from the Helper
          def exampleValue = helper.getClass().getMethod("exampleValue").invoke(helper)
          def description = helper.getClass().getMethod("description").invoke(helper)
          return new Helper(exampleValue.toString(), description.toString())
        }
      }
      return new Helper("", "")
    } catch (Exception e) {
      throw new RuntimeException("Failed to get fill help via reflection: " + e.message, e)
    }
  }
}