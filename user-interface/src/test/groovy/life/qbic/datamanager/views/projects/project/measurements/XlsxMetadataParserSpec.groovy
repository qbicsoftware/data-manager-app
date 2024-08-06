package life.qbic.datamanager.views.projects.project.measurements

import life.qbic.projectmanagement.application.measurement.validation.MeasurementNGSValidator
import life.qbic.projectmanagement.application.measurement.validation.MeasurementProteomicsValidator
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService
import spock.lang.Specification

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
class XlsxMetadataParserSpec extends Specification {

    static String EMPTY_WORKBOOK_PATH = "projects/project/measurements/empty-workbook.xlsx"
    static String INVALID_HEADER_WORKBOOK = "projects/project/measurements/wrong-header-row-workbook.xlsx"
    static String EMPTY_NGS_WORKBOOK = "projects/project/measurements/wrong-header-row-workbook.xlsx"
    static String EMPTY_NGS_EDIT_WORKBOOK = "projects/project/measurements/wrong-header-row-workbook.xlsx"

    def "parse metadata throws NoHeaderRowException if no header row is present"() {
        given:
        MeasurementValidationService validationService = Mock()
        var inputStream = loadResource(EMPTY_WORKBOOK_PATH)
        and: "an instance of the parser"
        XlsxMetadataParser parser = new XlsxMetadataParser(validationService)
        when:
        parser.parseMetadata(inputStream)
        then:
        thrown(MetadataParser.NoHeaderRowException)
    }

    def "parse metadata throws NoMatchingDomainFound if the header is not recognized"() {
        given:
        MeasurementValidationService validationService = new MeasurementValidationService(
                Mock(MeasurementNGSValidator),
                Mock(MeasurementProteomicsValidator)
        )
        var inputStream = loadResource(INVALID_HEADER_WORKBOOK)
        and:
        XlsxMetadataParser parser = new XlsxMetadataParser(validationService)
        when:
        parser.parseMetadata(inputStream)
        then:
        thrown(MetadataParser.NoMatchingDomainFoundException)
    }

    def "parse metadata returns empty list if no measurement row is contained"() {
        given:
        MeasurementValidationService validationService = new MeasurementValidationService(
                Mock(MeasurementNGSValidator),
                Mock(MeasurementProteomicsValidator)
        )
        var inputStream = loadResource(INVALID_HEADER_WORKBOOK)
        and:
        XlsxMetadataParser parser = new XlsxMetadataParser(validationService)
        when:
        parser.parseMetadata(inputStream)
        then:
        thrown(MetadataParser.NoMatchingDomainFoundException)
    }

    InputStream loadResource(String name) {
        return getClass().getClassLoader().getResourceAsStream(name)
    }
}
