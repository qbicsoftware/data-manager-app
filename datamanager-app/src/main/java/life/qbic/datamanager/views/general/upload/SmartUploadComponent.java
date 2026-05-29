package life.qbic.datamanager.views.general.upload;


import static java.util.Objects.nonNull;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

public class SmartUploadComponent extends Div {

  private static final Logger log = LoggerFactory.logger(SmartUploadComponent.class);
  private final long maxInMemoryBytes;

  private UploadMetadata uploadMetadata;

  private byte[] uploadedBytes;
  private Path tempFile;

  private Upload upload;

  /**
   * A data record representing uploaded data.
   *
   * @param fileName the name of the uploaded file
   * @param mimeType the type of the file
   * @param data     an {@link InputStream} serving the data
   */
  public record UploadedData(String fileName, String mimeType, InputStream data) {

    public UploadedData {
      Objects.requireNonNull(fileName);
      Objects.requireNonNull(mimeType);
      Objects.requireNonNull(data);
    }
  }


  public SmartUploadComponent(UploadConfiguration uploadConfiguration) {
    maxInMemoryBytes = uploadConfiguration.maxInMemoryBytes();

    UploadHandler handler = event -> {
      long fileSize = event.getFileSize();

      //decide how to handle the upload based on filesize
      UploadHandler delegateHandler = fileSize <= maxInMemoryBytes
          ? UploadHandler.inMemory(this::setData)
          : UploadHandler.toTempFile(this::setData);
      delegateHandler.handleUploadRequest(event);
    };
    upload = new Upload(handler);
    upload.addFileRemovedListener(event -> {
      assert nonNull(uploadMetadata) && event.getFileName().equals(
          uploadMetadata.fileName()) : "The removed file matches the uploaded file name";
      clearData();
    });
  }

  private synchronized void clearData() {
    uploadMetadata = null;
    uploadedBytes = null;
    try {
      Files.deleteIfExists(this.tempFile);
    } catch (IOException e) {
      log.warn("Temporarily uploaded file %s was not deleted. Reason:%s".formatted(
          tempFile.toAbsolutePath().toString(), e.getMessage()));
      throw new RuntimeException(e);
    }
  }

  private synchronized void setData(UploadMetadata metadata, File file) {
    clearData();
    // Capture File for later retrieval
    this.tempFile = file.toPath();
    this.uploadMetadata = metadata;
  }

  private synchronized void setData(UploadMetadata metadata, byte[] data) {
    clearData();
    // Capture byte[] for later retrieval
    this.uploadedBytes = data;
    this.uploadMetadata = metadata;
  }


  protected boolean isInMemory() {
    return uploadedBytes != null;
  }

  protected boolean isEmpty() {
    return uploadedBytes == null || (tempFile == null || Files.notExists(tempFile));
  }

  public Optional<UploadedData> getUploadedData() {
    if (isEmpty()) {
      return Optional.empty();
    }
    if (isInMemory()) {
      return Optional.of(new UploadedData(uploadMetadata.fileName(), uploadMetadata.contentType(),
          new ByteArrayInputStream(uploadedBytes)));
    } else {
      try {
        return Optional.of(new UploadedData(uploadMetadata.fileName(), uploadMetadata.contentType(),
            new FileInputStream(tempFile.toFile())));
      } catch (FileNotFoundException e) {
        log.warn("File %s was not found but was expected to be there. Reason:%s".formatted(
            tempFile.toAbsolutePath().toString(), e.getMessage()));
        return Optional.empty();
      }
    }
  }
}
