package life.qbic.datamanager.export.docx;

import java.io.File;
import life.qbic.datamanager.export.Formatter;
import life.qbic.datamanager.export.model.ResearchProject;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class DocxFormatter implements Formatter {

  public static DocxFormatter create() {
    return new DocxFormatter();
  }

  @Override
  public File from(String fileName, ResearchProject researchProject) {
    try {
      var wordPackage = WordprocessingMLPackage.createPackage();
      wordPackage.getMainDocumentPart().addParagraphOfText(researchProject.name());
      File file = new File(fileName);
      wordPackage.save(file);
      return file;
    } catch (Docx4JException e) {
      throw new FormatException("Creating docx package failed. ", e);
    }
  }
}
