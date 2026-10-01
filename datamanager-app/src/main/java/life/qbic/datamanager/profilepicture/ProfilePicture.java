package life.qbic.datamanager.profilepicture;

import java.time.Instant;

/**
 * Read model for a stored profile picture.
 *
 * @param ownerType   the kind of owner
 * @param ownerId     the owner id (user id or group id)
 * @param contentType the stored content type (always {@code image/png})
 * @param contentHash the SHA-256 hex digest of {@link #data}
 * @param width       derivative width in pixels
 * @param height      derivative height in pixels
 * @param data        the normalized PNG bytes
 * @param updatedAt   the last write timestamp
 * @since 1.22.0
 */
public record ProfilePicture(ProfilePictureOwnerType ownerType, String ownerId, String contentType,
                             String contentHash, int width, int height, byte[] data,
                             Instant updatedAt) {

}
