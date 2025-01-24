package life.qbic.datamanager.files.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;

@FunctionalInterface
public interface YamlFileSupplier<T> extends FileSupplier {

  T payload();

  @Override
  default File getFile(String fileName) {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    File file = new File(fileName);
    try {
      mapper.writeValue(file, payload());
      return file;
    } catch (IOException e) {
      throw new FormatException("Could not write to file " + fileName, e);
    }
  }
}
