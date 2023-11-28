package life.qbic.datamanager.views.projects.purchase;

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

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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

}
