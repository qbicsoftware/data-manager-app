package life.qbic.projectmanagement.domain.model.experiment

import life.qbic.projectmanagement.domain.model.experiment.*
import org.spockframework.util.Nullable
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class ExperimentalGroupSpec extends Specification {

    def condition = createCondition("Color", "red", null)
    def conditionWithUnit = createCondition("Time", "10", "Seconds")

    def "Experimental groups with the same id are equal"() {
        given:
        def experimentalGroup = ExperimentalGroup.create(condition, 1)
        experimentalGroup.experimentalGroupId = 201
        expect:
        experimentalGroup.equals(experimentalGroup)
    }

    def "Experimental groups with the different ids are not equal"() {
        given:
        def experimentalGroup = ExperimentalGroup.create(condition, 1)
        def experimentalGroup2 = ExperimentalGroup.create(condition, 1)
        experimentalGroup.experimentalGroupId = 201
        experimentalGroup2.experimentalGroupId = 202
        expect:
        !experimentalGroup.equals(experimentalGroup2)
    }

    def "Experimental groups with the same id have the same hashcode"() {
        given:
        def experimentalGroup = ExperimentalGroup.create(conditionWithUnit, 1)
        experimentalGroup.experimentalGroupId = 201
        expect:
        experimentalGroup.hashCode() == experimentalGroup.hashCode()
    }

    def "Experimental groups with the different ids have a different hashcode"() {
        given:
        def experimentalGroup = ExperimentalGroup.create(conditionWithUnit, 1)
        def experimentalGroup2 = ExperimentalGroup.create(conditionWithUnit, 1)
        experimentalGroup.experimentalGroupId = 201
        experimentalGroup2.experimentalGroupId = 202
        expect:
        !experimentalGroup.hashCode().equals(experimentalGroup2.hashCode())
    }

    Condition createCondition(String variableName, String valueName, @Nullable String valueUnit) {
        Condition condition
        if (valueUnit != null) {
            condition = Condition.create(List.of(VariableLevel.create(VariableName.create(variableName), ExperimentalValue.create(valueName, valueUnit))))
        } else {
            condition = Condition.create(List.of(VariableLevel.create(VariableName.create(variableName), ExperimentalValue.create(valueName))))
        }
        return condition
    }

    def "If an experimental group has no id it will never equal another experimental group with an id"() {
        given:
        def experimentalGroup = ExperimentalGroup.create(conditionWithUnit, 1)
        def experimentalGroup2 = ExperimentalGroup.create(conditionWithUnit, 1)
        experimentalGroup2.experimentalGroupId = 202
        expect:
        !experimentalGroup.hashCode().equals(experimentalGroup2.hashCode())
    }
}
