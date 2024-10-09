package life.qbic.datamanager.views.general.upload;

import static java.util.Objects.nonNull;

import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.component.upload.receivers.FileData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class FileMemoryBuffer implements Receiver {

  private FileData fileData;

  @Override
  public OutputStream receiveUpload(String fileName, String mimeType) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    this.fileData = new FileData(fileName, mimeType, byteArrayOutputStream);
    return byteArrayOutputStream;
  }

  public boolean hasUploadedData() {
    return nonNull(fileData);
  }

  public Optional<InputStream> getInputStream() {
    return Optional.ofNullable(fileData)
        .map(FileData::getOutputBuffer)
        .filter(ByteArrayOutputStream.class::isInstance)
        .map(ByteArrayOutputStream.class::cast)
        .map(ByteArrayOutputStream::toByteArray)
        .map(ByteArrayInputStream::new);
  }

  public Optional<String> getFileName() {
    return Optional.ofNullable(fileData.getFileName());
  }

  public Optional<String> getMimeType() {
    return Optional.ofNullable(fileData.getMimeType());
  }

  public void clear() {
    fileData = null;
  }

}
