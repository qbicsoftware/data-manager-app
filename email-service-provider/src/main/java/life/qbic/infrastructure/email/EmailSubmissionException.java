package life.qbic.infrastructure.email;

import java.io.Serial;

/**
 * <b>Email Submission Exception</b>
 *
 * <p>The infrastructure layer's own failure type for a message that could not be handed to the SMTP
 * server.</p>
 *
 * <h3>Why this type exists, and why it is thrown here rather than a context type</h3>
 *
 * <p>{@link EmailServiceProvider} is shared infrastructure: it is used by the identity, project
 * management and user-groups contexts, each of which defines its <em>own</em>
 * {@code CommunicationException}. Those are three distinct classes that merely share a name. Throwing
 * one context's type from shared code therefore breaks the other two: a caller in the user-groups
 * context catching its own {@code CommunicationException} would never see it.</p>
 *
 * <p>So the provider throws this context-free type, and each adapter in
 * {@code infrastructure.email.<context>} translates it into the exception that context's port
 * declares. For a long time this class was declared on the method signatures but never thrown — the
 * provider threw identity's {@code CommunicationException} instead — which made the declaration a
 * lie and the adapters' catch blocks unreachable.</p>
 *
 * @since 1.0.0
 */
public class EmailSubmissionException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -8309431489482091477L;

  public EmailSubmissionException(String message) {
    super(message);
  }

  public EmailSubmissionException(String message, Throwable cause) {
    super(message, cause);
  }

}
