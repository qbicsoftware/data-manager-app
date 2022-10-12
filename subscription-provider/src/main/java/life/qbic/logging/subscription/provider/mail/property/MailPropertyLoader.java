package life.qbic.logging.subscription.provider.mail.property;

import static java.util.Objects.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * <b>Email Property Loader</b>
 * <p>
 * Tries to load a properties file with name 'mail.properties' from the system resources.
 *
 * @since 1.0.0
 */
public class MailPropertyLoader {

  private static final String PROPERTIES = "mail.properties";

  public static MailPropertyLoader create() {
    return new MailPropertyLoader();
  }

  public Properties load() throws IOException {
    InputStream stream = requireNonNull(getClass().getClassLoader().getResourceAsStream(PROPERTIES),
        "Cannot find property file. Please make sure to provide a file 'mail.properties' in the resources");
    return PropertyFileParser.parse(stream);
  }

}
