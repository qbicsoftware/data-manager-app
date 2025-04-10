package life.qbic.datamanager.files.structure;

/**
 * Provides {@link Helper} with example values and description for columns in spreadsheet templates
 * provided in Data Manager.
 */
public interface ExampleProvider {

  Helper getHelper(Column column);

  record Helper(String exampleValue, String description) {

  }

}
