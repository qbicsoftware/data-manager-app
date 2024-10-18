package life.qbic.datamanager.export.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import life.qbic.datamanager.export.FileFormatSupplier;
import life.qbic.datamanager.export.model.ResearchProject;

/**
 * <b>YAML formatter implementation</b>
 * <p>
 * Creates YAML representations of various content types in a Data Manager's project.
 *
 * @since 1.0.0
 */
public class YamlSupplier implements FileFormatSupplier {

  public static YamlSupplier create() {
    return new YamlSupplier();
  }

  @Override
  public File from(String fileName, ResearchProject researchProject) throws FormatException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    File file = new File(fileName);
    try {
      mapper.writeValue(file, researchProject);
      return file;
    } catch (IOException e) {
      throw new FormatException("Could not write to file " + fileName, e);
    }
  }
}
