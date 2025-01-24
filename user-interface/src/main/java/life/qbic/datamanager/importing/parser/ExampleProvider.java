package life.qbic.datamanager.importing.parser;

import life.qbic.datamanager.files.structure.Column;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface ExampleProvider {

  record Helper(String exampleValue, String description) {

  }

  Helper getHelper(Column column);

}
