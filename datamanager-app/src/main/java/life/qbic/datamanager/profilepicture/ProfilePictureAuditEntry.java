package life.qbic.datamanager.profilepicture;

import java.time.Instant;

/**
 * One row of the profile-picture audit trail (set/replace only).
 *
 * @param ownerType          the kind of owner
 * @param ownerId            the owner id
 * @param action             {@code SET} or {@code REPLACE}
 * @param actorId            the user who performed the change
 * @param previousContentHash the replaced picture's hash, or {@code null} for a first set
 * @param newContentHash     the new picture's hash
 * @param createdAt          when the change happened
 * @since 1.22.0
 */
public record ProfilePictureAuditEntry(ProfilePictureOwnerType ownerType, String ownerId,
                                       String action, String actorId, String previousContentHash,
                                       String newContentHash, Instant createdAt) {

  /** A first-time set (no prior picture existed). */
  public static final String ACTION_SET = "SET";
  /** A replacement of an existing picture. */
  public static final String ACTION_REPLACE = "REPLACE";
}
