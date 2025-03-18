package life.qbic.projectmanagement.application.api.fair;

import java.io.InputStream;
import java.util.Optional;
import org.springframework.util.MimeType;

/**
 * A simple facade to describe a light version of a digital object.
 *
 * @since 1.10.0
 */
public interface DigitalObject {

  /**
   * The content as an {@link InputStream}.
   *
   * @return the content if available or <code>null</code> if no content is available.
   * @since 1.10.0
   */
  InputStream content();

  /**
   * The type of the content, following {@link MimeType} convention.
   *
   * @return the type of the content or <code>null</code> if no content is present.s
   * @since 1.10.0
   */
  MimeType mimeType();

  /**
   * The name of the object.
   *
   * @return the name of the digital object if present.
   * @since 1.10.0
   */
  Optional<String> name();

  /**
   * The ID of the object.
   *
   * @return The identifier of the digital object, if already present.
   * @since 1.10.0
   */
  Optional<String> id();

}
