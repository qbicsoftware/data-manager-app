package life.qbic.datamanager.views.general.upload;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.component.upload.UploadI18N.Error;
import com.vaadin.flow.shared.Registration;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * A component supporting a file upload and a display of the currently uploaded file.
 * <p>
 * Only supports uploading one file at a time, decreasing complexity of display behaviour and data.
 *
 * @since 1.4.0
 */
public class UploadWithDisplay extends Div {

  private final Upload upload;
  private final Div errorArea;
  private final Div displayContainer;
  private final Span displayContainerTitle;
  private final FileMemoryBuffer fileMemoryBuffer;

  public UploadWithDisplay(int maxFileSize) {
    this(maxFileSize, new FileType[]{});
  }

  public record FileType(String extension, String mimeType) {
  }

  public UploadWithDisplay(int maxFileSize, FileType[] fileTypes) {
    errorArea = new Div();
    displayContainer = new Div();
    fileMemoryBuffer = new FileMemoryBuffer();
    upload = new Upload();
    var restrictions = new Div();

    errorArea.addClassName("error-message-box");
    displayContainer.addClassName("uploaded-items-section");
    displayContainer.setVisible(false);

    displayContainerTitle = new Span("Uploaded file");
    displayContainerTitle.addClassName("section-title");
    displayContainerTitle.setVisible(false);

    restrictions.addClassName("restrictions");
    var allowedExtensions = Arrays.stream(fileTypes)
        .map(FileType::extension)
        .map(it -> it.startsWith(".") ? it : "." + it)
        .distinct()
        .toList();
    if (fileTypes.length > 0) {
      restrictions.add(new Div("Supported file formats: " + String.join(", ", allowedExtensions)));
      restrictions.add(new Div("Maximum file size: " + formatFileSize(maxFileSize)));
    }

    upload.setAcceptedFileTypes(
        fileTypes.length > 0 ? Arrays.stream(fileTypes)
            .map(FileType::mimeType)
            .filter(it -> !it.isBlank())
            .toArray(String[]::new) : null);

    upload.setMaxFileSize(maxFileSize);
    upload.setMaxFiles(1); // we only allow one file
    upload.setReceiver(fileMemoryBuffer);
    upload.addFileRemovedListener(fileRemovedEvent -> {
      fileMemoryBuffer.clear();
      displayContainer.removeAll();
      displayContainerTitle.setVisible(false);
      displayContainer.setVisible(false);
      fireEvent(new UploadRemovedEvent(this, fileRemovedEvent.isFromClient()));
    });

    Error errorTranslation = new Error();
    errorTranslation.setFileIsTooBig(
        "The provided file is too big. Please make sure your file is smaller than "
            + formatFileSize(maxFileSize));
    errorTranslation.setTooManyFiles("Please upload one file at a time.");
    errorTranslation.setIncorrectFileType(
        "Unsupported file type. Supported file types are " + String.join(", ", allowedExtensions));
    UploadI18N uploadI18N = new UploadI18N();
    uploadI18N.setError(errorTranslation);
    upload.setI18n(uploadI18N);

    upload.addSucceededListener(it -> fireEvent(new SucceededEvent(this, it.isFromClient())));
    upload.addFailedListener(it -> fireEvent(new FailedEvent(this, it.isFromClient())));
    upload.addFileRejectedListener(fileRejected -> {
      errorArea.setVisible(true);
      errorArea.setText(fileRejected.getErrorMessage());
    });
    upload.addFinishedListener(it -> errorArea.setVisible(false));
    add(errorArea, upload, restrictions, displayContainerTitle, displayContainer);
  }

  private static String formatFileSize(int bytes) {
    if (bytes > Math.pow(1024, 2)) {
      return bytes / Math.pow(1024, 2) + " MB";
    }
    if (bytes > 1024) {
      return bytes / 1024d + " KB";
    }
    return bytes + " B";
  }

  public Registration addSuccessListener(ComponentEventListener<SucceededEvent> listener) {
    return addListener(SucceededEvent.class, listener);
  }

  public Registration addFailureListener(ComponentEventListener<FailedEvent> listener) {
    return addListener(FailedEvent.class, listener);
  }

  public Registration addRemovedListener(ComponentEventListener<UploadRemovedEvent> listener) {
    return addListener(UploadRemovedEvent.class, listener);
  }

  /**
   * Sets component to display the currently available file. This component will be cleared when the
   * file is removed from the upload component.
   *
   * @param uploadProgressDisplay a component to display
   * @param <T>                   the type of component to display
   */
  public <T extends Component> void setDisplay(T uploadProgressDisplay) {
    displayContainer.removeAll();
    if (uploadProgressDisplay == null) {
      displayContainerTitle.setVisible(false);
      displayContainer.setVisible(false);
      return;
    }
    uploadProgressDisplay.addClassName("uploaded-item");
    displayContainer.add(uploadProgressDisplay);
    displayContainerTitle.setVisible(true);
    displayContainer.setVisible(true);
  }

  /**
   * Removes component from display.
   * <p>
   * If the component is not displayed, does nothing.
   * @param display
   * @param <T>
   */
  public <T extends Component> void removeDisplay(T display) {
    displayContainer.remove(display);
    displayContainerTitle.setVisible(false);
    displayContainer.setVisible(false);
    fireEvent(new UploadRemovedEvent(this, false));
  }

  /**
   * Clears the upload as well as resets the display.
   */
  public void clear() {
    upload.clearFileList();
    displayContainer.removeAll();
    displayContainerTitle.setVisible(false);
    displayContainer.setVisible(false);
    errorArea.removeAll();
    fileMemoryBuffer.clear();
  }

  /**
   * @return the uploaded data or {@link Optional#empty()} is nothing was uploaded yet.
   */
  public Optional<UploadedData> getUploadedData() {
    if (fileMemoryBuffer.hasUploadedData()) {
      var fileName = fileMemoryBuffer.getFileName().orElseThrow();
      var inputStream = fileMemoryBuffer.getInputStream().orElseThrow();
      var mimeType = fileMemoryBuffer.getMimeType().orElseThrow();
      return Optional.of(new UploadedData(fileName, inputStream, mimeType));
    }
    return Optional.empty();
  }

  /**
   * A representation of uploaded data
   * @param fileName the name of the uploaded file
   * @param inputStream the contents of the file
   * @param mimeType the type of file
   */
  public record UploadedData(String fileName, InputStream inputStream, String mimeType) {

    /**
     * {@inheritDoc}
     * <p>
     * <b>This method ignores the file content and only compares filename and mime tpye.</b>
     * @param o   the reference object with which to compare.
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof UploadedData that)) {
        return false;
      }

      return Objects.equals(fileName, that.fileName) && Objects.equals(mimeType,
          that.mimeType);
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(fileName);
      result = 31 * result + Objects.hashCode(mimeType);
      return result;
    }
  }

  public static class SucceededEvent extends ComponentEvent<UploadWithDisplay> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public SucceededEvent(UploadWithDisplay source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class FailedEvent extends ComponentEvent<UploadWithDisplay> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public FailedEvent(UploadWithDisplay source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * Indicates that the upload was removed.
   */
  public static class UploadRemovedEvent extends ComponentEvent<UploadWithDisplay> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public UploadRemovedEvent(UploadWithDisplay source, boolean fromClient) {
      super(source, fromClient);
    }
  }

}
