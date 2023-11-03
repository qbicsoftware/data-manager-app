package life.qbic.projectmanagement.domain

import life.qbic.projectmanagement.domain.model.project.ProjectIntent
import life.qbic.projectmanagement.domain.model.project.ProjectObjective
import life.qbic.projectmanagement.domain.model.project.ProjectTitle
import spock.lang.Specification

class ProjectIntentSpec extends Specification {

    def "expect creation without title not possible"() {
        given: "a project objective"
        ProjectObjective objective = ProjectObjective.create("my objective")
        when: "creation with null parameters is attempted"
        ProjectIntent.of(null, objective)
        then: "an exception is thrown"
        thrown(RuntimeException)
    }

    def "expect creation without objective not possible"() {
        given: "a project title"
        ProjectTitle title = ProjectTitle.of("my objective")
        when: "creation with null parameters is attempted"
        ProjectIntent.of(title, null)
        then: "an exception is thrown"
        thrown(RuntimeException)
    }

    def "expect a project intent with equal values to be equal"() {
        given:
        ProjectTitle title = ProjectTitle.of("title")
        ProjectObjective objective = ProjectObjective.create("objective")

        expect: "a project intent with equal values to be equal"
        ProjectIntent.of(title, objective)
                == ProjectIntent.of(title, objective)
    }

    def "expect two project intents with different title to be different"() {
        given:
        ProjectTitle titleOne = ProjectTitle.of("title one")
        ProjectTitle titleTwo = ProjectTitle.of("title two")
        ProjectObjective objective = ProjectObjective.create("objective")
        expect: "two project intents with different title to be different"
        ProjectIntent.of(titleOne, objective) != ProjectIntent.of(titleTwo, objective)
    }

    def "expect two project intents with different objectives to be different"() {
        given:
        ProjectTitle title = ProjectTitle.of("title")
        ProjectObjective objectiveOne = ProjectObjective.create("objective one")
        ProjectObjective objectiveTwo = ProjectObjective.create("objective two")
        expect: "two project intents with different title to be different"
        ProjectIntent.of(title, objectiveOne) != ProjectIntent.of(title, objectiveTwo)
    }
}
