package life.qbic.datamanager.export;

import java.io.File;
import life.qbic.datamanager.export.model.ResearchProject;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface Formatter {

  File from(String fileName, ResearchProject researchProject) throws FormatException;

  class FormatException extends RuntimeException {
    public FormatException(String message) {}
    public FormatException(String message, Throwable cause) {}
  }

}
