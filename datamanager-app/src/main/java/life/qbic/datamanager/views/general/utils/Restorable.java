package life.qbic.datamanager.views.general.utils;

import java.time.Instant;
import java.util.Optional;

/**
 * Enables a component to snapshot the current state into a memento to be restored later. Does not
 * leak information from the snapshot to the outside. See <a
 * href="https://refactoring.guru/design-patterns/memento">https://refactoring.guru/design-patterns/memento</a>
 * for more information.
 */
public interface Restorable {

  /**
   * A data object holding information on the state of the {@link Restorable} component.
   */
  interface Snapshot {

    /**
     * @return the point in time the state was captured
     */
    Instant getTimestamp();

    /**
     * Snapshots can be named. By default, no name is provided.
     *
     * @return an optional name for the snapshot
     */
    default Optional<String> getName() {
      return Optional.empty();
    }
  }

  /**
   * Take a snapshot and capture the current state.
   *
   * @return the generated snapshot
   */
  Snapshot snapshot();

  /**
   * Restore a snapshot. Restores the internal state of the Restorable object to match the state
   * captured in the snapshot.
   * <p>
   * Implementing classes can decide on the type of Snapshots they support. If a snapshot is not
   * recognized or not supported by this Restorable, a {@link UnsupportedSnapshotTypeException} must
   * be thrown.
   *
   * @param snapshot the snapshot to restore
   * @throws UnsupportedOperationException in case the snapshot is of an unsupported type.
   */
  void restore(Snapshot snapshot) throws UnsupportedOperationException;

  /**
   * Indicate the snapshot is not of a valid type to be restored by this {@link Restorable}.
   */
  class UnsupportedSnapshotTypeException extends RuntimeException {

    public UnsupportedSnapshotTypeException(String message) {
      super(message);
    }
  }
}
