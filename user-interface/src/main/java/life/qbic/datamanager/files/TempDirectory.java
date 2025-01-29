package life.qbic.datamanager.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <b>Temporary Director</b>
 * <p>
 * Provides a temporary directory for the application to use when files need to be created for
 * exporting them.
 *
 * @since <version tag>
 */
@Component
public class TempDirectory {

  private final Path tempDir;

  public TempDirectory(@Value("${service.host.temp.dir}") String tempDirectoryPath)
      throws IOException {
    Path tempDirPath = Path.of(tempDirectoryPath);
    if (!Files.exists(tempDirPath)) {
      throw new IOException(tempDirectoryPath + " does not exist");
    }
    if (!Files.isWritable(tempDirPath)) {
      throw new IOException(tempDirectoryPath + " is not writable");
    }
    if (!Files.isExecutable(tempDirPath)) {
      throw new IOException(tempDirectoryPath + " is not executable");
    }
    tempDir = tempDirPath;
  }

  /**
   * Creates a new directory in the configured temporary directory location.
   * <p>
   * The method minimises the probability for collisions, so the client does not need to take care
   * about potential existing directories.
   *
   * @return the path to the newly created directory in the app's global temporary directory
   * @throws IOException in case the directory could not be created
   * @since 1.6.0
   */
  public Path createDirectory() throws IOException {
    return Files.createDirectory(tempDir.resolve(UUID.randomUUID().toString()));
  }

}
