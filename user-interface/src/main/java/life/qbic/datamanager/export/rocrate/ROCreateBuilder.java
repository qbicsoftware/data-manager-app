package life.qbic.datamanager.export.rocrate;

import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.data.FileEntity;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import life.qbic.datamanager.export.TempDirectory;
import life.qbic.datamanager.export.docx.DocxFormatter;
import life.qbic.datamanager.export.model.ResearchProject;
import life.qbic.datamanager.export.yaml.YamlFormatter;
import life.qbic.projectmanagement.domain.model.project.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class ROCreateBuilder {

  private final TempDirectory tempDirectory;

  @Autowired
  public ROCreateBuilder(TempDirectory tempDir) {
    this.tempDirectory = Objects.requireNonNull(tempDir);
  }

  private static RoCrate buildRoCrate(Path buildDir, ResearchProject researchProject) {
    var projectInfoDocx = DocxFormatter.create()
        .from(buildDir.resolve("project-information.docx").toString(), researchProject);
    var projectInfoYaml = YamlFormatter.create()
        .from(buildDir.resolve("project-information.yml").toString(), researchProject);
    return new RoCrate.RoCrateBuilder(
        "QBiC-project-%s-ro-crate".formatted(researchProject.identifier()),
        "Description of the project %s with the title '%s', managed on the Data Manager, Quantitative Biology Center, University of Tübingen.".formatted(
            researchProject.identifier(), researchProject.name()))
        .addDataEntity(
            new FileEntity.FileEntityBuilder()
                .setSource(projectInfoDocx)
                .setId("project-information.docx")
                .addProperty("name", "Project Information")
                .addProperty("encodingFormat",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build())
        .addDataEntity(
            new FileEntity.FileEntityBuilder()
                .setSource(projectInfoYaml)
                .setId("project-information.yml")
                .addProperty("name", "Project Information")
                .addProperty("encodingFormat", "application/yaml")
                .build())
        .build();
  }

  public RoCrate projectInformation(Project project) throws ROCrateBuildException {
    var researchProject = convertToResearchProject(project);
    try {
      var assignedBuildDirectory = tempDirectory.createDirectory();
      return buildRoCrate(assignedBuildDirectory, researchProject);
    } catch (IOException e) {
      throw new ROCrateBuildException("RO-Crate creation failed", e);
    }

  }

  private ResearchProject convertToResearchProject(Project project) {
    return ResearchProject.from(project.getProjectIntent().projectTitle().title(),
        project.getProjectCode().value(), project.getProjectIntent().objective().objective());
  }

  public static class ROCrateBuildException extends RuntimeException {

    public ROCrateBuildException(String message) {
      super(message);
    }

    public ROCrateBuildException(String message, Throwable cause) {
      super(message, cause);
    }
  }

}
