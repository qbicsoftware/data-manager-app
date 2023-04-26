package life.qbic.application.commons.tmp;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The result of an operation containing a value or an error.
 * <p/>
 * <p>
 * Whenever an operation can stray from the golden path and can return alternative values, consider
 * using this Result object. The expected value for an operation is contained in the result object.
 * The expected return value is the value an operation returns. Whenever there are alternative
 * return values, they are represented by the error contained in a result.
 * </p>
 * <h3>Creating a Result</h3>
 * <p>
 * The factory method {@linkplain Result#fromValue(Object)} constructs a result containing the
 * expected return value.
 * <pre>{@code
 *   Result<Integer, String> result = Result.<Integer, String>fromValue(5);
 * }</pre>
 * It is advisable for this method to provide the concrete type information. This is necessary for
 * type inference if the error is of a specific type. Otherwise, only {@linkplain Object} is
 * inferred by the compiler for the error type.
 * <p>
 * The factory method {@linkplain Result#fromError(Object)} constructs a result containing some
 * error information.
 * <pre>{@code
 * Result<Integer, String> result = Result.<String, Integer>fromError("Some non-exceptional condition was true.");
 * }</pre>
 * It is advisable for this method to provide the concrete type information. This is necessary for
 * type inference if the value is of a specific type. Otherwise, only {@linkplain Object} is
 * inferred by the compiler for the value type.
 * <p>
 * <h3>Working with the contents</h3>
 * The result object provides some basic transformations to process the result further. These can be
 * applied on the value of a result or on possible errors respectively.
 * <p>
 * <ul>
 *   <li>The {@link Result#map(Function)} transformation applies the provided function to the value of the result if the result is a value.</li>
 *   <li>The {@link Result#mapError(Function)} transformation applies the provided function to the error of a result if the result is an error.</li>
 *   <li>The {@link Result#flatMap(Function)} transformation applies the function to the value of this result and unwraps the returned result into the itself.</li>
 *   <li>The {@link Result#flatMapError(Function)} transformation applies the function to the error of this result and unwraps the returned result into the itself.</li>
 *   <li>The {@link Result#recover(Function)} transformation applies the function to the error and sets the result as the value.</li>
 * </ul>
 * <p>
 * Depending on the result type different actions can be taken. For that two consumer methods are provided.
 * <ul>
 *   <li>The {@link Result#onValue(Consumer)} method passes the value to the consumer if any value exists. The result itself is not altered.</li>
 *   <li>The {@link Result#onError(Consumer)} method passes the error to the consumer if the result is an error. The result itself is not altered.</li>
 * </ul>
 * <p>
 * <h3>Extracting the content of a result</h3>
 * <ul>
 *   <li>The {@link Result#valueOrElse(Object)} method returns the value if it exists, or the provided object if the result is an error.</li>
 *   <li>The {@link Result#valueOrElseGet(Supplier)} method returns the value if it exists. Otherwise the supplier supplies the value.</li>
 *   <li>The {@link Result#valueOrElseThrow(Supplier)} method returns the value if it exists. Otherwise the supplied exception is thrown.</li>
 *   <li> The {@link Result#fold(Function, Function)} method applies the appropriate function to the content and returns the resulting value.
 * </ul>
 *
 * @param <V> the value type
 * @param <E> the error type
 */
public abstract class Result<V, E> {

  /**
   * Constructs a new result from a value.
   * <p>
   * If a specific error type is expected, this method needs to be called with type information.
   * <pre>{@code
  Result.fromValue("Hello World!");}</pre> Will create a result of type {@code Result<String, Object>}.
   * <p>
   * To create a result of type {@code Result<String, MyErrorCodeEnum>} instead use
   * <pre> {@code
  Result.<String, MyErrorCodeEnum>fromValue("Hello World!");}
   * </pre>
   * @param value the value wrapped in the result.
   * @return a result wrapping the value
   * @param <V> the value type
   * @param <E> the error type
   */
  public static <V, E> Result<V, E> fromValue(V value) {
    return new Value<>(value);
  }

  /**
   * Constructs a new result from an error.
   * <p>
   * If a specific value type is expected, this method needs to be called with type information.
   * <pre>{@code
  Result.fromError("Hello World!");}</pre> Will create a result of type {@code Result<Object, String>}.
   * <p>
   * To create a result of type {@code Result<Integer, String>} instead use
   * <pre> {@code
  Result.<Integer, String>fromError("Hello World!");}
   * </pre>
   * @param error the error wrapped in the result
   * @return a result wrapping an error
   * @param <V> the value type
   * @param <E> the error type
   */
  public static <V, E> Result<V, E> fromError(E error) {
    return new Error<>(error);
  }

  /**
   * Executes some action on the value of the result if a value is present.
   * @param consumer the action to run on the value.
   * @return an unaltered result.
   */
  public abstract Result<V, E> onValue(Consumer<V> consumer);

  /**
   * Executes some action on the error of the result if an error is present.
   * @param consumer the action to run on the error
   * @return an unaltered result
   */
  public abstract Result<V, E> onError(Consumer<E> consumer);

  /**
   *
   * @return true if the result is a value, false otherwise
   */
  public abstract boolean isValue();

  /**
   *
   * @return true if the result is an error, false otherwise
   */
  public abstract boolean isError();

  /**
   * Applies a function to the value if a value is present and returns a result wrapping the transformed value.
   <pre> {@code
  Result<String, MyErrorCode> result = Result.<String, MyErrorCode>fromValue("Hello World!"); // value = "Hello World!"
  Result<Integer, MyErrorCode> transformed = result.map(string -> string.length()) // value = 12}
   </pre>
   * @param transform the transformation to apply on the value
   * @return a result object with the transformed value
   * @param <U> the transformed value type
   */
  public abstract <U> Result<U, E> map(Function<V, U> transform);

  /**
   * Applies a function to the error if an error is present and returns a result wrapping the transformed error.
   <blockquote><pre>
   Result&lt;String, MyErrorCode&gt; result = Result.&lt;String, MyErrorCode&gt;fromError(MyErrorCode.NO_CATS_HERE); // value = NO_CATS_HERE
   Result&lt;String, Boolean&gt; happyDeveloper = result.mapError(errorCode -&gt; switch (errorCode) {
   case NO_CATS_HERE -&gt; false;
   case SOME_OTHER_ERROR -&gt; true;
   }); // value = false
   </pre></blockquote>
   * @param transform the transformation to apply to the error
   * @return a result object with the transformed error
   * @param <T> the error type
   */
  public abstract <T> Result<V, T> mapError(Function<E, T> transform);

  /**
   * If the result is a value; Applies the function to the value and flat maps it into a new Result object.
   * @param mapper the mapping function
   * @return a result object containing the transformed value
   * @param <U> the transformed value type
   */
  public abstract <U> Result<U, E> flatMap(Function<V, Result<U, E>> mapper);

  /**
   * If the result is an error; Applies the function to the error and flat maps it into a new Result object.
   * @param mapper the mapping function
   * @return a result object containing the transformed error
   * @param <T> the transformed error type
   */
  public abstract <T> Result<V, T> flatMapError(Function<E, Result<V, T>> mapper);

  /**
   * Folds the result back to the value. Example:
   * <pre>
   * {@code
   * int offset = 3;
   * Result<String, Integer> someResult = ...;
   * int offsetLength = someResult.fold(value -> value.length() + offset, error -> error + offset);
   * }
   * </pre>
   * @param valueMapper the mapping function for the value
   * @param errorMapper the mapping function for the error
   * @return the mapping result
   * @param <U> the type of the returned value
   */
  public abstract <U> U fold(Function<V, U> valueMapper, Function<E, U> errorMapper);

  /**
   * Recover the error to a value
   * <pre>
   * {@code
   * Result<String, ErrorCode> result = Result.<String, ErrorCode>fromError(VALUE_MISSING);
   * String message = result.recover(errorCode -> errorCode == VALUE_MISSING ? "some value is missing" : "some other error occurred");
   * assert(message == "some value missing");
   * }</pre
   * @param recovery the function that maps the error to the value
   * @return a result containing the recovered value
   */
  public abstract Result<V, E> recover(Function<E, V> recovery);

  /**
   * Retuns the value or `other` value if no value is present
   * @param other the alternative value to return
   * @return the value or other
   */
  public abstract V valueOrElse(V other);

  /**
   * Returns the value or the supplied value if the result is not a value
   * @param supplier the supplier of the alternative value
   * @return the result's value or the supplied value
   */
  public abstract V valueOrElseGet(Supplier<V> supplier);

  /**
   * Returns the value if the result is a value; trhows the supplied exception otherwise.
   * @param supplier the exception supplier
   * @return the stored value
   */
  public abstract V valueOrElseThrow(Supplier<? extends RuntimeException> supplier);

  private static class Value<V, E> extends Result<V, E> {

    private final V value;

    private Value(V value) {
      this.value = value;
    }

    private V get() {
      return value;
    }

    @Override
    public Result<V, E> onValue(Consumer<V> consumer) {
      consumer.accept(value);
      return this;
    }

    @Override
    public Result<V, E> onError(Consumer<E> consumer) {
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
    public <U> Result<U, E> map(Function<V, U> transform) {
      return Result.fromValue(transform.apply(value));
    }

    @Override
    public <T> Result<V, T> mapError(Function<E, T> transform) {
      return Result.fromValue(value);
    }

    @Override
    public <U> Result<U, E> flatMap(Function<V, Result<U, E>> mapper) {
      return mapper.apply(value);
    }

    @Override
    public <T> Result<V, T> flatMapError(Function<E, Result<V, T>> mapper) {
      return Result.fromValue(value);
    }

    @Override
    public <U> U fold(Function<V, U> valueMapper, Function<E, U> errorMapper) {
      return valueMapper.apply(value);
    }

    @Override
    public Result<V, E> recover(Function<E, V> recovery) {
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

  private static class Error<V, E> extends Result<V, E> {

    private final E error;

    private Error(E error) {
      this.error = error;
    }

    private E get() {
      return error;
    }

    @Override
    public Result<V, E> onValue(Consumer<V> consumer) {
      return this;
    }

    @Override
    public Result<V, E> onError(Consumer<E> consumer) {
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
    public <U> Result<U, E> map(Function<V, U> transform) {
      return Result.fromError(error);
    }

    @Override
    public <T> Result<V, T> mapError(Function<E, T> transform) {
      T transformed = transform.apply(error);
      return Result.fromError(transformed);
    }

    @Override
    public <U> Result<U, E> flatMap(Function<V, Result<U, E>> mapper) {
      return Result.fromError(error);
    }

    @Override
    public <T> Result<V, T> flatMapError(Function<E, Result<V, T>> mapper) {
      return mapper.apply(error);
    }

    @Override
    public <U> U fold(Function<V, U> valueMapper, Function<E, U> errorMapper) {
      return errorMapper.apply(error);
    }

    @Override
    public Result<V, E> recover(Function<E, V> recovery) {
      V recoveredValue = recovery.apply(error);
      return Result.fromValue(recoveredValue);
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
