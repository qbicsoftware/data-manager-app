package life.qbic.datamanager.files.export.download;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public interface FileDownloadStreamProvider extends DownloadStreamProvider {

  File getFile();

  @Override
  default InputStream getStream() {
    try {
      return new ByteArrayInputStream(Files.readAllBytes(getFile().toPath()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
