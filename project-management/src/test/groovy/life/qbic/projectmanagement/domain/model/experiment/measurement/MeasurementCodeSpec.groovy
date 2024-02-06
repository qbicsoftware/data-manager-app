package life.qbic.projectmanagement.domain.model.experiment.measurement

import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode
import spock.lang.Specification

class MeasurementCodeSpec extends Specification {


    def "A measurement code without a valid prefix must throw an IllegalArgumentException"() {
        given:
        def testPrefix = invalidCode

        when:
        MeasurementCode.parse(testPrefix)

        then:
        thrown(IllegalArgumentException)

        where:
        invalidCode << ["MIMI123QTEST001AE", "PXP1QTEST001AE", "IMAGING12QTEST001AE"]
    }

    def "Valid prefixes must result in a successfully parsed measurement code object"() {
        given:
        String testPrefix = validCode

        when:
        MeasurementCode parsedCode = MeasurementCode.parse(testPrefix)

        then:
        parsedCode instanceof MeasurementCode

        where:
        validCode << ["NGSQTEST001AE-" + System.nanoTime()]
    }

    def "Valid measurement counters must result in a successfully parsed measurement code object"() {
        given:
        String testPrefix = validCode

        when:
        MeasurementCode parsedCode = MeasurementCode.parse(testPrefix)

        then:
        parsedCode instanceof MeasurementCode
        parsedCode.value().equals(validCode)

        where:
        validCode << ["NGS1QTEST001AE-"+System.nanoTime(), "MS1QTEST001AE-"+System.nanoTime(), "IMG234QTEST001AE-"+System.nanoTime()]
    }

    def "Valid measurement code string with valid sample code must result in a successfully parsed representation"() {
        given:
        String testPrefix = validCode

        when:
        MeasurementCode parsedCode = MeasurementCode.parse(testPrefix)

        then:
        parsedCode instanceof MeasurementCode
        parsedCode.value().equals(validCode)

        where:
        validCode << ["NGS1QTEST001AE-"+System.nanoTime(), "MS1QTEST001AE-"+System.nanoTime(), "IMG234QTEST001AE-"+System.nanoTime()]
    }

    def "Valid measurement code string with invalid sample code must result in an IllegalArumentException"() {
        given:
        String testPrefix = validCode

        when:
        MeasurementCode parsedCode = MeasurementCode.parse(testPrefix)

        then:
        thrown IllegalArgumentException


        where:
        validCode << ["NGS1QTEST001AE11", "MS1Q001AE", "IMG234QTEST"]
    }


}
