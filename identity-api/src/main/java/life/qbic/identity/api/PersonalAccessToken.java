package life.qbic.identity.api;

import java.time.Instant;

/**
 * <b>Personal Access Token (PAT)</b>
 * <p>
 * Information about the users personal access token creation. The record must never contain the
 * actual token value, since it is a secret.
 *
 * @since 1.0.0
 */
public record PersonalAccessToken(String description, Instant expiration, boolean expired) {

}
