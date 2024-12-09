package life.qbic.datamanager.export.rocrate;

import static life.qbic.datamanager.export.rocrate.ROCreateBuilder.ResearchProjectConstants.SUMMARY_FILENAME_DOCX;
import static life.qbic.datamanager.export.rocrate.ROCreateBuilder.ResearchProjectConstants.SUMMARY_FILENAME_YAML;

import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.RoCrate.RoCrateBuilder;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity.ContextualEntityBuilder;
import edu.kit.datamanager.ro_crate.entities.data.FileEntity.FileEntityBuilder;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import life.qbic.datamanager.export.TempDirectory;
import life.qbic.datamanager.export.docx.DocxSupplier;
import life.qbic.datamanager.export.model.ContactPoint;
import life.qbic.datamanager.export.model.ResearchProject;
import life.qbic.datamanager.export.yaml.YamlSupplier;
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

  private static final String ROR_URL_QBIC = "https://ror.org/00v34f693";

  private static final String DATA_PROVIDER_QBIC = "Quantitative Biology Center";

  private static final String LICENSE_URL_CCBY_4 = "https://creativecommons.org/licenses/by/4.0/";

  @Autowired
  public ROCreateBuilder(TempDirectory tempDir) {
    this.tempDirectory = Objects.requireNonNull(tempDir);
  }

  private static ContextualEntity licenseCCBY() {
    return new ContextualEntityBuilder()
        .addType("CreativeWork")
        .setId(LICENSE_URL_CCBY_4)
        .addProperty("description", "This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit https://creativecommons.org/licenses/by/4.0/ .")
        .addProperty("identifier", LICENSE_URL_CCBY_4)
        .addProperty("name", "Attribution-4.0 International (CC BY 4.0)")
        .build();
  }

  private static ContextualEntity qbicOrganisation() {
    return new ContextualEntityBuilder()
        .addType("Organization")
        .setId(ROR_URL_QBIC)
        .addProperty("identifier", ROR_URL_QBIC)
        .addProperty("name", DATA_PROVIDER_QBIC)
        .build();
  }

  private static RoCrate buildRoCrate(Path buildDir, ResearchProject researchProject) {
    var projectInfoDocx = DocxSupplier.create()
        .from(buildDir.resolve(SUMMARY_FILENAME_DOCX.value()).toString(), researchProject);
    var projectInfoYaml = YamlSupplier.create()
        .from(buildDir.resolve(SUMMARY_FILENAME_YAML.value()).toString(), researchProject);
    var crate = new RoCrateBuilder(
        "QBiC-project-%s-ro-crate".formatted(researchProject.identifier()),
        "Description of the project %s with the title '%s', managed on the Data Manager, Quantitative Biology Center, University of Tübingen.".formatted(
            researchProject.identifier(), researchProject.name()))
        .addContextualEntity(licenseCCBY())// default is CC BY 4.0 international (https://creativecommons.org/licenses/by/4.0/)
        .addContextualEntity(qbicOrganisation())
        .addDataEntity(
            new FileEntityBuilder()
                .setSource(projectInfoDocx)
                .setId(SUMMARY_FILENAME_DOCX.value())
                .addProperty("name", "Project Summary")
                .addProperty("encodingFormat",
                    MimeTypes.DOCX.value())
                .build())
        .addDataEntity(
            new FileEntityBuilder()
                .setSource(projectInfoYaml)
                .setId(SUMMARY_FILENAME_YAML.value())
                .addProperty("name", "Project Summary")
                .addProperty("encodingFormat", MimeTypes.YAML.value())
                .build())
        .build();
    crate.getRootDataEntity().addIdProperty("publisher", ROR_URL_QBIC);
    crate.getRootDataEntity().addIdProperty("license", LICENSE_URL_CCBY_4);
    crate.getRootDataEntity().addProperty("datePublished", Instant.now().toString());
    return crate;
  }

  public RoCrate projectSummary(Project project, Path buildDirectory) throws ROCrateBuildException {
    var researchProject = convertToResearchProject(project);
    if (!buildDirectory.toFile().exists()) {
      throw new ROCrateBuildException("File does not exist: " + buildDirectory);
    }
    return buildRoCrate(buildDirectory, researchProject);
  }

  private ResearchProject convertToResearchProject(Project project) {
    var contactPoints = new ArrayList<ContactPoint>();
    contactPoints.add(toContactPoint(project.getPrincipalInvestigator(), "Principal Investigator"));
    contactPoints.add(toContactPoint(project.getProjectManager(), "Project Manager"));
    if (project.getResponsiblePerson().isPresent()) {
      contactPoints.add(toContactPoint(project.getResponsiblePerson().orElseThrow(), "Responsible Person"));
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
