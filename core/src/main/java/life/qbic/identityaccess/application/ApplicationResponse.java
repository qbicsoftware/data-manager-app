package life.qbic.identityaccess.application;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import life.qbic.identityaccess.application.user.UserRegistrationService;

/**
 * <b>Application Response</b>
 *
 * <p>Application responses can be used to report successful or failing application operations.</p>
 *
 * @since 1.0.0
 */
public class ApplicationResponse {

  private enum Type {SUCCESSFUL, FAILED}

  private ApplicationResponse.Type type;

  private List<RuntimeException> exceptions;

  public static ApplicationResponse successResponse() {
    var successResponse = new ApplicationResponse();
    successResponse.setType(ApplicationResponse.Type.SUCCESSFUL);
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

  private ApplicationResponse() {
    super();
  }

  private void setType(ApplicationResponse.Type type) {
    this.type = type;
  }

  public ApplicationResponse.Type getType() {
    return type;
  }

  private void setExceptions(RuntimeException... exceptions) {
    this.exceptions = Arrays.stream(exceptions).toList();
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

}
