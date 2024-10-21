package life.qbic.datamanager.export;

import java.io.File;
import life.qbic.datamanager.export.model.ResearchProject;

/**
 * <b>File format supplier</b>
 * <p>
 * Interface describing the API of suppliers for different file formats.
 *
 * @since 1.6.0
 */
public interface FileFormatSupplier {

  File from(String fileName, ResearchProject researchProject) throws FormatException;

  class FormatException extends RuntimeException {

    public FormatException(String message) {
      super(message);
    }

    public FormatException(String message, Throwable cause) {
      super(message, cause);
    }
  }

}
