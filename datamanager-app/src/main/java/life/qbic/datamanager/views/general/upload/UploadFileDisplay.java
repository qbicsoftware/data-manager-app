package life.qbic.datamanager.views.general.upload;

import java.util.List;

/**
 * Interface for components that display uploaded files and their validation status.
 * <p>
 * Implementations render an up-to-date list of {@link FileEntry} objects and {@link ValidationStatus},
 * receiving a full snapshot on every change.
 */
public interface UploadFileDisplay {

  /**
   * Called whenever the set of uploaded files or their validation state changes.
   *
   * @param files      the complete current list of uploaded file entries
   * @param changeType the type of change that triggered this notification
   */
  void onFilesChanged(List<FileEntry> files, ChangeType changeType);

  /**
   * Describes what kind of change triggered an {@link #onFilesChanged} call.
   */
  enum ChangeType {
    /** A new file was uploaded. */
    FILE_ADDED,
    /** An uploaded file was removed by the user. */
    FILE_REMOVED,
    /** Validation of an uploaded file has begun. */
    VALIDATION_STARTED,
    /** Validation of an uploaded file has finished, regardless of outcome. */
    VALIDATION_COMPLETED
  }

  /**
   * Represents a single uploaded file and its current validation state.
   *
   * @param fileName         the original file name
   * @param mimeType         the MIME type of the file
   * @param size             the file size in bytes
   * @param validationStatus the current validation state
   * @param statusMessage    a human-readable message describing the current status
   */
  record FileEntry(String fileName, String mimeType, long size, ValidationStatus validationStatus,
                   String statusMessage) {

  }

  /**
   * The lifecycle state of a file's validation process.
   */
  enum ValidationStatus {
    /** The file has been uploaded but not yet validated. */
    UPLOADED,
    /** The file is currently being validated. */
    VALIDATING,
    /** The file passed validation. */
    SUCCESS,
    /** The file failed validation. */
    FAILED
  }
}
