package life.qbic.datamanager.files.export.download;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.ss.usermodel.Workbook;

public interface WorkbookDownloadStreamProvider extends DownloadStreamProvider {

  Workbook getWorkbook();

  @Override
  default InputStream getStream() {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      getWorkbook().write(outputStream);
      return new ByteArrayInputStream(outputStream.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
