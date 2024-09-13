package life.qbic.datamanager.spreadsheet

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
}
