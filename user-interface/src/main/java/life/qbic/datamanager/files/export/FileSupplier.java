package life.qbic.datamanager.files.export;

import java.io.File;

@FunctionalInterface
public interface FileSupplier {

  File getFile(String fileName);

  class FormatException extends RuntimeException {

    public FormatException(String message) {
      super(message);
    }

    public FormatException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
