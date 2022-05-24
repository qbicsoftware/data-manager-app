package life.qbic.email;

import life.qbic.usermanagement.registration.Recipient;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface Email {

  String content();

  String subject();

  String from();

  Recipient to();

  String mimeType();
}
