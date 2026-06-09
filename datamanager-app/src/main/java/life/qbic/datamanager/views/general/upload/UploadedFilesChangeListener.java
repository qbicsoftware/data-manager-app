package life.qbic.datamanager.views.general.upload;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import java.io.Serializable;
import java.util.List;
import life.qbic.datamanager.views.general.upload.UploadedFilesChangeListener.UploadedFilesChangeEvent;

/**
 * Interface for components that display uploaded files and their validation status.
 * <p>
 */
public interface UploadedFilesChangeListener extends
    ComponentEventListener<UploadedFilesChangeEvent> {


  class UploadedFilesChangeEvent extends ComponentEvent<ContentUploadComponent> {

    private final List<FileEntry> files;
    private final List<FileEntry> changedFiles;
    private final ChangeType changeType;


    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source the source component
     */
    public UploadedFilesChangeEvent(ContentUploadComponent source, List<FileEntry> files,
        List<FileEntry> changedFiles, ChangeType changeType) {
      super(source, false);
      this.files = files.stream().toList();
      this.changedFiles = changedFiles.stream().toList();
      this.changeType = changeType;
    }

    public List<FileEntry> files() {
      return files;
    }

    public List<FileEntry> changedFiles() {
      return changedFiles;
    }

    public ChangeType changeType() {
      return changeType;
    }
  }

  /**
   * Describes what kind of change triggered the firing of the event.
   */
  enum ChangeType {
    /** A new file was uploaded. */
    FILE_ADDED,
    /** An uploaded file was removed by the user. */
    FILE_REMOVED
  }

  /**
   * Represents a single uploaded file and its current validation state.
   *
   * @param fileName         the original file name
   * @param mimeType         the MIME type of the file
   * @param size             the file size in bytes
   */
  record FileEntry(String fileName, String mimeType, long size) implements
      Serializable {

  }
}
