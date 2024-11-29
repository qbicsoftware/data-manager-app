package life.qbic.datamanager.views.general.dialog;

import org.springframework.lang.NonNull;

/**
 * <b>User Input</b>
 *
 * <p>Content that can be validated and asked for changes. </p> Validation by contract needs to
 * return a valid {@link InputValidation} in order for the client to be able to react on passing or
 * failing validation of user input.
 * <p>
 * Changes of the underlying data that are modifiable are indicated by a simple boolean flag.
 *
 * @since 1.7.0
 */
public interface UserInput {

  /**
   * Triggers the validation of the user input and communicate the result of the validation.
   *
   * @return the {@link InputValidation} result.
   * @since 1.7.0
   */
  @NonNull
  InputValidation validate();

  /**
   * Indicates, if the underlying user input was changed from the original state.
   *
   * @return true, if there have been any changes to the original input state, else false
   * @since 1.7.0
   */
  boolean hasChanges();

}
