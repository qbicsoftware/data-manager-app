package life.qbic.application.commons

import spock.lang.Specification

class MeasurementCodeComparatorSpec extends Specification {

    def "should order equal measurement codes by numeric timestamp, sign #expectedSign"() {
        expect:
        Integer.signum(MeasurementCodeComparator.INSTANCE.compare(a, b)) == expectedSign

        where:
        a                                | b                                || expectedSign
        "NGSQ2RGU6004A0-123456789012345" | "NGSQ2RGU6004A0-234567890123456" || -1
        "NGSQ2RGU6004A0-234567890123456" | "NGSQ2RGU6004A0-123456789012345" || 1
        "NGSQ2RGU6004A0-345678901234567" | "NGSQ2RGU6004A0-345678901234567" || 0
        "MSQ2RGU6042AE-123456789012345"  | "MSQ2RGU6042AE-234567890123456"  || -1
        "MSQ2RGU6042AE-234567890123456"  | "MSQ2RGU6042AE-123456789012345"  || 1
        "MSQ2RGU6042AE-345678901234567"  | "MSQ2RGU6042AE-345678901234567"  || 0
        "IPQ2IMMU667AM-123456789012345"  | "IPQ2IMMU667AM-234567890123456"  || -1
        "IPQ2IMMU667AM-234567890123456"  | "IPQ2IMMU667AM-123456789012345"  || 1
        "IPQ2IMMU667AM-345678901234567"  | "IPQ2IMMU667AM-345678901234567"  || 0
    }

    def "should order different measurement codes by code portion first"() {
        expect:
        Integer.signum(MeasurementCodeComparator.INSTANCE.compare(a, b)) == expectedSign

        where:
        a                                | b                                || expectedSign
        "NGSQ2RGU6004A0-123456789012345" | "NGSQ2RGU6ABCDE-123456789012345" || -1
        "NGSQ2RGU6ABCDE-627616355705691" | "NGSQ2RGU6004A0-62761635570569"  || 1
        "MSQ2RGU6042AE-464962944487339"  | "MSQ2RGU6043AN-464963251848323"  || -1
        "MSQ2RGU6043AN-464963251848323"  | "MSQ2RGU6042AE-464962944487339"  || 1
        "IPQ2IMMU667AM-49649226907583"   | "IPQ2IMMU697AC-49683925451625"   || -1
        "IPQ2IMMU697AC-49683925451625"   | "IPQ2IMMU667AM-49649226907583"   || 1
    }

    def "should sort a list of NGS measurement codes correctly"() {
        given:
        def codes = [
                "NGSQ2IMMU004A0-627616355705691",
                "NGSQ2RGU6006AI-627616358631007",
                "NGSQ2RGU6008A2-627616361851621",
                "NGSQ2IMMU005A9-627616345670657",
                "NGSQ2RGU6007AR-627616349186183",
                "NGSQ2IMMU003AP-627616159590351",
        ]

        expect:
        codes.toSorted(MeasurementCodeComparator.INSTANCE) == [
                "NGSQ2IMMU003AP-627616159590351",
                "NGSQ2IMMU004A0-627616355705691",
                "NGSQ2IMMU005A9-627616345670657",
                "NGSQ2RGU6006AI-627616358631007",
                "NGSQ2RGU6007AR-627616349186183",
                "NGSQ2RGU6008A2-627616361851621",
        ]
    }

    def "should sort a list of PxP measurement codes correctly"() {
        given:
        def codes = [
                "MSQ2RGU6044AW-464962722396166",
                "MSQ2IMMU033AF-464962844142228",
                "MSQ2RGU6042AE-464962944487339",
                "MSQ2IMMU040AU-464963146825511",
                "MSQ2RGU6043AN-464963251848323",
                "MSQ2IMMU032A6-464963351785520",
        ]

        expect:
        codes.toSorted(MeasurementCodeComparator.INSTANCE) == [
                "MSQ2IMMU032A6-464963351785520",
                "MSQ2IMMU033AF-464962844142228",
                "MSQ2IMMU040AU-464963146825511",
                "MSQ2RGU6042AE-464962944487339",
                "MSQ2RGU6043AN-464963251848323",
                "MSQ2RGU6044AW-464962722396166",

        ]
    }

    def "should sort a list of IP measurement codes correctly"() {
        given:
        def codes = [
                "IPQ2RGU6667AM-49649226907583",
                "IPQ2IMMU697AC-49683925451625",
                "IPQ2IMMU799A3-49679851237708",
                "IPQ2RGU6715A5-49654676415125",
                "IPQ2IMMU837AC-49684457461208",
                "IPQ2RGU6699AU-49680188910542",
        ]

        expect:
        codes.toSorted(MeasurementCodeComparator.INSTANCE) == [
                "IPQ2IMMU697AC-49683925451625",
                "IPQ2IMMU799A3-49679851237708",
                "IPQ2IMMU837AC-49684457461208",
                "IPQ2RGU6667AM-49649226907583",
                "IPQ2RGU6699AU-49680188910542",
                "IPQ2RGU6715A5-49654676415125",
        ]
    }

    def "should sort a mixed list of measurement codes correctly"() {
        given:
        def codes = [
                "IPQ2RGU6009AB-182164473421523",
                "MSQ2RGU6042AE-464962944487339",
                "NGSQ2RGU6006AI-627616358631007",
                "MSQ2IMMU603AP-627854040523020",
                "IPQ2RGU6031AC-49649931669667",
                "NGSQ2IMMU604A0-627616355705691",
                "IPQ2IMMU667AM-49649226907583",
                "MSQ2RGU6033AF-464962844142228",
                "NGSQ2RGU6007AR-627616349186183",
        ]

        expect:
        codes.toSorted(MeasurementCodeComparator.INSTANCE) == [
                "IPQ2IMMU667AM-49649226907583",
                "IPQ2RGU6009AB-182164473421523",
                "IPQ2RGU6031AC-49649931669667",
                "MSQ2IMMU603AP-627854040523020",
                "MSQ2RGU6033AF-464962844142228",
                "MSQ2RGU6042AE-464962944487339",
                "NGSQ2IMMU604A0-627616355705691",
                "NGSQ2RGU6006AI-627616358631007",
                "NGSQ2RGU6007AR-627616349186183",
        ]
    }

    def "should return 0 when both values are null"() {
        expect:
        MeasurementCodeComparator.INSTANCE.compare(null, null) == 0
    }

    def "should sort null before any non-null measurement code"() {
        expect:
        MeasurementCodeComparator.INSTANCE.compare(null, "NGSQ2RGU6004A0-627616355705691") < 0
        MeasurementCodeComparator.INSTANCE.compare("NGSQ2RGU6004A0-627616355705691", null) > 0
        MeasurementCodeComparator.INSTANCE.compare(null, "MSQ2RGU6003AP-627854040523020") < 0
        MeasurementCodeComparator.INSTANCE.compare("MSQ2RGU6003AP-627854040523020", null) > 0
        MeasurementCodeComparator.INSTANCE.compare(null, "IPQ2IMMU667AM-49649226907583") < 0
        MeasurementCodeComparator.INSTANCE.compare("IPQ2IMMU667AM-49649226907583", null) > 0
    }
}
