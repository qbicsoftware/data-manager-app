package life.qbic.projectmanagement.domain.project.experiment;

import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class UnknownVariableLevelException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -2616683268074946569L;

  public UnknownVariableLevelException(String message) {
    super(message);
  }
}
