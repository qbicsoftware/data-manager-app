package life.qbic.application.commons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * <b>Application Response</b>
 *
 * <p>Application responses can be used to report successful or failing application operations.</p>
 *
 * @since 1.0.0
 */
public class ApplicationResponse {

  protected enum Type {SUCCESSFUL, FAILED}

  protected Type type;

  protected final List<RuntimeException> exceptions;

  public static ApplicationResponse successResponse() {
    var successResponse = new ApplicationResponse();
    successResponse.setType(Type.SUCCESSFUL);
    return successResponse;
  }

  public static ApplicationResponse failureResponse(RuntimeException... exceptions) {
    if (exceptions == null) {
      throw new IllegalArgumentException("Null references are not allowed.");
    }
    var failureResponse = new ApplicationResponse();
    failureResponse.setType(Type.FAILED);
    failureResponse.setExceptions(exceptions);
    return failureResponse;
  }

  protected ApplicationResponse() {
    exceptions = new ArrayList<>();
  }

  protected void setType(Type type) {
    this.type = type;
  }

  /**
   * @return true if the response represents a success, false otherwise
   */
  public boolean isSuccess() {
    return type.equals(Type.SUCCESSFUL);
  }

  protected void setExceptions(RuntimeException... exceptions) {
    this.exceptions.clear();
    this.exceptions.addAll(Arrays.stream(exceptions).toList());
  }

  public boolean hasFailures() {
    return type == Type.FAILED;
  }

  public List<RuntimeException> failures() {
    return exceptions;
  }

  /**
   * Depending on the response, two type of downstream actions can be passed to the
   * {@link ApplicationResponse}.
   * <p>
   * If the instance contains failures, the downstream failure {@link Consumer} action will be
   * triggered and a reference to itself passed as argument. Otherwise, the downstream failure
   * consumer is called.
   *
   * @param downstreamSuccess consumer for success responses
   * @param downstreamFailure consumer for failure responses
   * @since 1.0.0
   */
  public void ifSuccessOrElse(Consumer<ApplicationResponse> downstreamSuccess,
      Consumer<ApplicationResponse> downstreamFailure) {
    if (hasFailures()) {
      downstreamFailure.accept(this);
    } else {
      downstreamSuccess.accept(this);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ApplicationResponse that = (ApplicationResponse) o;

    if (type != that.type) {
      return false;
    }
    return exceptions.equals(that.exceptions);
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + exceptions.hashCode();
    return result;
  }
}
