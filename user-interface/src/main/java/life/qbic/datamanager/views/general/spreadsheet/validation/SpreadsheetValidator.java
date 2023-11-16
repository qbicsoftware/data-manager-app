package life.qbic.datamanager.views.general.spreadsheet.validation;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface SpreadsheetValidator<T> {

  ValidationResult validate(T value);

}
