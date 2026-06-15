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

/**
 * A file upload component that supports single and multi-file uploads with automatic in-memory
 * and on-disk storage management.
 *
 * <p>Files uploaded within the configured {@code maxInMemoryBytes} threshold are stored in memory;
 * larger files are temporarily written to disk and cleaned up automatically on component detachment
 * or when files are removed.
 *
 * <p>The component exposes change events via {@link UploadedFilesChangeListener}, allowing consumers
 * to react to files being added or removed. It also provides methods to retrieve uploaded data
 * ({@link #getContent(String)}) and metadata ({@link #getMetadata(String)}) after upload completion.
 *
 * <p><b>Error display:</b> File rejection errors (too big, wrong type, too many files) are displayed
 * inline below the upload area. {@link UnspecificFailedEvent} is fired for network-level failures.
 *
 * <p><b>Thread safety:</b> The internal file data store is backed by a synchronized map.
 * File removal and data storage operations are thread-safe.
 *
 * @author QBiC
 * @see UploadedFilesChangeListener
 * @see UploadedFilesChangeListener.FileEntry
 */
public class ContentUploadComponent extends Div {

  private static final Logger log = LoggerFactory.logger(ContentUploadComponent.class);
  private final long maxInMemoryBytes;
  private final Map<String, FileData> fileDataStore = Collections.synchronizedMap(
      new LinkedHashMap<>()); //linked hash map to keep insertion order
  private final Upload upload;
  private final Div restrictionsArea = new Div();
  private final Div errorArea = new Div();


  /**
   * Holds metadata for an uploaded file, including file name, MIME type, and content length.
   *
   * @param fileName     the name of the uploaded file
   * @param mimeType     the MIME content type of the file
   * @param size         the content length in bytes
   */
  public record UploadedMetadata(String fileName, String mimeType, long size) {


  }
  /**
   * Represents the storage format for uploaded file data.
   *
   * <p>Files within the configured in-memory threshold are stored as in-memory byte arrays;
   * larger files are written to temporary disk storage. The sealed hierarchy ensures safe
   * pattern matching for consumers.
   */
  sealed interface FileData permits InMemory,
      OnDisk {

    /**
     * In-memory file data storing the metadata and raw byte content.
     *
     * <p>Used for files whose size is at or below the configured {@code maxInMemoryBytes} threshold.
     * Implements {@link Serializable} for potential clustering/session replication support.
     */
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
    /**
     * On-disk file data storing the metadata and a path to a temporary file.
     *
     * <p>Used for files whose size exceeds the configured {@code maxInMemoryBytes} threshold.
     * The temporary file is automatically deleted when the component is detached or the file is removed.
     * Implements {@link Serializable} for potential clustering/session replication support.
     */
    record OnDisk(UploadMetadata metadata, Path path) implements
        FileData, Serializable {


    }
    UploadMetadata metadata();

  }

  /**
   * Creates a new upload component with the given upload configuration.
   *
   * <p>Configures file size thresholds, upload constraints (max files, accepted file types),
   * internationalized error messages, and file rejection/removed handlers.
   *
   * <p>Cleanup of temporary on-disk files is handled automatically via a detach listener.
   *
   * @param uploadConfiguration configuration for max in-memory threshold and file size limits
   * @throws NullPointerException if uploadConfiguration is null
   */
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

  /**
   * Sets the maximum allowed file size for uploads, expressed as a {@link DataSize}.
   *
   * <p>Calling this method with a negative or zero value removes the file size restriction.
   * Updates the restrictions display area automatically.
   *
   * @param maxFileSize maximum upload size; zero or negative removes the limit
   * @throws IllegalArgumentException if maxFileSize is negative (pre-condition check)
   */
  public void setMaxFileSize(@NonNull DataSize maxFileSize) {
    if (maxFileSize.toBytes() > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "Maximum file size exceeds Integer.MAX_VALUE bytes and cannot be applied");
    }
    setMaxFileSize(Math.toIntExact(maxFileSize.toBytes()));
  }

  /**
   * Sets the maximum allowed file size for uploads, expressed in bytes.
   *
   * <p>Calling this method with a value less than or equal to zero removes the file size restriction.
   * Updates the restrictions display area automatically.
   *
   * @param maxFileSize maximum upload size in bytes; zero or negative removes the limit
   */
  public void setMaxFileSize(int maxFileSize) {
    if (maxFileSize <= 0) {
      upload.getElement().removeProperty("maxFileSize");
    } else {
      upload.setMaxFileSize(maxFileSize);
    }
    updateRestrictionsDisplay();
  }

  /**
   * Sets internationalization (I18N) labels for the upload component.
   *
   * <p>Configures error messages for file rejection scenarios such as oversized files,
   * incorrect file types, and exceeding the maximum file count.
   *
   * @param uploadI18N internationalization configuration containing error labels
   */
  public void setI18n(UploadI18N uploadI18N) {
    upload.setI18n(uploadI18N);
  }

  /**
   * Sets the list of accepted file types by MIME type and/or extension.
   *
   * <p>Accepts file types such as mime type strings ({@code "application/pdf"}) and/or file
   * extensions ({@code ".pdf"}). File type filtering is performed on the client side.
   * Server-side validation should still be performed by the consumer after upload.
   *
   * @param acceptedFileTypes array of accepted file type strings (MIME types or extensions)
   * @see Upload#setAcceptedFileTypes(String...)
   */
  public void setAcceptedFileTypes(String... acceptedFileTypes) {
    upload.setAcceptedFileTypes(acceptedFileTypes);
  }

  /**
   * Sets the maximum number of files that may be uploaded in a single batch.
   *
   * <p>Exceeding this limit results in a {@link FileRejectedEvent} for additional files.
   *
   * @param maxFiles maximum number of files allowed in one batch upload
   */
  public void setMaxFiles(int maxFiles) {
    upload.setMaxFiles(maxFiles);
  }

  private FileEntry maptoFileEntry(FileData file) {

    return new FileEntry(file.metadata().fileName(),
        file.metadata().contentType(),
        file.metadata().contentLength());
  }

  /**
   * Updates the restrictions display area to show the current maximum file size.
   *
   * <p>Displays the file size restriction text when {@link #getMaxFileSize()} returns a positive value,
   * and hides the area when no restriction is set.
   */
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

  /**
   * Retrieves metadata for the specified uploaded file.
   *
   * @param fileName the name of the uploaded file
   * @return an {@link Optional} containing the file metadata if found, or empty otherwise
   */
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
   * Retrieves an input stream for the content of the specified uploaded file.
   *
   * <p>For in-memory files, a {@link ByteArrayInputStream} is returned. For on-disk files,
   * a {@link FileInputStream} is opened pointing to the temporary file. Callers must close
   * the returned stream to avoid resource leaks.
   *
   * @param fileName the name of the uploaded file
   * @return an {@link Optional} containing the input stream of the file's content, or empty if
   *         the file was not uploaded or a {@link FileNotFoundException} occurred
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

  /**
   * Retrieves the configured maximum file size in bytes.
   *
   * @return the maximum file size; returns -1 if no limit is set
   */
  public long getMaxFileSize() {
    return upload.getMaxFileSize();
  }

  /**
   * Retrieves the configured maximum number of files per upload batch.
   *
   * @return the maximum number of files allowed; returns -1 if unlimited
   */
  public int getMaxFiles() {
    return upload.getMaxFiles();
  }

  /**
   * Registers a listener for file addition and removal change events.
   *
   * @param listener the change listener to register
   * @return a {@link Registration} that can be used to remove the listener
   */
  public Registration addChangeListener(UploadedFilesChangeListener listener) {
    return addListener(UploadedFilesChangeEvent.class, listener);
  }

  /**
   * Registers a listener for unspecific upload failures (e.g., network errors).
   *
   * @param listener the failure event listener to register
   * @return a {@link Registration} that can be used to remove the listener
   */
  public Registration addUnspecificFailureListener(
      ComponentEventListener<UnspecificFailedEvent> listener) {
    return addListener(UnspecificFailedEvent.class, listener);
  }

  /**
   * Registers a listener for file rejection events triggered when files violate upload constraints.
   *
   * <p>{@inheritDoc}
   */
  public Registration addFileRejectedListener(
      ComponentEventListener<FileRejectedEvent> listener) {
    return upload.addFileRejectedListener(listener);
  }

  /**
   * Registers a listener for file removal events triggered when the user removes a file from the upload queue.
   *
   * <p>{@inheritDoc}
   */
  public Registration addFileRemovedListener(
      ComponentEventListener<FileRemovedEvent> listener) {
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
