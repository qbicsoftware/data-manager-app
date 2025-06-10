package life.qbic.projectmanagement.infrastructure.api.fair.rocrate;

import java.util.List;
import life.qbic.projectmanagement.application.api.fair.ResearchProject;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.P;

public class ResearchProjectDocxBuilder implements DocxBuilder<ResearchProject> {

  @Override
  public List<Object> createContent(MainDocumentPart mainDocument,
      ResearchProject project) {
    List<P> contactPoints = project.contactPoint().stream()
        .map(contactPoint -> mainDocument.createParagraphOfText(
            "%s (%s) - %s".formatted(contactPoint.name(), contactPoint.contactType(),
                contactPoint.email())))
        .toList();
    return List.of(
        DocxBuilder.createTitle(mainDocument, project.name()),
        DocxBuilder.createSubtitle(mainDocument, "Project ID: " + project.identifier()),
        new Section("Description",
            List.of(mainDocument.createParagraphOfText(project.description()))),
        new Section("Contact Points", contactPoints)
    );
  }
}
