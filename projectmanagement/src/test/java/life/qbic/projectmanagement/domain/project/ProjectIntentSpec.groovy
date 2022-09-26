package life.qbic.projectmanagement.domain.project


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
    ProjectTitle title = ProjectTitle.create("my objective")
    when: "creation with null parameters is attempted"
    ProjectIntent.of(title, null)
    then: "an exception is thrown"
    thrown(RuntimeException)
  }

  def "expect a project intent with equal values to be equal"() {
    given:
    ProjectTitle title = ProjectTitle.create("title")
    ProjectObjective objective = ProjectObjective.create("objective")
    ExperimentalDesignDescription experimentalDesignDescription = ExperimentalDesignDescription.create("description")

    expect: "a project intent with equal values to be equal"
    ProjectIntent.of(title, objective).with(experimentalDesignDescription)
            == ProjectIntent.of(title, objective).with(experimentalDesignDescription)
  }

  def "expect two project intents with different title to be different"() {
    given:
    ProjectTitle titleOne = ProjectTitle.create("title one")
    ProjectTitle titleTwo = ProjectTitle.create("title two")
    ProjectObjective objective = ProjectObjective.create("objective")
    expect: "two project intents with different title to be different"
    ProjectIntent.of(titleOne, objective) != ProjectIntent.of(titleTwo, objective)
  }

  def "expect two project intents with different objectives to be different"() {
    given:
    ProjectTitle title = ProjectTitle.create("title")
    ProjectObjective objectiveOne = ProjectObjective.create("objective one")
    ProjectObjective objectiveTwo = ProjectObjective.create("objective two")
    expect: "two project intents with different title to be different"
    ProjectIntent.of(title, objectiveOne) != ProjectIntent.of(title, objectiveTwo)
  }

  def "expect two project intents with different experimental design descriptions to be different"() {
    given:
    ProjectTitle title = ProjectTitle.create("title")
    ProjectObjective objective = ProjectObjective.create("objective")
    ExperimentalDesignDescription descriptionOne = ExperimentalDesignDescription.create("description one")
    ExperimentalDesignDescription descriptionTwo = ExperimentalDesignDescription.create("description two")

    expect: "two project intents with different title to be different"
    ProjectIntent.of(title, objective).with(descriptionOne) != ProjectIntent.of(title, objective).with(descriptionTwo)
  }
}
