package life.qbic.projectmanagement.domain.project.experiment

import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class ExperimentalGroupSpec extends Specification {

    def "Two groups with distinct variable levels are not equal independent on sample size"() {
        given:
        ExperimentalValue ageValue1 = ExperimentalValue.create("25", "years")
        ExperimentalVariable ageVar1 = ExperimentalVariable.create("age", ageValue1)
        def ageLevel1 = VariableLevel.create(ageVar1.name(), ageValue1)
        ExperimentalValue seriesValue1 = ExperimentalValue.create("friends")
        ExperimentalVariable seriesVar1 = ExperimentalVariable.create("favorite series", seriesValue1)
        def seriesLevel1 = VariableLevel.create(seriesVar1.name(), seriesValue1)
        ExperimentalValue colorValue1 = ExperimentalValue.create("blue")
        ExperimentalVariable colorVar1 = ExperimentalVariable.create("favorite color", colorValue1)
        def colorLevel1 = VariableLevel.create(colorVar1.name(), colorValue1)

        ExperimentalValue ageValue2 = ExperimentalValue.create("20", "years")
        ExperimentalVariable ageVar2 = ExperimentalVariable.create("age", ageValue2)
        def ageLevel2 = VariableLevel.create(ageVar2.name(), ageValue2)
        ExperimentalValue seriesValue2 = ExperimentalValue.create("the expanse")
        ExperimentalVariable seriesVar2 = ExperimentalVariable.create("favorite series", seriesValue2)
        def seriesLevel2 = VariableLevel.create(seriesVar2.name(), seriesValue2)
        ExperimentalValue colorValue2 = ExperimentalValue.create("orange")
        ExperimentalVariable colorVar2 = ExperimentalVariable.create("favorite color", colorValue2)
        def colorLevel2 = VariableLevel.create(colorVar2.name(), colorValue2)

        when:
        def condition1 = Condition.create(Arrays.asList(ageLevel1, seriesLevel1, colorLevel1))
        ExperimentalGroup one = ExperimentalGroup.create(condition1, 1);
        def condition2 = Condition.create(Arrays.asList(ageLevel2, seriesLevel2, colorLevel2))
        ExperimentalGroup two = ExperimentalGroup.create(condition2, 1);

        then:
        !one.equals(two)
        !(one==two)
    }

}
