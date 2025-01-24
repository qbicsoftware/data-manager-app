package life.qbic.datamanager.files.export;

import java.io.File;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

@FunctionalInterface
public interface WordFileSupplier extends FileSupplier {

  WordprocessingMLPackage getWordPackage();

  @Override
  default File getFile(String fileName) {
    try {
      File file = new File(fileName);
      getWordPackage().save(file);
      return file;
    } catch (Docx4JException e) {
      throw new FormatException("Creating docx package failed.", e);
    }
  }
}
