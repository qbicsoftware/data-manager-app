package life.qbic.datamanager.parser;

import com.mysql.cj.xdevapi.RowResult;
import java.io.InputStream;
import java.util.List;
import org.apache.poi.ss.formula.functions.T;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface MetadataParser {

  ParseResult parse(InputStream inputStream);

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
