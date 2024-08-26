package life.qbic.datamanager.parser;

import java.io.InputStream;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface MetadataParser {

  ParsingResult parse(InputStream inputStream);

  class UnknownDomainException extends RuntimeException {

    public UnknownDomainException(String message) {
      super(message);
    }
  }

  class UnknownPropertiesException extends RuntimeException {

    public UnknownPropertiesException(String message) {
      super(message);
    }
  }

  class ParsingException extends RuntimeException {
    public ParsingException(String message, Throwable cause) {
      super(message, cause);
    }

    public ParsingException(String message) {
      super(message);
    }
  }
}
