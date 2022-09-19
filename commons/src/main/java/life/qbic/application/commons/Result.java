package life.qbic.application.commons;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <b>Class Result</b>
 *
 * <p>This class introduces the Rust idiom to use Result objects to enforce the client code
 * to apply some best practises to safe value access and proper exception handling.</p>
 *
 * <p>Results can be used to wrap an actual value of type <code>V</code>, which indicates a
 * success, or an exception of type
 * <code>E</code>. An object of type Result can only contain either a value or an exception, not
 * both in the same instance.</p>
 * <p>
 * To properly deal with Result objects, a good idiom using Java's enhanced switch statements looks
 * like this:
 *
 * <pre>
 *
 * final Result&lt;String, Exception&gt; result =
 *          new Result("Contains actual information")
 * // Using the  {@link Function} interface
 * Function function = switch (result) {*    case result.isOk(): yield Function&lt;V, ?&gt;{...}*    case result.isError(): yield Function&lt;E, ?&gt;{...}*}* function.apply(result)
 *
 * // or using the {@link Consumer} interface
 * Consumer consumer = switch (result) {*   case result.isOk() : yield Consumer&lt;V&gt;{...}*   case result.isError() : yield Consumer&lt;E&gt;{...}*}* consumer.accept(result)
 *
 * // or using lambda expressions
 * switch (result) {*   case result.isOk() : () -> {}*   case result.isError() : () -> {}*}* </pre>
 *
 * @param <V> the value of this result in case it is OK
 * @param <E> the type of error this result can hold
 * @since 1.0.0
 */
class Result<V, E extends Exception> {

  private final V value;
  private final E exception;

  private final Type type;

  private enum Type {
    FAILURE, SUCCESS
  }

  /**
   * Static constructor method for creating a result object instance of type <code>V,E</code>
   * wrapping an actual value <code>V</code>.
   *
   * @param value the notorious value to get wrapped in a result object
   * @return a new result object instance
   */
  public static <V, E extends Exception> Result<V, E> success(V value) {
    Objects.requireNonNull(value);
    return new Result<>(value);
  }

  /**
   * Static constructor method for creating a result object instance of type <code>V,E</code>
   * wrapping an exception <code>E</code>.
   *
   * @param exception the exception to get wrapped in a result object
   * @return a new result object instance
   */
  public static <V, E extends Exception> Result<V, E> exception(E exception) {
    Objects.requireNonNull(exception);
    return new Result<>(exception);
  }

  private Result(V value) {
    this.value = value;
    this.exception = null;
    this.type = Type.SUCCESS;
  }

  private Result(E exception) {
    this.value = null;
    this.exception = exception;
    this.type = Type.FAILURE;
  }

  /**
   * Access the wrapped value if present
   *
   * @return the wrapped value
   * @throws NoSuchElementException if no value exists in the result object
   */
  V value() throws NoSuchElementException {
    if (Objects.isNull(value)) {
      throw new NoSuchElementException("Result with exception has no value.");
    }
    return value;
  }

  /**
   * Access the wrapped exception if present
   *
   * @return the wrapped exception
   * @throws NoSuchElementException if no error exists in the result object
   */
  E exception() throws NoSuchElementException {
    if (Objects.isNull(exception)) {
      throw new NoSuchElementException("Result with value has no exception.");
    }
    return exception;
  }

  /**
   * Returns <code>true</code>, if the result object contains an error. Is always the negation of
   * {@link Result#isSuccess()} ()}.
   * <p>So <code>{@link Result#isFailure()} ()} == !{@link Result#isSuccess()}</code></p>
   *
   * @return true, if the result object has an error, else false
   */
  public Boolean isFailure() {
    return type.equals(Type.FAILURE);
  }

  /**
   * Returns <code>true</code>, if the result object contains an error. Is always the negation of
   * {@link Result#isFailure()} ()}.
   * <p>So <code>{@link Result#isSuccess()} ()} == !{@link Result#isFailure()} ()}</code></p>
   *
   * @return true, if the result object has a value, else false
   */
  public Boolean isSuccess() {
    return type.equals(Type.SUCCESS);
  }

  /**
   * <p>Maps the current result object to a consumer function, that expects the same input
   * type <code>V</code> as the result's value type and produces a result object of type
   * <code>U,E</code>.</p>
   *
   * <p>If the current result contains an error (and therefore has no value), the created result
   * object will contain the error of type <code>E</code> of the input result object.
   * </p>
   *
   * @param function a function transforming data of type <code>V</code> to <code>U</code>
   * @return a new result object instance of type <code>U,E</code>
   */
  public <U> Result<U, ? extends E> map(Function<V, U> function) {
    Objects.requireNonNull(function);
    Result<U, ? extends E> result = null;
    switch (this.type) {
      case FAILURE -> result = Result.exception(this.exception);
      case SUCCESS -> result = apply(function, this.value());
    }
    return result;
  }

  /**
   * Takes a {@link Consumer &lt;? super V>} and calls its {@link Consumer#accept(Object)} method,
   * when the result object contains a value of type V.
   * <p>
   * If the result object does not contain a value but an exception, the method does nothing.
   *
   * @param consumer A target consumer object reference that can consume the value of type
   *                 <code>V</code>
   * @since 1.0.0
   */
  public void ifSuccess(Consumer<? super V> consumer) {
    Objects.requireNonNull(consumer);
    if (type.equals(Type.SUCCESS) && Objects.nonNull(value)) {
      consumer.accept(value);
    }
  }

  /**
   * Takes a {@link Consumer &lt;? super E>} and calls its {@link Consumer#accept(Object)} method, *
   * when the result object contains an exception.
   * <p>
   * If the result object contains a value, the method does nothing.
   *
   * @param consumer A target consumer object reference that can consume an exception of type *
   *                 <code>E</code>
   * @since 1.0.0
   */
  public void ifFailure(Consumer<? super E> consumer) {
    Objects.requireNonNull(consumer);
    if (type.equals(Type.FAILURE)) {
      consumer.accept(exception);
    }
  }

  /**
   * Takes a {@link Consumer &lt;? super V>} and calls its {@link Consumer#accept(Object)} method,
   * when the result object represents a success and contains a value and takes a
   * {@link Consumer &lt;? super E>} and calls its {@link Consumer#accept(Object)}, if the result
   * contains a failure.
   *
   * @param consumerOfValue A target consumer object reference that can consume a value of type *
   *                        <code>V</code>
   * @param consumerOfError A target consumer object reference that can consume an exception of
   *                        type
   *                        <code>E</code>
   * @since 1.0.0
   */
  public void ifSuccessOrElse(Consumer<? super V> consumerOfValue,
      Consumer<? super E> consumerOfError) {
    if (type.equals(Type.FAILURE)) {
      consumerOfError.accept(exception);
    } else {
      consumerOfValue.accept(value);
    }
  }

  /**
   * Returns the value of type <code>V</code> if present or takes an {@link Supplier} of type
   * <code>X</code> and throws an object of type {@link Throwable} if the result contains an
   * exception.
   *
   * @param <X>               a subtype of {@link Throwable}
   * @param throwableSupplier a throwable supplier to get, when the result object contains an
   *                          exception
   * @return the value <code>V</code>
   * @throws X which is a subtype of {@link Throwable}
   * @since 1.0.0
   */
  public <X extends Throwable> V orElseThrow(Supplier<? extends X> throwableSupplier) throws X {
    if (type.equals(Type.SUCCESS)) {
      return value;
    } else {
      throw throwableSupplier.get();
    }
  }

  private <U> Result<U, E> apply(Function<V, U> function, V value) {
    Result<U, E> result;
    try {
      result = Result.success(function.apply(value));
    } catch (Exception e) {
      result = (Result<U, E>) Result.exception(e);
    }
    return result;
  }
}
