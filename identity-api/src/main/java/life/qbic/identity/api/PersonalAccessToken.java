package life.qbic.identity.api;

import java.time.Instant;

/**
 * <b>Personal Access Token (PAT)</b>
 * <p>
 * Information about the users personal access token creation. The record must never contain the
 * actual token value, since it is a secret.
 *
 * @param tokenId      the unique token identifier
 * @param description  the user-provided description of the token
 * @param creationDate the point in time the token was created
 * @param expiration   the point in time the token expires
 * @param expired      whether the token has already expired
 * @since 1.0.0
 */
public record PersonalAccessToken(String tokenId, String description, Instant creationDate,
                                  Instant expiration, boolean expired) {

}
