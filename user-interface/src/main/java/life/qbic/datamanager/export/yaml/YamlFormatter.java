package life.qbic.datamanager.export.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import life.qbic.datamanager.export.Formatter;
import life.qbic.datamanager.export.model.ResearchProject;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class YamlFormatter implements Formatter {

  public static YamlFormatter create() {
    return new YamlFormatter();
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
