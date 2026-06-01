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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import life.qbic.datamanager.configuration.UploadConfiguration;
import life.qbic.datamanager.views.general.upload.ContentUploadComponent.FileData.InMemory;
import life.qbic.datamanager.views.general.upload.ContentUploadComponent.FileData.OnDisk;
import life.qbic.datamanager.views.general.upload.ContentUploadComponent.ValidationStatus.Failure;
import life.qbic.datamanager.views.general.upload.ContentUploadComponent.ValidationStatus.None;
import life.qbic.datamanager.views.general.upload.ContentUploadComponent.ValidationStatus.Success;
import life.qbic.datamanager.views.general.upload.ContentUploadComponent.ValidationStatus.Validating;
import life.qbic.datamanager.views.general.upload.UploadFileDisplay.ChangeType;
import life.qbic.datamanager.views.general.upload.UploadFileDisplay.FileEntry;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

public class ContentUploadComponent extends Div {

  private static final Logger log = LoggerFactory.logger(ContentUploadComponent.class);
  private final long maxInMemoryBytes;
  private final Map<String, TrackedFile> fileDataStore = Collections.synchronizedMap(
      new LinkedHashMap<>()); //linked hash map to keep insertion order
  private final Set<UploadFileDisplay> displays = new HashSet<>();
  private final Upload upload;


  sealed interface ValidationStatus permits Failure, None, Success, Validating {

    record None() implements ValidationStatus {

    }

    record Validating() implements ValidationStatus {

    }

    record Success(String message) implements ValidationStatus {

    }

    record Failure(String message) implements ValidationStatus {

    }
  }

  public record UploadedMetadata(String fileName, String mimeType, long size) {

  }

  record TrackedFile(FileData data, ValidationStatus validationStatus) {

  }

  sealed interface FileData permits InMemory,
      OnDisk {

    record InMemory(UploadMetadata metadata, byte[] data)
        implements FileData {

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
        FileData {

    }

    UploadMetadata metadata();
  }

  public ContentUploadComponent(UploadConfiguration uploadConfiguration) {
    //ensure cleanup after detachment of component
    addDetachListener(event -> deletePendingFiles());

    maxInMemoryBytes = uploadConfiguration.maxInMemoryBytes();
    upload = new Upload();
    UploadHandler handler = event -> {
      long fileSize = event.getFileSize();
      var fileName = event.getFileName();
      //if file with name already exists, it is replaced.
      if (fileDataStore.containsKey(fileName)) {
        //seems the only way to change the error message
        String message = "File with name " + fileName + " already exists";
        //todo in vaadin 25 upload can be rejected with event.reject(message)
        throw new RuntimeException(message);
      }

      //decide how to handle the upload based on filesize
      UploadHandler delegateHandler = fileSize <= maxInMemoryBytes
          ? UploadHandler.inMemory(this::setData)
          : UploadHandler.toTempFile(this::setData);
      delegateHandler.handleUploadRequest(event);
    };
    upload.setUploadHandler(handler);
    upload.addFileRemovedListener(event -> removeFile(event.getFileName()));
    upload.addAllFinishedListener(event -> {
    });
    add(upload);
  }

  private FileEntry maptoFileEntry(TrackedFile trackedFile) {
    var validationMessage = switch (trackedFile.validationStatus()) {
      case Failure(String message) -> message;
      case None ignored -> "";
      case Success(String message) -> message;
      case Validating ignored -> "";
    };

    return new FileEntry(trackedFile.data().metadata().fileName(),
        trackedFile.data().metadata().contentType(),
        trackedFile.data().metadata().contentLength(),
        mapValidationStatus(trackedFile.validationStatus()),
        validationMessage
    );
  }

  private synchronized void deletePendingFiles() {
    fileDataStore.values().forEach(
        it -> {
          if (it.data() instanceof OnDisk onDisk) {
            deleteFileFromDisk(onDisk);
          }
        }
    );
  }

  private synchronized void removeFile(String fileName) {
    assert fileDataStore.containsKey(
        fileName) : "The removed file was uploaded before";
    if (fileDataStore.containsKey(fileName)) {
      TrackedFile trackedFile = fileDataStore.get(fileName);
      if (trackedFile.data() instanceof OnDisk onDisk) {
        deleteFileFromDisk(onDisk);
      }
      fileDataStore.remove(fileName);
      notifyDisplayFilesRemoved(List.of(maptoFileEntry(trackedFile)));
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

  private void guardDuplicateFiles(String fileName) {
    if (fileDataStore.containsKey(fileName)) {
      throw new RuntimeException("File with name " + fileName + " already exists");
    }
  }

  private void notifyDisplayFilesAdded(List<FileEntry> files) {
    for (UploadFileDisplay display : displays) {
      display.onFilesChanged(files, ChangeType.FILE_ADDED);
    }
  }

  private void notifyDisplayFilesRemoved(List<FileEntry> files) {
    for (UploadFileDisplay display : displays) {
      display.onFilesChanged(files, ChangeType.FILE_REMOVED);
    }
  }

  private synchronized void setData(UploadMetadata metadata, File file) {
    guardDuplicateFiles(metadata.fileName());
    TrackedFile trackedFile = new TrackedFile(new OnDisk(metadata, file.toPath()), new None());
    fileDataStore.put(metadata.fileName(), trackedFile);
    notifyDisplayFilesAdded(List.of(maptoFileEntry(trackedFile)));
  }

  private synchronized void setData(UploadMetadata metadata, byte[] data) {
    guardDuplicateFiles(metadata.fileName());
    TrackedFile trackedFile = new TrackedFile(new InMemory(metadata, data), new None());
    fileDataStore.put(metadata.fileName(), trackedFile);
    notifyDisplayFilesAdded(List.of(maptoFileEntry(trackedFile)));

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
    FileData entry = fileDataStore.get(fileName).data();
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
    FileData fileData = fileDataStore.get(fileName).data();
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

  public void addDisplay(UploadFileDisplay display) {
    List<FileEntry> allFiles = fileDataStore.values()
        .stream()
        .map(this::maptoFileEntry)
        .toList();
    display.onFilesChanged(allFiles, ChangeType.FILE_ADDED);
    List<FileEntry> validating = allFiles.stream().filter(
        it -> it.validationStatus().equals(UploadFileDisplay.ValidationStatus.VALIDATING)).toList();
    display.onFilesChanged(validating, ChangeType.VALIDATION_STARTED);
    List<FileEntry> validated = allFiles.stream().filter(
        it -> it.validationStatus().equals(UploadFileDisplay.ValidationStatus.SUCCESS)
            || it.validationStatus().equals(
            UploadFileDisplay.ValidationStatus.FAILED)).toList();
    display.onFilesChanged(validated, ChangeType.VALIDATION_COMPLETED);
    //only add at the end to ensure initial state is loaded
    displays.add(display);
  }

  public void removeDisplay(UploadFileDisplay display) {
    displays.remove(display);
  }

  UploadFileDisplay.ValidationStatus mapValidationStatus(ValidationStatus validationStatus) {
    return switch (validationStatus) {
      case Failure failure -> UploadFileDisplay.ValidationStatus.FAILED;
      case None none -> UploadFileDisplay.ValidationStatus.UPLOADED;
      case Success success -> UploadFileDisplay.ValidationStatus.SUCCESS;
      case Validating validating -> UploadFileDisplay.ValidationStatus.VALIDATING;
    };
  }
}
