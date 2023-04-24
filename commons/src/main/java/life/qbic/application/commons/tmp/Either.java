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

  public static <V, E> Either<V, E> fromValue(V value) {
    return new Value<>(value);
  }

  public static <V, E> Either<V, E> fromError(E error) {
    return new Error<>(error);
  }

  public abstract Either<V, E> onValue(Consumer<V> consumer);

  public abstract Either<V, E> onError(Consumer<E> consumer);

  public abstract boolean isValue();

  public abstract boolean isError();

  public abstract <U> Either<U, E> map(Function<V, U> transform);

  public abstract <T> Either<V, T> mapError(Function<E, T> transform);

  public abstract <U> Either<U, E> flatMap(Function<V, Either<U, E>> mapper);

  public abstract <T> Either<V, T> flatMapError(Function<E, Either<V, T>> mapper);

  public abstract <U> U fold(Function<V, U> valueMapper, Function<E, U> errorMapper);

  public abstract Either<V, E> recover(Function<E, V> recovery);

  public abstract V valueOrElse(V other);

  public abstract V valueOrElseGet(Supplier<V> supplier);

  public abstract V valueOrElseThrow(Supplier<? extends RuntimeException> supplier);

  private static class Value<V, E> extends Either<V, E> {

    private final V value;

    private Value(V value) {
      this.value = value;
    }

    private V get() {
      return value;
    }

    @Override
    public Either<V, E> onValue(Consumer<V> consumer) {
      consumer.accept(value);
      return this;
    }

    @Override
    public Either<V, E> onError(Consumer<E> consumer) {
      return this;
    }

    @Override
    public boolean isValue() {
      return true;
    }

    @Override
    public boolean isError() {
      return false;
    }

    @Override
    public <U> Either<U, E> map(Function<V, U> transform) {
      return Either.fromValue(transform.apply(value));
    }

    @Override
    public <T> Either<V, T> mapError(Function<E, T> transform) {
      return Either.fromValue(value);
    }

    @Override
    public <U> Either<U, E> flatMap(Function<V, Either<U, E>> mapper) {
      return mapper.apply(value);
    }

    @Override
    public <T> Either<V, T> flatMapError(Function<E, Either<V, T>> mapper) {
      return Either.fromValue(value);
    }

    @Override
    public <U> U fold(Function<V, U> valueMapper, Function<E, U> errorMapper) {
      return valueMapper.apply(value);
    }

    @Override
    public Either<V, E> recover(Function<E, V> recovery) {
      return this;
    }

    @Override
    public V valueOrElse(V other) {
      return value;
    }

    @Override
    public V valueOrElseGet(Supplier<V> supplier) {
      return value;
    }

    @Override
    public V valueOrElseThrow(Supplier<? extends RuntimeException> supplier) {
      return value;
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

    private final E error;

    private Error(E error) {
      this.error = error;
    }

    private E get() {
      return error;
    }

    @Override
    public Either<V, E> onValue(Consumer<V> consumer) {
      return this;
    }

    @Override
    public Either<V, E> onError(Consumer<E> consumer) {
      consumer.accept(error);
      return this;
    }

    @Override
    public boolean isValue() {
      return false;
    }

    @Override
    public boolean isError() {
      return true;
    }

    @Override
    public <U> Either<U, E> map(Function<V, U> transform) {
      return Either.fromError(error);
    }

    @Override
    public <T> Either<V, T> mapError(Function<E, T> transform) {
      T transformed = transform.apply(error);
      return Either.fromError(transformed);
    }

    @Override
    public <U> Either<U, E> flatMap(Function<V, Either<U, E>> mapper) {
      return Either.fromError(error);
    }

    @Override
    public <T> Either<V, T> flatMapError(Function<E, Either<V, T>> mapper) {
      return mapper.apply(error);
    }

    @Override
    public <U> U fold(Function<V, U> valueMapper, Function<E, U> errorMapper) {
      return errorMapper.apply(error);
    }

    @Override
    public Either<V, E> recover(Function<E, V> recovery) {
      V recoveredValue = recovery.apply(error);
      return Either.fromValue(recoveredValue);
    }

    @Override
    public V valueOrElse(V other) {
      return other;
    }

    @Override
    public V valueOrElseGet(Supplier<V> supplier) {
      return supplier.get();
    }

    @Override
    public V valueOrElseThrow(Supplier<? extends RuntimeException> supplier) {
      throw supplier.get();
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
