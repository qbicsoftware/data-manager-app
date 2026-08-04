package life.qbic.projectmanagement.infrastructure.external.invenio;

import java.util.Arrays;
import java.util.Objects;
import life.qbic.projectmanagement.application.associated_dataset.CredentialValidationException;
import life.qbic.projectmanagement.application.associated_dataset.InstanceConfig;
import life.qbic.projectmanagement.infrastructure.external.CredentialValidatorAdapter;

/**
 * Validates InvenioRDM personal access tokens via the spec-defined
 * authenticated-user endpoint.
 *
 * <p>Endpoint: {@code GET /api/users} (operationId: {@code getAUserById})
 * with {@code Authorization: Bearer <token>}.</p>
 *
 * <ul>
 *   <li>{@code 200} — token is valid → returns {@code true}</li>
 *   <li>{@code 401 / 403} — token is invalid or expired → returns
 *       {@code false}</li>
 *   <li>Transient failures (5xx, network errors) → throws
 *       {@link CredentialValidationException}</li>
 * </ul>
 *
 * <p>The plaintext token is zeroed in a {@code finally} block after
 * use (ADR-0002 D1 decryption boundary).</p>
 *
 * @since 1.12.0
 */
public class InvenioRdmCredentialValidatorAdapter implements CredentialValidatorAdapter {

  private final InvenioRdmClient client;

  public InvenioRdmCredentialValidatorAdapter(InvenioRdmClient client) {
    this.client = Objects.requireNonNull(client, "client must not be null");
  }

  @Override
  public boolean validate(InstanceConfig config, char[] token) {
    Objects.requireNonNull(config, "config must not be null");
    Objects.requireNonNull(token, "token must not be null");

    // Copy token to build the auth header string in local scope.
    // The original token array is zeroed in the finally block per the
    // CredentialValidatorAdapter contract (ADR-0002 D1).
    char[] tokenCopy = Arrays.copyOf(token, token.length);
    try {
      String authHeader = "Bearer " + new String(tokenCopy);
      client.getAuthenticatedUser(config.baseUrl(), authHeader);
      // 200 response → token is valid
      return true;
    } catch (InvenioRdmClient.InvenioRdmPermanentException e) {
      // 401 / 403 → token rejected
      if (e.getStatusCode() == 401 || e.getStatusCode() == 403) {
        return false;
      }
      // Other 4xx → unexpected (e.g. 404 means endpoint missing)
      throw new CredentialValidationException(
          "Unexpected error validating token on " + config.displayName()
              + " (status " + e.getStatusCode() + ")", e);
    } catch (InvenioRdmClient.InvenioRdmTransientException e) {
      // 5xx / network error → retry exhausted, surface to caller
      throw new CredentialValidationException(
          "Token validation failed on " + config.displayName()
              + " due to a transient error", e);
    } catch (InvenioRdmClient.InvenioRdmInterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CredentialValidationException(
          "Token validation on " + config.displayName()
              + " was interrupted", e);
    } finally {
      Arrays.fill(tokenCopy, '\0');
      Arrays.fill(token, '\0');
    }
  }

}
