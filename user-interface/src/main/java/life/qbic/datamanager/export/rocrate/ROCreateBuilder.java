package life.qbic.datamanager.export.rocrate;

import static life.qbic.datamanager.export.rocrate.ROCreateBuilder.ResearchProjectConstants.SUMMARY_FILENAME_DOCX;
import static life.qbic.datamanager.export.rocrate.ROCreateBuilder.ResearchProjectConstants.SUMMARY_FILENAME_YAML;

import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.data.FileEntity;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import life.qbic.datamanager.export.TempDirectory;
import life.qbic.datamanager.export.docx.DocxFormatter;
import life.qbic.datamanager.export.model.ContactPoint;
import life.qbic.datamanager.export.model.ResearchProject;
import life.qbic.datamanager.export.yaml.YamlFormatter;
import life.qbic.projectmanagement.domain.model.project.Contact;
import life.qbic.projectmanagement.domain.model.project.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>RO-Crate Builder</b>
 * <p>
 * Builder class that helps to build a RO-Crate based on various QBiC data manager project
 * information.
 *
 * @since 1.6.0
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
        .from(buildDir.resolve(SUMMARY_FILENAME_DOCX.value()).toString(), researchProject);
    var projectInfoYaml = YamlFormatter.create()
        .from(buildDir.resolve(SUMMARY_FILENAME_YAML.value()).toString(), researchProject);
    return new RoCrate.RoCrateBuilder(
        "QBiC-project-%s-ro-crate".formatted(researchProject.identifier()),
        "Description of the project %s with the title '%s', managed on the Data Manager, Quantitative Biology Center, University of Tübingen.".formatted(
            researchProject.identifier(), researchProject.name()))
        .addDataEntity(
            new FileEntity.FileEntityBuilder()
                .setSource(projectInfoDocx)
                .setId(SUMMARY_FILENAME_DOCX.value())
                .addProperty("name", "Project Summary")
                .addProperty("encodingFormat",
                    MimeTypes.DOCX.value())
                .build())
        .addDataEntity(
            new FileEntity.FileEntityBuilder()
                .setSource(projectInfoYaml)
                .setId(SUMMARY_FILENAME_YAML.value())
                .addProperty("name", "Project Summary")
                .addProperty("encodingFormat", MimeTypes.YAML.value())
                .build())
        .build();
  }

  public RoCrate projectSummary(Project project) throws ROCrateBuildException {
    var researchProject = convertToResearchProject(project);
    try {
      var assignedBuildDirectory = tempDirectory.createDirectory();
      return buildRoCrate(assignedBuildDirectory, researchProject);
    } catch (IOException e) {
      throw new ROCrateBuildException("RO-Crate creation failed", e);
    }

  }

  private ResearchProject convertToResearchProject(Project project) {
    var contactPoints = new ArrayList<ContactPoint>();
    contactPoints.add(toContactPoint(project.getPrincipalInvestigator(), "Principal Investigator"));
    contactPoints.add(toContactPoint(project.getProjectManager(), "Project Manager"));
    if (project.getResponsiblePerson().isPresent()) {
      contactPoints.add(toContactPoint(project.getResponsiblePerson().get(), "Responsible Person"));
    }
    return ResearchProject.from(project.getProjectIntent().projectTitle().title(),
        project.getProjectCode().value(), project.getProjectIntent().objective().objective(),
        contactPoints);
  }

  private ContactPoint toContactPoint(Contact contact, String contactType) {
    return ContactPoint.from(contact.fullName(), contact.emailAddress(), contactType);
  }

  enum ResearchProjectConstants implements Value {

    SUMMARY_FILENAME_DOCX("project-summary.docx"),
    SUMMARY_FILENAME_YAML("project-summary.yml");

    private final String value;

    ResearchProjectConstants(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  enum MimeTypes implements Value {

    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    YAML("application/yaml");

    private final String value;

    MimeTypes(String value) {
      this.value = value;
    }

    @Override
    public String value() {
      return value;
    }
  }

  interface Value {

    String value();
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
