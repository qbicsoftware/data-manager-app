package life.qbic.datamanager.views.general.upload;

import java.io.InputStream;
import java.util.Optional;

/**
 * Strategy interface for validating the content of an uploaded file.
 * <p>
 * Implementations inspect the file stream and return an empty {@link Optional} to indicate
 * that the content is acceptable, or an error message describing the problem.
 * <p>
 * Implementations must not close the provided input stream.
 */
public interface UploadContentValidator {

  /**
   * Validates the content of an uploaded file.
   *
   * @param fileName the original name of the uploaded file (may be used to determine the expected
   *                 format or type)
   * @param content  an {@link InputStream} providing the file content to validate
   * @return {@link Optional#empty()} if the content is valid, or an {@link Optional} containing a
   *         human-readable error message describing why validation failed
   */
  Optional<String> validate(String fileName, InputStream content);
}
