package life.qbic.datamanager.views.general;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class Container<T> {

  private T item;

  public void set(T item) {
    this.item = item;
  }

  public T get() {
    return this.item;
  }

}
