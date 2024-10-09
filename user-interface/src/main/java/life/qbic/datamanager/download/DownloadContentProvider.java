package life.qbic.datamanager.download;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Provides content and file name for any files created from data and metadata.
 */
public interface DownloadContentProvider {

  public byte[] getContent();
  public String getFileName();

  class XLSXDownloadContentProvider implements DownloadContentProvider {

    private final String fileName;
    private final Workbook workbook;

    public XLSXDownloadContentProvider(String fileName, Workbook workbook) {
      this.fileName = fileName;
      this.workbook = workbook;
    }

    @Override
    public byte[] getContent() {
      try (ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()) {
        workbook.write(arrayOutputStream);
        return arrayOutputStream.toByteArray();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String getFileName() {
      return fileName;
    }
  }
}
