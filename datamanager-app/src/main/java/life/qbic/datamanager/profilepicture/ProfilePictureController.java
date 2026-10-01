package life.qbic.datamanager.profilepicture;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import life.qbic.datamanager.views.identicon.IdenticonGenerator;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.UriUtils;

/**
 * Serves profile pictures and their identicon fallback.
 *
 * <p>Two endpoints are exposed:</p>
 * <ul>
 *   <li>{@code GET /profile-pictures/{ownerType}/{ownerId}} — the <b>stable</b> URL an avatar
 *   component can use blindly. If a stored picture exists it redirects to the immutable,
 *   content-hashed URL; otherwise it serves the identicon SVG derived from the owner id.</li>
 *   <li>{@code GET /profile-pictures/{ownerType}/{ownerId}/{hash}.png} — the <b>immutable</b>
 *   hashed URL, cacheable for a year.</li>
 * </ul>
 *
 * <p>Read access is deliberately broad: any authenticated user may fetch any avatar, matching the
 * agreed policy. Authentication is enforced here explicitly rather than relying on filter-chain
 * request-matcher ordering, so the endpoint is safe regardless of configuration.</p>
 *
 * @since 1.22.0
 */
@Controller
public class ProfilePictureController {

  private static final String SVG_CONTENT_TYPE = "image/svg+xml";

  private final ProfilePictureService service;

  public ProfilePictureController(ProfilePictureService service) {
    this.service = service;
  }

  /**
   * Stable avatar URL: redirects to the immutable hashed URL when a picture exists, otherwise
   * serves the identicon fallback.
   */
  @GetMapping(ProfilePictureUrlResolver.BASE_PATH + "/{ownerType}/{ownerId}")
  @ResponseBody
  public ResponseEntity<byte[]> stable(@PathVariable String ownerType,
      @PathVariable String ownerId, Authentication authentication) {
    if (!isAuthenticated(authentication)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    Optional<ProfilePictureOwnerType> parsedType = parseOwnerType(ownerType);
    if (parsedType.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    String decodedOwnerId = UriUtils.decode(ownerId, StandardCharsets.UTF_8);
    Optional<ProfilePicture> picture = service.find(parsedType.get(), decodedOwnerId);
    if (picture.isPresent()) {
      String target = hashedPath(parsedType.get(), decodedOwnerId, picture.get().contentHash());
      // The redirect itself must not be cached: the picture may be replaced and would then
      // resolve to a new immutable URL. The target is immutable and cached for a year.
      return ResponseEntity.status(HttpStatus.FOUND)
          .location(URI.create(target))
          .cacheControl(CacheControl.noStore())
          .build();
    }
    byte[] svg = IdenticonGenerator.generateIdenticonSVG(decodedOwnerId)
        .getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .contentType(MediaType.valueOf(SVG_CONTENT_TYPE))
        .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
        .body(svg);
  }

  /**
   * Immutable, content-hashed URL. A {@code 404} is returned when no picture exists or the hash
   * does not match; an {@code If-None-Match} match returns {@code 304}.
   */
  @GetMapping(ProfilePictureUrlResolver.BASE_PATH + "/{ownerType}/{ownerId}/{hash}")
  @ResponseBody
  public ResponseEntity<byte[]> picture(@PathVariable String ownerType,
      @PathVariable String ownerId, @PathVariable String hash, Authentication authentication,
      WebRequest request) {
    if (!isAuthenticated(authentication)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    Optional<ProfilePictureOwnerType> parsedType = parseOwnerType(ownerType);
    if (parsedType.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    String decodedOwnerId = UriUtils.decode(ownerId, StandardCharsets.UTF_8);
    Optional<ProfilePicture> picture = service.find(parsedType.get(), decodedOwnerId);
    if (picture.isEmpty() || !picture.get().contentHash().equals(hash)) {
      return ResponseEntity.notFound().build();
    }

    String etag = "\"" + picture.get().contentHash() + "\"";
    if (request.checkNotModified(etag)) {
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .eTag(etag)
        .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
        .body(picture.get().data());
  }

  private static String hashedPath(ProfilePictureOwnerType ownerType, String ownerId,
      String hash) {
    return ProfilePictureUrlResolver.stablePath(ownerType, ownerId) + "/" + hash;
  }

  private static boolean isAuthenticated(Authentication authentication) {
    return authentication != null && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }

  private static Optional<ProfilePictureOwnerType> parseOwnerType(String value) {
    try {
      return Optional.of(ProfilePictureOwnerType.valueOf(value.toUpperCase(Locale.ROOT)));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
