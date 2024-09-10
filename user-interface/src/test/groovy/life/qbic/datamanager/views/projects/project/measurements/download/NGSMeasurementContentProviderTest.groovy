package life.qbic.datamanager.views.projects.project.measurements.download

import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import spock.lang.Specification

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
class NGSMeasurementContentProviderTest extends Specification {

    def "test that column reference works"() {
        given:
        Workbook workbook = WorkbookFactory.create(true)
        def sheet = workbook.createSheet("My sheet")
        when:
        var result = NGSMeasurementContentProvider.addValueListWithName("namedArea", sheet, List.of("test1", "test2", "aböüß"), "test values")
        then:
        result.getRefersToFormula() == "'My sheet'!\$A\$1:\$A\$4"
        result.getNameName() == "namedArea"
        workbook.getName("namedArea") != null


    }
}
