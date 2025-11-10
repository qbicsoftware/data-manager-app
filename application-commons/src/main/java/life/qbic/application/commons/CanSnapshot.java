package life.qbic.application.commons;

/**
 * Interface to implement the Memento/Snapshot pattern. <a
 * href="https://refactoring.guru/design-patterns/memento">Memento Pattern</a> Originators of
 * Mementos or Creators of Snapshots should implement this interface.
 */
public interface CanSnapshot {

  /**
   * Capture the internal state and expose it as a Snapshot.
   *
   * @return the created snapshot
   */
  Snapshot snapshot();

  /**
   * Adapts the internal state to match the information provided by the snapshot. The method can
   * fail throwing a {@link SnapshotRestorationException} for unknown types of Snapshots.
   * <p>
   * It is the responsibility of the implementation to make sure after successful application or
   * failure, the internal state of the object is valid.
   * <p>
   * After snapshot restoration failure. The object is expected to revert all modifications made by
   * the attempt.
   *
   * @param snapshot the snapshot to be restored
   * @throws SnapshotRestorationException thrown when snapshot restoration failed.
   */
  void restore(Snapshot snapshot) throws SnapshotRestorationException;

  /**
   * Indicates that a snapshot cannot be used to restore the state of the originator.
   */
  class SnapshotRestorationException extends RuntimeException {

    public SnapshotRestorationException(String message) {
      super(message);
    }

    public SnapshotRestorationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
