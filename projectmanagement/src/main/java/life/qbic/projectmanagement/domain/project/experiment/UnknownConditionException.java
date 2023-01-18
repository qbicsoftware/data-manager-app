package life.qbic.projectmanagement.domain.project.experiment;

import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class UnknownConditionException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -4767929765634452078L;

  public UnknownConditionException(String message) {
    super(message);
  }

}
