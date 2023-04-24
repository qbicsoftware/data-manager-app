package life.qbic.application.commons.tmp;

import java.util.Objects;
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

  abstract <U> Either<U, E> bindValue(Function<V, Either<U, E>> mapper);

  abstract <T> Either<V, T> bindError(Function<E, Either<V, T>> mapper);

  abstract <U> U fold(Function<V, U> valueMapper, Function<E, U> errorMapper);

  abstract Either<V, E> recover(Function<E, V> recovery);

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
    <U> Either<U, E> bindValue(Function<V, Either<U, E>> mapper) {
      return mapper.apply(value);
    }

    @Override
    <T> Either<V, T> bindError(Function<E, Either<V, T>> mapper) {
      return Either.<V, T>fromValue(value);
    }

    @Override
    <U> U fold(Function<V, U> valueMapper, Function<E, U> errorMapper) {
      return valueMapper.apply(value);
    }

    @Override
    Either<V, E> recover(Function<E, V> recovery) {
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Value<?, ?> value1 = (Value<?, ?>) o;

      return Objects.equals(value, value1.value);
    }

    @Override
    public int hashCode() {
      return value != null ? value.hashCode() : 0;
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
    <U> Either<U, E> bindValue(Function<V, Either<U, E>> mapper) {
      return Either.<U, E>fromError(error);
    }

    @Override
    <T> Either<V, T> bindError(Function<E, Either<V, T>> mapper) {
      return mapper.apply(error);
    }

    @Override
    <U> U fold(Function<V, U> valueMapper, Function<E, U> errorMapper) {
      return errorMapper.apply(error);
    }

    @Override
    Either<V, E> recover(Function<E, V> recovery) {
      V recoveredValue = recovery.apply(error);
      return Either.<V, E>fromValue(recoveredValue);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Error<?, ?> error1 = (Error<?, ?>) o;

      return Objects.equals(error, error1.error);
    }

    @Override
    public int hashCode() {
      return error != null ? error.hashCode() : 0;
    }
  }
}
