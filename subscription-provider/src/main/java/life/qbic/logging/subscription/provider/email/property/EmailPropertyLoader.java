package life.qbic.logging.subscription.provider.email.property;

import static java.util.Objects.*;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class EmailPropertyLoader {

  private static final String PROPERTIES = "mail.properties";

  public static EmailPropertyLoader create() {
    return new EmailPropertyLoader();
  }

  public Properties load() throws IOException {
    String file = requireNonNull(getClass().getClassLoader().getResource(PROPERTIES), "Cannot find property file. Please make sure to provide a file 'mail.properties' in the resources").getFile();
    return PropertyFileParser.parse(new File(file));
  }

}
