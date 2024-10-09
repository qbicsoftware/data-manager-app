package life.qbic.datamanager.views.general.upload;

import com.vaadin.flow.component.upload.MultiFileReceiver;
import com.vaadin.flow.component.upload.receivers.FileData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <b>Editable multi-file memory buffer</b>
 * <p>
 * An improved {@link MultiFileReceiver} enables removing cached file content, which Vaadin's
 * {@link com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer} does not provide.
 *
 * @since 1.0.0
 */
public class EditableMultiFileMemoryBuffer implements MultiFileReceiver {

  @Serial
  private static final long serialVersionUID = 2041166242210420469L;
  private final Map<String, FileData> files = new HashMap<>();

  @Override
  public OutputStream receiveUpload(String fileName, String mimeType) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    this.files.put(fileName, new FileData(fileName, mimeType, byteArrayOutputStream));
    return byteArrayOutputStream;
  }

  public Optional<OutputStream> outputStream(String fileName) {
    return Optional.ofNullable(files.getOrDefault(fileName, null)).map(FileData::getOutputBuffer);
  }

  public Optional<InputStream> inputStream(String fileName) {
    return Optional.ofNullable(files.getOrDefault(fileName, null))
        .map(fileData -> (ByteArrayOutputStream) fileData.getOutputBuffer())
        .map(outputStream -> new ByteArrayInputStream(outputStream.toByteArray()));
  }

  public void remove(String fileName) {
    files.remove(fileName);
  }

  /**
   * Clears the complete buffer and removes any content cashed in the buffer.
   * <p>
   * All cached files and their content are removed from the buffer.
   *
   * @since 1.0.0
   */
  public void clear() {
    files.clear();
  }

  public Stream<String> fileNames() {
    return this.files.keySet().stream();
  }
}
