package life.qbic.datamanager.views.general;

import com.vaadin.flow.data.binder.ValidationException;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface BoundField<T, V> {

  T getField();

  V getValue() throws ValidationException;

  void setValue(V value);

  boolean isValid();

  boolean hasChanged();
}
