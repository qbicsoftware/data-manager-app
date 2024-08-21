package life.qbic.datamanager.parser;

import java.io.InputStream;
import java.util.List;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface MetadataParser<T> {

  List<T> parse(InputStream inputStream);

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
}
