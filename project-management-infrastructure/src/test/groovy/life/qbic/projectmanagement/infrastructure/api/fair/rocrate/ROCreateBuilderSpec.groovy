package life.qbic.projectmanagement.infrastructure.api.fair.rocrate

import java.nio.file.Path

import life.qbic.projectmanagement.application.api.fair.ContactPoint
import life.qbic.projectmanagement.application.api.fair.ResearchProject
import spock.lang.Specification
import spock.lang.TempDir

class ROCreateBuilderSpec extends Specification {

  @TempDir
  Path buildDir

  def "crate has no preview to avoid the library's hard-coded .tmp directory"() {
    given:
    def project = ResearchProject.from(
        "Test project",
        "QTEST001",
        "A project used to verify RO-Crate generation.",
        [ContactPoint.from("Jane Doe", "jane.doe@example.com", "principal investigator")])

    when:
    def crate = ROCreateBuilder.buildRoCrate(buildDir, project)

    then: "the optional preview is disabled"
    crate.preview == null
  }
}
