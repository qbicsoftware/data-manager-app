package life.qbic.datamanager.views.general.upload;

import java.util.List;

/**
 * Receives status updates about uploaded files.
 * <p>
 * Implementations display the current list of files and their validation status.
 */
public interface UploadFileDisplay {

  void onFilesChanged(List<FileEntry> files);

  record FileEntry(String fileName, String mimeType, long size, ValidationStatus validationStatus,
                   String statusMessage) {

  }

  enum ValidationStatus {
    UPLOADED,
    VALIDATING,
    SUCCESS,
    FAILED
  }
}
