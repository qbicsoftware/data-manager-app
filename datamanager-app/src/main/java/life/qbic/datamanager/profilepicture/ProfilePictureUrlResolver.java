package life.qbic.datamanager.profilepicture;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

/**
 * Resolves the immutable, content-hashed delivery URL for a stored profile picture.
 *
 * <p><b>Context path:</b> the application may run under a servlet context path (e.g. {@code /dev}).
 * All URLs built here include it. The static helpers read the current {@link VaadinRequest} when
 * available and otherwise fall back to the configured {@code server.servlet.context-path}, so URLs
 * built on a background thread (e.g. inside {@code UI.access}) keep the prefix. The controller
 * builds its redirect from the actual {@code HttpServletRequest} instead.</p>
 *
 * @since 1.19.0
 */
@Component
public class ProfilePictureUrlResolver {

  /** Base path of the delivery endpoint, relative to the servlet context path. */
  public static final String BASE_PATH = "/profile-pictures";

  /**
   * The servlet context path the application is deployed under, captured once from the
   * {@code server.servlet.context-path} configuration (see {@code application.properties}). It is
   * used as a fallback whenever a URL is built outside a Vaadin request.
   *
   * <p>This matters because avatar URLs are also built from {@code UI.access(...)} callbacks (for
   * example when the access roster is re-rendered after a grant). Inside such a callback
   * {@link VaadinService#getCurrentRequest()} is deliberately undefined, so
   * {@link #contextPath()} would otherwise return an empty string and drop the context path from
   * the URL. The browser would then request the avatar at the wrong path, the request would 404,
   * and the {@code Avatar} component would fall back to showing initials instead of the picture.</p>
   */
  private static volatile String configuredContextPath = "";

  private final ProfilePictureService service;

  public ProfilePictureUrlResolver(ProfilePictureService service,
      @Value("${server.servlet.context-path:}") String contextPath) {
    this.service = service;
    configuredContextPath = normalizeContextPath(contextPath);
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
   * The servlet context path to prefix avatar URLs with.
   *
   * <p>Inside an active Vaadin request the actual request context path is used. Outside a request
   * (for example inside {@code UI.access}) the context path configured via
   * {@code server.servlet.context-path} is used, so URLs built on a background re-render keep the
   * prefix and remain resolvable by the browser.</p>
   *
   * @return the context path, never {@code null}
   */
  public static String contextPath() {
    String requestContextPath = requestContextPath();
    return requestContextPath != null ? requestContextPath : configuredContextPath;
  }

  /**
   * The context path of the current Vaadin request, or {@code null} when there is no active request
   * (e.g. inside {@code UI.access}) so the caller can fall back to the configured context path.
   */
  private static String requestContextPath() {
    try {
      VaadinRequest request = VaadinService.getCurrentRequest();
      return request == null ? null : request.getContextPath();
    } catch (RuntimeException e) {
      return null;
    }
  }

  /**
   * Normalises a configured context path: {@code null}, blank and {@code "/"} all mean "no context
   * path", and a trailing slash is removed so the result can be concatenated safely.
   */
  private static String normalizeContextPath(String contextPath) {
    if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
      return "";
    }
    return contextPath.endsWith("/")
        ? contextPath.substring(0, contextPath.length() - 1)
        : contextPath;
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
