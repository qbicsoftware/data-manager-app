package life.qbic.datamanager.files.export.download;

import java.io.InputStream;
import java.util.Optional;

public interface DownloadStreamProvider {

  String getFilename();

  InputStream getStream();

  String getContentType();

  Optional<Long> contentLength();
}
