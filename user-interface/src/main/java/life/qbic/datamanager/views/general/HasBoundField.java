package life.qbic.datamanager.views.general;

import com.vaadin.flow.data.binder.ValidationException;

/**
 * <b>Bound Field</b>
 *
 * <p>A bound field offers some common access and behaviour to the implemented bound field.</p>
 *
 * @since 1.6.0
 */
public interface HasBoundField<T, V> {

  /**
   * Returns the field with bindings
   *
   * @since 1.6.0
   */
  T getField();

  /**
   * Returns the bound value
   *
   * @throws ValidationException if any validation of the field fails
   * @since 1.6.0
   */
  V getValue() throws ValidationException;

  /**
   * Set the bound value for the field. This will also update the field content.
   *
   * @param value sets an original value
   * @since 1.6.0
   */
  void setValue(V value);

  /**
   * <code>true</code>, if the bound value is valid, else returns <code>false</code>
   *
   * @since 1.6.0
   */
  boolean isValid();

  /**
   * Indicates, if the original value has changed after being set via {@link #setValue(Object)}
   *
   * @return <code>true</code>, if the original value has changed, else <code>false</code>
   * @since 1.6.0
   */
  boolean hasChanged();
}
