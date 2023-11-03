package life.qbic.projectmanagement.domain

import life.qbic.projectmanagement.domain.model.project.ExperimentalDesignDescription
import spock.lang.Specification

class ExperimentalDesignDescriptionSpec extends Specification {

    def "expect creation without value throws RuntimeException"() {
        when: "creation without value throws RuntimeException"
        ExperimentalDesignDescription.create(null)
        then:
        thrown(RuntimeException)
    }

    def "expect two experimental design descriptions to be equal for equal value"() {
        expect: "two experimental design descriptions to be equal for equal value"
        ExperimentalDesignDescription.create("some value") == ExperimentalDesignDescription.create("some value")
    }

    def "expect two experimental design descriptions with different values to be different"() {
        expect: "two experimental design descriptions with different values to be different"
        ExperimentalDesignDescription.create("value one") != ExperimentalDesignDescription.create("value two")
    }

    def "expect values longer than the max length throw a RuntimeException"() {
        given:
        String invalidString = maxLengthString() + "i"
        when: "values longer than #maxLength throw a RuntimeException"
        ExperimentalDesignDescription.create(invalidString)

        then:
        thrown(RuntimeException)
    }

    def "expect values shorter or equal to do not throw a RuntimeException"() {
        when:
        ExperimentalDesignDescription.create(invalidString)
        then:
        noExceptionThrown()
        where:
        invalidString << [maxLengthString(), "short string"]
    }

    String maxLengthString() {
        String maxLength = ""
        for (i in 0..<ExperimentalDesignDescription.MAX_LENGTH) {
            maxLength = maxLength + "i"
        }
        return maxLength
    }
}
