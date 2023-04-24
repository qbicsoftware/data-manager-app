package life.qbic.application.commons.tmp;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public abstract class Either<V, E> {

  static <V, E> Either<V, E> fromValue(V value) {
    return new Value<V, E>(value);
  }

  static <V, E> Either<V, E> fromError(E error) {
    return new Error<V, E>(error);
  }

  abstract Either<V, E> onValue(Consumer<V> consumer);

  abstract Either<V, E> onError(Consumer<E> consumer);

  abstract boolean isValue();

  abstract boolean isError();

  abstract <U> Either<U, E> transformValue(Function<V, U> transform);

  abstract <T> Either<V, T> transformError(Function<E, T> transform);

  abstract <U extends V> Either<U, E> bindValue(Function<V, Either<U, E>> mapper);

  abstract <T extends E> Either<V, T> bindError(Function<E, Either<V, T>> mapper);

  <U> U fold(Function<V, U> valueMapper, Function<E, U> errorMapper) {
    return null;
  }

  <U> Either<U, E> recover(Function<E, U> recovery) {
    return null;
  }

  public V valueOrElse(V other) {
    return null;
  }

  public V valueOrElseGet(Supplier<V> supplier) {
    return null;
  }

  public V valueOrElseThrow(Supplier<V> supplier) {
    return null;
  }

  private static class Value<V, E> extends Either<V, E> {

    private V value;

    private Value(V value) {
      this.value = value;
    }

    private V get() {
      return value;
    }

    @Override
    Either<V, E> onValue(Consumer<V> consumer) {
      consumer.accept(value);
      return this;
    }

    @Override
    Either<V, E> onError(Consumer<E> consumer) {
      return this;
    }

    @Override
    boolean isValue() {
      return true;
    }

    @Override
    boolean isError() {
      return false;
    }

    @Override
    <U> Either<U, E> transformValue(Function<V, U> transform) {
      return Either.<U, E>fromValue(transform.apply(value));
    }

    @Override
    <T> Either<V, T> transformError(Function<E, T> transform) {
      return Either.<V, T>fromValue(value);
    }

    @Override
    <U extends V> Either<U, E> bindValue(Function<V, Either<U, E>> mapper) {
      return null;
    }

    @Override
    <T extends E> Either<V, T> bindError(Function<E, Either<V, T>> mapper) {
      return null;
    }
  }

  private static class Error<V, E> extends Either<V, E> {

    private E error;

    private Error(E error) {
      this.error = error;
    }

    private E get() {
      return error;
    }

    @Override
    Either<V, E> onValue(Consumer<V> consumer) {
      return this;
    }

    @Override
    Either<V, E> onError(Consumer<E> consumer) {
      consumer.accept(error);
      return this;
    }

    @Override
    boolean isValue() {
      return false;
    }

    @Override
    boolean isError() {
      return true;
    }

    @Override
    <U> Either<U, E> transformValue(Function<V, U> transform) {
      return Either.<U, E>fromError(error);
    }

    @Override
    <T> Either<V, T> transformError(Function<E, T> transform) {
      T transformed = transform.apply(error);
      return Either.<V, T>fromError(transformed);
    }

    @Override
    <U extends V> Either<U, E> bindValue(Function<V, Either<U, E>> mapper) {
      return null;
    }

    @Override
    <T extends E> Either<V, T> bindError(Function<E, Either<V, T>> mapper) {
      return null;
    }
  }
}
