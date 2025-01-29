package life.qbic.datamanager.files.structure;

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
