package life.qbic.datamanager.views.general.upload;


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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.datamanager.views.general.upload.SmartUploadComponent.FileData.InMemory;
import life.qbic.datamanager.views.general.upload.SmartUploadComponent.FileData.OnDisk;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

public class SmartUploadComponent extends Div {

  private static final Logger log = LoggerFactory.logger(SmartUploadComponent.class);
  private final long maxInMemoryBytes;
  private final Map<String, FileData> fileDataStore = Collections.synchronizedMap(
      new LinkedHashMap<>()); //linked hash map to keep insertion order
  private final List<UploadFileDisplay> displays = new ArrayList<>();
  private final Upload upload;

  public record UploadedMetadata(String fileName, String mimeType, long size) {

  }

  sealed interface FileData permits InMemory,
      OnDisk {

    record InMemory(byte[] data, UploadMetadata metadata)
        implements FileData {

      @Override
      public boolean equals(Object o) {
        //need to overwrite as array field is present
        if (o instanceof InMemory(byte[] otherData, UploadMetadata otherMetadata)) {
          return Arrays.equals(data, otherData) && Objects.equals(metadata, otherMetadata);
        }
        return false;
      }

      @Override
      public int hashCode() {
        //need to overwrite as array field is present
        int result = Arrays.hashCode(data);
        result = 31 * result + Objects.hashCode(metadata);
        return result;
      }
    }

    record OnDisk(Path path, UploadMetadata metadata) implements
        FileData {

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
      assert fileDataStore.containsKey(
          event.getFileName()) : "The removed file was uploaded before";
      clearData();
    });
    upload.addAllFinishedListener(event -> {
    });
    add(upload);
  }

  private synchronized void clearData() {
    //clear temporary files
    fileDataStore.forEach((fileName, fileData) -> {
      if (fileData instanceof OnDisk onDisk) {
        try {
          Files.deleteIfExists(onDisk.path());
        } catch (IOException e) {
          log.warn("Temporarily uploaded file %s was not deleted. Reason:%s".formatted(
              onDisk.path().toAbsolutePath().toString(), e.getMessage()));
          throw new RuntimeException(e);
        }
      }
    });
    fileDataStore.clear();
  }

  private synchronized void setData(UploadMetadata metadata, File file) {
    if (fileDataStore.containsKey(metadata.fileName())) {
      throw new RuntimeException("File with name " + metadata.fileName() + " already exists");
    }
    fileDataStore.put(metadata.fileName(), new OnDisk(file.toPath(), metadata));
  }

  private synchronized void setData(UploadMetadata metadata, byte[] data) {
    if (fileDataStore.containsKey(metadata.fileName())) {
      throw new RuntimeException("File with name " + metadata.fileName() + " already exists");
    }
    fileDataStore.put(metadata.fileName(), new InMemory(data, metadata));
  }


  protected boolean isEmpty() {
    return fileDataStore.isEmpty();
  }

  /**
   * Lists the names of all uploaded files. The ordering of names is not guaranteed to be preserved.
   *
   * @return a list of names of uploaded files.
   */
  public List<String> getFileNames() {
    var keys = fileDataStore.keySet();
    return keys.stream()
        .toList();
  }

  public Optional<UploadedMetadata> getMetadata(String fileName) {
    if (!fileDataStore.containsKey(fileName)) {
      return Optional.empty();
    }
    FileData entry = fileDataStore.get(fileName);
    return switch (entry) {
      case InMemory inMemory -> Optional.of(new UploadedMetadata(inMemory.metadata().fileName(),
          inMemory.metadata().contentType(), inMemory.metadata().contentLength()));
      case OnDisk onDisk -> Optional.of(
          new UploadedMetadata(onDisk.metadata().fileName(), onDisk.metadata().contentType(),
              onDisk.metadata().contentLength()));
    };

  }

  /**
   * Request the data for a specific file
   *
   * @param fileName the name of the uploaded file.
   * @return the uploaded data or {@link Optional#empty()} if not data was found for the provided
   * fileName.
   */
  public Optional<InputStream> getContent(String fileName) {
    if (isEmpty()) {
      return Optional.empty();
    }
    if (!fileDataStore.containsKey(fileName)) {
      return Optional.empty();
    }
    FileData fileData = fileDataStore.get(fileName);
    return switch (fileData) {
      case InMemory inMemory -> Optional.of(new ByteArrayInputStream(inMemory.data()));
      case OnDisk onDisk -> {
        InputStream inputStream = null;
        try {
          inputStream = new FileInputStream(onDisk.path().toFile());
        } catch (FileNotFoundException e) {
          log.warn("File %s was not found but was expected to be there. Reason:%s".formatted(
              onDisk.path().toAbsolutePath().toString(), e.getMessage()));
        }
        yield Optional.ofNullable(inputStream);
      }
    };
  }
}
