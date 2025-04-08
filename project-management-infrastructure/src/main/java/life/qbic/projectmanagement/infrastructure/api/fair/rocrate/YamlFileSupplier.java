package life.qbic.projectmanagement.infrastructure.api.fair.rocrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;

public class YamlFileSupplier<T> implements FileSupplier {

  private final T payload;

  YamlFileSupplier(T payload) {
    this.payload = payload;
  }

  public static <S> YamlFileSupplier<S> supplying(S payload) {
    return new YamlFileSupplier<>(payload);
  }

  @Override
  public File getFile(String fileName) {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    File file = new File(fileName);
    try {
      mapper.writeValue(file, payload);
      return file;
    } catch (IOException e) {
      throw new FormatException("Could not write to file " + fileName, e);
    }
  }
}
