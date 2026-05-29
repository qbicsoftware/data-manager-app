package life.qbic.datamanager.views.general.upload;

import java.io.InputStream;
import java.util.Optional;

/**
 * Validates the content of an uploaded file.
 * <p>
 * Returns {@link Optional#empty()} if the content is valid, or an error message if invalid.
 */
public interface UploadContentValidator {

  Optional<String> validate(String fileName, InputStream content);
}
