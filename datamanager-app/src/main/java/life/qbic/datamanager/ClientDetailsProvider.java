package life.qbic.datamanager;

import java.util.Optional;

/**
 * Provides information about the client
 */
public interface ClientDetailsProvider {

  record ClientDetails(String timeZoneId) {

  }

  /**
   * @return the latest client details or empty if none were fetched
   */
  Optional<ClientDetails> latestDetails();
}
