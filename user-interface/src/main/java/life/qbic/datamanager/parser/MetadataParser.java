package life.qbic.datamanager.parser;

import java.io.InputStream;

/**
 * <b>Metadata Parser</b>
 * <p>
 * A generic interface to hide the details of input file formats to parse, and enables clients and
 * downstream consumers to work with an intermediate abstraction of the content, which is contained
 * in the {@link ParsingResult} object.
 *
 * @since 1.4.0
 */
public interface MetadataParser {

  ParsingResult parse(InputStream inputStream);

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
