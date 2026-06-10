package life.qbic.datamanager.views.general.upload;


import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.component.upload.FileRemovedEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.component.upload.UploadI18N.Error;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import com.vaadin.flow.shared.Registration;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.FileSizeFormatter;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.datamanager.views.general.upload.ContentUploadComponent.FileData.InMemory;
import life.qbic.datamanager.views.general.upload.ContentUploadComponent.FileData.OnDisk;
import life.qbic.datamanager.views.general.upload.UploadedFilesChangeListener.ChangeType;
import life.qbic.datamanager.views.general.upload.UploadedFilesChangeListener.FileEntry;
import life.qbic.datamanager.views.general.upload.UploadedFilesChangeListener.UploadedFilesChangeEvent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.util.unit.DataSize;

public class ContentUploadComponent extends Div {

  private static final Logger log = LoggerFactory.logger(ContentUploadComponent.class);
  private final long maxInMemoryBytes;
  private final Map<String, FileData> fileDataStore = Collections.synchronizedMap(
      new LinkedHashMap<>()); //linked hash map to keep insertion order
  private final Upload upload;
  private final Div restrictionsArea = new Div();
  private final Div errorArea = new Div();


  public record UploadedMetadata(String fileName, String mimeType, long size) {


  }
  sealed interface FileData permits InMemory,
      OnDisk {

    record InMemory(UploadMetadata metadata, byte[] data)
        implements FileData, Serializable {

      @Override
      public boolean equals(Object o) {
        //need to overwrite as array field is present
        if (o instanceof InMemory(UploadMetadata otherMetadata, byte[] otherData)) {
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
    record OnDisk(UploadMetadata metadata, Path path) implements
        FileData, Serializable {


    }
    UploadMetadata metadata();

  }

  public ContentUploadComponent(UploadConfiguration uploadConfiguration) {
    //ensure cleanup after detachment of component
    addDetachListener(event -> deletePendingFiles());
    addClassNames("upload-component");
    maxInMemoryBytes = uploadConfiguration.maxInMemoryThreshold().toBytes();

    restrictionsArea.addClassNames("restrictions", "extra-small-body-text", "color-secondary");

    errorArea.addClassNames("upload-component-error");
    errorArea.setVisible(false);

    upload = new Upload();

    Error errorTranslation = new Error();
    errorTranslation.setFileIsTooBig(
        "The provided file is too big. Please try again with a smaller file.");
    errorTranslation.setTooManyFiles(
        "Too many files uploaded. Please try uploading less files.");
    errorTranslation.setIncorrectFileType(
        "Incorrect file type detected. Please ensure your file is intact and supported."
    );
    UploadI18N uploadI18N = new UploadI18N();
    uploadI18N.setError(errorTranslation);
    upload.setI18n(uploadI18N);

    UploadHandler handler = event -> {
      //TODO in vaadin25.1 the upload event has a reject method. use that instead.
      try {
        long fileSize = event.getFileSize();
        //decide how to handle the upload based on filesize
        UploadHandler delegateHandler = fileSize <= maxInMemoryBytes
            ? UploadHandler.inMemory(this::process)
            : UploadHandler.toTempFile(this::process);
        delegateHandler.handleUploadRequest(event);
      } catch (IOException e) {
        log.warn("Unexpected upload failure: " + e.getMessage());
        fireEvent(new UnspecificFailedEvent(this, false, e));
      }
    };
    upload.setUploadHandler(handler);

    upload.addFileRemovedListener(event -> removeFile(event.getFileName()));
    upload.addAllFinishedListener(event -> {
      errorArea.setVisible(false);
      errorArea.removeAll();
    });
    /**
     * FIXME:
     *   Please be advised that when even 1 file out of all files is uploaded, no rejection messages are shown.
     *   This needs to be fixed in vaadin 25.1 where the upload handler/ upload event got necessary methods
     */
    upload.addFileRejectedListener(event ->
    {
      log.warn("Failed to upload " + event.getFileName() +
          " Reason: " + event.getErrorMessage());
      Span fileNameSpan = new Span(event.getFileName());
      fileNameSpan.addClassNames("bold");
      Span errorMessage = new Span(event.getErrorMessage());
      errorArea.add(new Div(fileNameSpan, new Span(" rejected because: "),
          errorMessage));
      errorArea.setVisible(true);
    });

    if (!uploadConfiguration.maxFileSize().isNegative()) {
      setMaxFileSize(uploadConfiguration.maxFileSize());
    }
    add(errorArea, upload, restrictionsArea);
  }

  public void setMaxFileSize(@NonNull DataSize maxFileSize) {
    setMaxFileSize(Math.toIntExact(maxFileSize.toBytes()));
  }

  public void setMaxFileSize(int maxFileSize) {
    if (maxFileSize <= 0) {
      upload.getElement().removeProperty("maxFileSize");
    } else {
      upload.setMaxFileSize(maxFileSize);
    }
    updateRestrictionsDisplay();
  }

  public void setI18n(UploadI18N uploadI18N) {
    upload.setI18n(uploadI18N);
  }

  /**
   * @param acceptedFileTypes a list of accepted file types
   * @see Upload#setAcceptedFileTypes(String...)
   */
  public void setAcceptedFileTypes(String... acceptedFileTypes) {
    upload.setAcceptedFileTypes(acceptedFileTypes);
  }

  public void setMaxFiles(int maxFiles) {
    upload.setMaxFiles(maxFiles);
  }


  private FileEntry maptoFileEntry(FileData file) {

    return new FileEntry(file.metadata().fileName(),
        file.metadata().contentType(),
        file.metadata().contentLength());
  }

  protected void updateRestrictionsDisplay() {
    if (getMaxFileSize() > 0) {
      restrictionsArea.removeAll();
      Div restriction = new Div(
          "Maximum file size is " + FileSizeFormatter.formatBytes(getMaxFileSize()));
      restriction.addClassNames("restriction");
      restrictionsArea.add(restriction);
      restrictionsArea.setVisible(true);
    } else {
      restrictionsArea.removeAll();
      restrictionsArea.setVisible(false);
    }
  }
  private synchronized void deletePendingFiles() {
    fileDataStore.values().forEach(
        it -> {
          if (it instanceof OnDisk onDisk) {
            deleteFileFromDisk(onDisk);
          }
        }
    );
  }

  private synchronized void removeFile(String fileName) {
    assert fileDataStore.containsKey(
        fileName) : "The removed file was uploaded before";
    if (fileDataStore.containsKey(fileName)) {
      FileData trackedFile = fileDataStore.get(fileName);
      if (trackedFile instanceof OnDisk onDisk) {
        deleteFileFromDisk(onDisk);
      }
      fileDataStore.remove(fileName);
      fireRemoveChange(List.of(trackedFile));
    }
  }

  private static void deleteFileFromDisk(OnDisk onDisk) {
    try {
      Files.deleteIfExists(onDisk.path());
    } catch (IOException e) {
      log.warn("Temporarily uploaded file %s was not deleted. Reason:%s".formatted(
          onDisk.path().toAbsolutePath().toString(), e.getMessage()));
    }
  }

  private void fireAddedChange(List<FileData> files) {
    fireEvent(new UploadedFilesChangeEvent(this,
        fileDataStore.values().stream().map(this::maptoFileEntry).toList(),
        files.stream().map(this::maptoFileEntry).toList(),
        ChangeType.FILE_ADDED));
  }

  private void fireRemoveChange(List<FileData> files) {
    fireEvent(new UploadedFilesChangeEvent(this,
        fileDataStore.values().stream().map(this::maptoFileEntry).toList(),
        files.stream().map(this::maptoFileEntry).toList(),
        ChangeType.FILE_REMOVED));
  }

  private void process(UploadMetadata metadata, File file) {
    setData(new OnDisk(metadata, file.toPath()));
  }

  private void process(UploadMetadata metadata, byte[] data) {
    setData(new InMemory(metadata, data));
  }

  private synchronized void setData(FileData fileData) {
    String fileName = fileData.metadata().fileName();
    fileDataStore.put(fileName, fileData);
    fireAddedChange(List.of(fileData));
  }



  protected boolean isEmpty() {
    return fileDataStore.isEmpty();
  }

  /**
   * Lists the names of all uploaded files. The ordering of names is not guaranteed to be
   * preserved.
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

  public long getMaxFileSize() {
    return upload.getMaxFileSize();
  }

  public int getMaxFiles() {
    return upload.getMaxFiles();
  }

  public Registration addChangeListener(UploadedFilesChangeListener listener) {
    return addListener(UploadedFilesChangeEvent.class, listener);
  }

  public Registration addUnspecificFailureListener(
      ComponentEventListener<UnspecificFailedEvent> listener) {
    return addListener(UnspecificFailedEvent.class, listener);
  }

  public Registration addFileRejectedListener(ComponentEventListener<FileRejectedEvent> listener) {
    return upload.addFileRejectedListener(listener);
  }

  public Registration addFileRemovedListener(ComponentEventListener<FileRemovedEvent> listener) {
    return upload.addFileRemovedListener(listener);
  }

  public static class UnspecificFailedEvent extends ComponentEvent<ContentUploadComponent> {

    private final Exception cause;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public UnspecificFailedEvent(ContentUploadComponent source, boolean fromClient,
        Exception cause) {
      super(source, fromClient);
      this.cause = cause;
    }

    public Exception getCause() {
      return cause;
    }
  }

}
