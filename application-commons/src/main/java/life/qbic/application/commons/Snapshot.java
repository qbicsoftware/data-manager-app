package life.qbic.application.commons;

import java.time.Instant;
import java.util.Optional;

/**
 * Interface for the Memento in the <a
 * href="https://refactoring.guru/design-patterns/memento">Memento Pattern</a>
 */
public interface Snapshot {

  Optional<String> getName();

  Instant getTimestamp();

  boolean holdsEqualState(Snapshot snapshot);
}
