package life.qbic.datamanager.files.export.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public interface FileDownloadStreamProvider extends DownloadStreamProvider {

  File getFile();

  @Override
  default InputStream getStream() {
    try {
      return new RemovingFileInputStream(getFile());
//      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
//          Files.readAllBytes(getFile().toPath().toAbsolutePath()));
//      return byteArrayInputStream;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  class RemovingFileInputStream extends FileInputStream {

    private File file;

    public RemovingFileInputStream(File file)
        throws FileNotFoundException {
      super(file);
      this.file = file;
    }

    @Override
    public void close() throws IOException {
      try {
        super.close();
        Files.delete(file.toPath());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    }
  }
}
