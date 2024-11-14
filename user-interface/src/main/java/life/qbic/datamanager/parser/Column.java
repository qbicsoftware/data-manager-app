package life.qbic.datamanager.parser;

import java.util.Optional;
import life.qbic.datamanager.parser.ExampleProvider.Helper;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface Column {

  Optional<Helper> getFillHelp();
}
