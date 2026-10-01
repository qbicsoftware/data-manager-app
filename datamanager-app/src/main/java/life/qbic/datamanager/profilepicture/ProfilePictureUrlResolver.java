package life.qbic.datamanager.profilepicture;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

/**
 * Resolves the immutable, content-hashed delivery URL for a stored profile picture.
 *
 * <p>View code uses this to decide whether to render a stored picture or fall back to the
 * identicon: an {@link Optional#isEmpty() empty} result means no picture is set. The URL embeds the
 * content hash, so a replaced picture gets a new URL and no cache invalidation is needed.</p>
 *
 * @since 1.22.0
 */
@Component
public class ProfilePictureUrlResolver {

  /** Base path of the delivery endpoint. Must match {@link ProfilePictureController}. */
  public static final String BASE_PATH = "/profile-pictures";

  private final ProfilePictureService service;

  public ProfilePictureUrlResolver(ProfilePictureService service) {
    this.service = service;
  }

  /**
   * Builds the stable avatar URL for an owner, regardless of whether a picture is set. The
   * endpoint redirects to the immutable hashed URL or serves the identicon fallback.
   *
   * @param ownerType the owner kind
   * @param ownerId   the owner id
   * @return the stable avatar URL
   */
  public static String stablePath(ProfilePictureOwnerType ownerType, String ownerId) {
    return BASE_PATH + "/" + ownerType.name().toLowerCase(java.util.Locale.ROOT) + "/"
        + UriUtils.encodePathSegment(ownerId, StandardCharsets.UTF_8);
  }

  /**
   * @param ownerType the owner kind
   * @param ownerId   the owner id
   * @return the immutable URL of the stored picture, or empty when none exists
   */
  public Optional<String> resolve(ProfilePictureOwnerType ownerType, String ownerId) {
    if (ownerType == null || ownerId == null || ownerId.isBlank()) {
      return Optional.empty();
    }
    return service.findContentHash(ownerType, ownerId)
        .map(hash -> stablePath(ownerType, ownerId) + "/" + hash);
  }
}
