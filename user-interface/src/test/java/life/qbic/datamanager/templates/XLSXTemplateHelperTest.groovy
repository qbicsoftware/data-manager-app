package life.qbic.datamanager.templates

import life.qbic.datamanager.files.export.XLSXTemplateHelper
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import spock.lang.Specification

class XLSXTemplateHelperTest extends Specification {
    def "test to camel case"() {
        expect:
        XLSXTemplateHelper.toCamelCase(input).equals(output)

        where:
        input             | output
        "this is a test"  | "thisIsATest"
        "this is 4 test"  | "thisIs4Test"
        "this-is-a-test"  | "thisIsATest"
        "this_is_a_test"  | "thisIsATest"
        "this is_a-test"  | "thisIsATest"
        "thisisatest"     | "thisisatest"
        "thisIsATest"     | "thisIsATest"
        "this is a test*" | "thisIsATest"

    }

    def "test that column reference works"() {
        given:
        Workbook workbook = WorkbookFactory.create(true)
        def sheet = workbook.createSheet("My sheet")
        when:
        var result = XLSXTemplateHelper.createOptionArea(sheet,
                "test values",
                List.of("test1", "test2", "aböüß"))
        then:
        result.getRefersToFormula() == "'My sheet'!\$A\$1:\$A\$4"
        result.getNameName() == "testValues"
        workbook.getName("testValues") != null
    }
}
