package life.qbic.datamanager.views.demo;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A list that pretends it is a backend repository with paginated API.
 * <p>
 * This can be used for testing or demo purposes.
 *
 * @param <T> item type
 * @param <F> filter type
 */
class PseudoDataBackend<T, F> {

  private final Function<F, Predicate<T>> filterConverter;
  private final List<T> items;

  public PseudoDataBackend(List<T> items, Function<F, Predicate<T>> filterConverter) {
    this.items = items;
    this.filterConverter = filterConverter;
  }


  public Stream<T> fetch(int offset, int limit, F filter) {
    return items.stream()
        .skip(offset)
        .filter(item -> filterConverter.apply(filter).test(item))
        .limit(limit);
  }

  public int count(F filter) {
    return Math.toIntExact(items.stream()
        .filter(item -> filterConverter.apply(filter).test(item))
        .count());
  }
}
