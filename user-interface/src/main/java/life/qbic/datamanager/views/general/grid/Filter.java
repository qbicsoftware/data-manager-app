package life.qbic.datamanager.views.general.grid;


/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface Filter<T> {

  void setSearchTerm(String searchTerm);

  String searchTerm();

  boolean test(T data);

}
