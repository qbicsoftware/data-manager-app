package life.qbic.datamanager.files.export.download;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public interface ByteArrayDownloadStreamProvider extends DownloadStreamProvider {

  byte[] getBytes();

  @Override
  default InputStream getStream() {
    return new ByteArrayInputStream(getBytes());
  }
}
