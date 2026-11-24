package life.qbic.datamanager.files.structure

import life.qbic.projectmanagement.application.measurement.validation.MeasurementFieldLength
import life.qbic.projectmanagement.application.sample.SampleFieldLength
import spock.lang.Specification

/**
 * Pins each user-facing template hint to the limit its own concern validates.
 *
 * <p>The defect behind #1695 and #1706 was that the templates promised 500 characters while the
 * database columns held 255, and nothing validated either number. Validation alone would leave the
 * text free to drift back, so this test asserts every comment column advertises the limit of its
 * concern — sample, NGS, proteomics or immunopeptidomics.
 *
 * <p>The expected values intentionally come from four constants rather than one: the limits belong
 * to the concerns and may diverge in future.
 */
class CommentColumnLimitSpec extends Specification {

  private static final Map<String, Integer> COMMENT_COLUMN_ENUMS = [
      // sample concern, both copies of the parsing/template enums
      "life.qbic.datamanager.files.structure.sample.RegisterColumn": SampleFieldLength.COMMENT,
      "life.qbic.datamanager.files.structure.sample.EditColumn": SampleFieldLength.COMMENT,
      "life.qbic.datamanager.files.structure.sample.InformationColumn": SampleFieldLength.COMMENT,
      "life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.RegisterColumn": SampleFieldLength.COMMENT,
      "life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.EditColumn": SampleFieldLength.COMMENT,
      "life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.InformationColumn": SampleFieldLength.COMMENT,
      // NGS concern
      "life.qbic.datamanager.files.structure.measurement.NGSMeasurementRegisterColumn": MeasurementFieldLength.NGS_COMMENT,
      "life.qbic.datamanager.files.structure.measurement.NGSMeasurementEditColumn": MeasurementFieldLength.NGS_COMMENT,
      "life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.NGSMeasurementRegisterColumn": MeasurementFieldLength.NGS_COMMENT,
      "life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.NGSMeasurementEditColumn": MeasurementFieldLength.NGS_COMMENT,
      // proteomics concern
      "life.qbic.datamanager.files.structure.measurement.ProteomicsMeasurementRegisterColumn": MeasurementFieldLength.PROTEOMICS_COMMENT,
      "life.qbic.datamanager.files.structure.measurement.ProteomicsMeasurementEditColumn": MeasurementFieldLength.PROTEOMICS_COMMENT,
      "life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.ProteomicsMeasurementRegisterColumn": MeasurementFieldLength.PROTEOMICS_COMMENT,
      "life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.ProteomicsMeasurementEditColumn": MeasurementFieldLength.PROTEOMICS_COMMENT,
      // immunopeptidomics concern
      "life.qbic.datamanager.files.structure.measurement.IPMeasurementRegisterColumn": MeasurementFieldLength.IMMUNOPEPTIDOMICS_COMMENT,
      "life.qbic.datamanager.files.structure.measurement.IPMeasurementEditColumn": MeasurementFieldLength.IMMUNOPEPTIDOMICS_COMMENT,
      "life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.IPMeasurementRegisterColumn": MeasurementFieldLength.IMMUNOPEPTIDOMICS_COMMENT,
      "life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.IPMeasurementEditColumn": MeasurementFieldLength.IMMUNOPEPTIDOMICS_COMMENT,
  ]

  def "every comment column advertises the limit of its own concern"() {
    when: "the description of every comment column is read"
    def descriptions = commentColumnDescriptions()

    then: "all comment columns were found"
    descriptions.size() == COMMENT_COLUMN_ENUMS.size()

    and: "each of them advertises the limit its concern validates"
    for (entry in descriptions.entrySet()) {
      def expectedMax = COMMENT_COLUMN_ENUMS[entry.key]
      assert entry.value.contains("Max ${expectedMax} characters"):
          "${entry.key} advertises '${entry.value}' but its concern validates ${expectedMax}"
    }
  }

  private static Map<String, String> commentColumnDescriptions() {
    def descriptions = new LinkedHashMap<String, String>()
    for (String className : COMMENT_COLUMN_ENUMS.keySet()) {
      def enumClass = Class.forName(className)
      def commentColumn = enumClass.enumConstants.find { it.name() == "COMMENT" }
      assert commentColumn != null: "${className} has no COMMENT column"
      def helper = commentColumn.getFillHelp().orElse(null)
      assert helper != null: "${className}.COMMENT has no helper text"
      descriptions[className] = helper.description()
    }
    descriptions
  }
}
