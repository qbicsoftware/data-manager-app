package life.qbic.projectmanagement.infrastructure.template.provider.openxml;

/**
 * Provides {@link Helper} with example values and description for columns in spreadsheet templates
 * provided in Data Manager.
 */
public interface ExampleProvider {

  record Helper(String exampleValue, String description) {
  }

  Helper getHelper(Column column);
}
