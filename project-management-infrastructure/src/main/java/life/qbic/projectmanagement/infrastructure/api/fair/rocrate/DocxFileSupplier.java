package life.qbic.projectmanagement.infrastructure.api.fair.rocrate;

import java.io.File;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

public class DocxFileSupplier implements FileSupplier {

  private final WordprocessingMLPackage wordPackage;

  DocxFileSupplier(WordprocessingMLPackage wordPackage) {
    this.wordPackage = wordPackage;

  }

  public static DocxFileSupplier supplying(WordprocessingMLPackage wordPackage) {
    return new DocxFileSupplier(wordPackage);
  }


  @Override
  public File getFile(String fileName) {
    try {
      File file = new File(fileName);
      wordPackage.save(file);
      return file;
    } catch (Docx4JException e) {
      throw new FormatException("Creating docx package failed.", e);
    }
  }
}
