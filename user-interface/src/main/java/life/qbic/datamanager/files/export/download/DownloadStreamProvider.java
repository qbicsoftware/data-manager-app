package life.qbic.datamanager.files.export.download;

import java.io.InputStream;

public interface DownloadStreamProvider {

  String getFilename();

  InputStream getStream();

}
