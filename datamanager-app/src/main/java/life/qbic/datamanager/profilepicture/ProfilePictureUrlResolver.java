package life.qbic.datamanager.profilepicture;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

/**
 * Resolves the immutable, content-hashed delivery URL for a stored profile picture.
 *
 * <p><b>Context path:</b> the application may run under a servlet context path (e.g. {@code /dev}).
 * All URLs built here include it. The static helpers read the current {@link VaadinRequest} when
 * available; outside a Vaadin request (tests, background code) an empty context path is assumed,
 * and the controller builds its redirect from the actual {@code HttpServletRequest} instead.</p>
 *
 * @since 1.22.0
 */
@Component
public class ProfilePictureUrlResolver {

  /** Base path of the delivery endpoint, relative to the servlet context path. */
  public static final String BASE_PATH = "/profile-pictures";

  private final ProfilePictureService service;

  public ProfilePictureUrlResolver(ProfilePictureService service) {
    this.service = service;
  }

  /**
   * Builds the stable avatar URL for an owner, including the current servlet context path. The
   * endpoint redirects to the immutable hashed URL or serves the identicon fallback.
   *
   * @param ownerType the owner kind
   * @param ownerId   the owner id
   * @return the stable avatar URL
   */
  public static String stablePath(ProfilePictureOwnerType ownerType, String ownerId) {
    return contextPath() + relativeStablePath(ownerType, ownerId);
  }

  /**
   * Builds the stable path relative to the servlet context path (no context path prefix).
   *
   * @param ownerType the owner kind
   * @param ownerId   the owner id
   * @return the context-relative stable path
   */
  public static String relativeStablePath(ProfilePictureOwnerType ownerType, String ownerId) {
    return BASE_PATH + "/" + ownerType.name().toLowerCase(Locale.ROOT) + "/"
        + UriUtils.encodePathSegment(ownerId, StandardCharsets.UTF_8);
  }

  /**
   * The current servlet context path, or an empty string when there is no active Vaadin request.
   *
   * @return the context path, never {@code null}
   */
  public static String contextPath() {
    try {
      VaadinRequest request = VaadinService.getCurrentRequest();
      String contextPath = request == null ? null : request.getContextPath();
      return contextPath == null ? "" : contextPath;
    } catch (RuntimeException e) {
      return "";
    }
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
