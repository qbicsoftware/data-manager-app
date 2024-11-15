package life.qbic.datamanager.download;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import life.qbic.application.commons.ApplicationException;
import life.qbic.logging.api.Logger;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Provides content and file name for any files created from data and metadata.
 */
public interface DownloadContentProvider {

  byte[] getContent();
  String getFileName();

  class XLSXDownloadContentProvider implements DownloadContentProvider {

    private final String fileName;
    private final Workbook workbook;
    private static final Logger log = logger(XLSXDownloadContentProvider.class);

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
        log.error(e.getMessage(), e.getCause());
        throw new ApplicationException("Retrieving content from the download provider failed");
      }
    }

    @Override
    public String getFileName() {
      return fileName;
    }
  }
}
