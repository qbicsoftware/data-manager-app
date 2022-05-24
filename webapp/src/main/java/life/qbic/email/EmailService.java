package life.qbic.email;

import org.springframework.stereotype.Service;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Service
public interface EmailService {

  void send(Email email);

}
