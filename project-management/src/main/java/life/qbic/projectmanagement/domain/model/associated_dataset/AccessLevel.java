package life.qbic.projectmanagement.domain.model.associated_dataset;

/**
 * The access level of a connected dataset at the time of connection.
 *
 * <p>For this iteration (FEAT-DATSET-01), only {@link #PUBLIC} datasets can
 * be connected. Restricted datasets (stories 05/14/15) are a future
 * addition but the domain model is designed to hold this information
 * from the start.</p>
 *
 * @since 1.12.0
 */
public enum AccessLevel {

  /**
   * The dataset is publicly accessible without authentication.
   */
  PUBLIC,

  /**
   * The dataset requires authentication (a token) to access its files.
   */
  RESTRICTED;

  public static AccessLevel parse(String value) {
    try {
      return valueOf(value);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException("Unknown access level: " + value, e);
    }
  }
}
