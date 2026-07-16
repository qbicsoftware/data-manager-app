package life.qbic.projectmanagement.domain.model.associated_dataset;

/**
 * Represents the lifecycle state of a dataset connection to a project.
 *
 * <p>Per ADR-0001, removal uses soft-delete: a connection transitions
 * to {@link #REMOVED} and is retained as a tombstone for audit. The
 * {@link #CONNECTED} state is the only active state in v1.</p>
 *
 * @since 1.12.0
 */
public enum ConnectionState {

  /**
   * The dataset is actively connected to the project.
   */
  CONNECTED,

  /**
   * The connection has been removed (soft-delete tombstone retained for audit).
   * No UI exposure in v1 per ADR-0001.
   */
  REMOVED;

  public static ConnectionState parse(String value) {
    try {
      return valueOf(value);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException("Unknown connection state: " + value, e);
    }
  }
}
