package life.qbic.datamanager.exporting.docx;

import java.io.File;
import life.qbic.datamanager.exporting.FileFormatSupplier;
import life.qbic.datamanager.exporting.model.ContactPoint;
import life.qbic.datamanager.exporting.model.ResearchProject;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

/**
 * <b>DOCX formatter implementation</b>
 * <p>
 * Creates DOCX representations of various content types in a Data Manager's project.
 *
 * @since 1.0.0
 */
public class DocxSupplier implements FileFormatSupplier {

  public static DocxSupplier create() {
    return new DocxSupplier();
  }

  @Override
  public File from(String fileName, ResearchProject researchProject) {
    try {
      var wordPackage = WordprocessingMLPackage.createPackage();
      var mainDocument = wordPackage.getMainDocumentPart();
      addTitle(mainDocument, researchProject);
      addProjectId(mainDocument, researchProject);
      addSection(mainDocument, "Description", researchProject.description());
      addSectionTitle(mainDocument, "Contact Points");
      researchProject.contactPoint()
          .forEach(contactPoint -> addContactPoint(mainDocument, contactPoint));
      File file = new File(fileName);
      wordPackage.save(file);
      return file;
    } catch (Docx4JException e) {
      throw new FormatException("Creating docx package failed. ", e);
    }
  }

  private void addTitle(MainDocumentPart mainDocumentPart, ResearchProject researchProject) {
    mainDocumentPart.addStyledParagraphOfText("Title", researchProject.name());
  }

  private void addProjectId(MainDocumentPart mainDocumentPart, ResearchProject researchProject) {
    mainDocumentPart.addStyledParagraphOfText("Subtitle",
        "Project ID: " + researchProject.identifier());
  }

  private void addSection(MainDocumentPart mainDocumentPart, String sectionTitle,
      String sectionContent) {
    mainDocumentPart.addStyledParagraphOfText("Heading1", sectionTitle);
    mainDocumentPart.addParagraphOfText(sectionContent);
  }

  private void addSectionTitle(MainDocumentPart mainDocumentPart, String title) {
    mainDocumentPart.addStyledParagraphOfText("Heading1", title);
  }

  private void addContactPoint(MainDocumentPart mainDocumentPart, ContactPoint contactPoint) {
    var contactPointFormatted = "%s (%s) - %s".formatted(contactPoint.name(),
        contactPoint.contactType(), contactPoint.email());
    mainDocumentPart.addParagraphOfText(contactPointFormatted);
  }
}
