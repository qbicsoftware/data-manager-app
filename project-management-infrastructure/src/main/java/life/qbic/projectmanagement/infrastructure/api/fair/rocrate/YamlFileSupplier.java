package life.qbic.projectmanagement.infrastructure.api.fair.rocrate;

import tools.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;

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
    var mapper = new YAMLMapper();
    File file = new File(fileName);
    mapper.writeValue(file, payload);
    return file;
  }
}
