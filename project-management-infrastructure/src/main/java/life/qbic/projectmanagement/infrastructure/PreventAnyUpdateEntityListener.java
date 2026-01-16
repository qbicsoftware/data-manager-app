package life.qbic.projectmanagement.infrastructure;

import static java.util.Objects.isNull;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * A JPA entity listener that prevents any modifications to entities by throwing an exception for
 * all persistence operations (create, update, delete).
 *
 * <p>This listener is designed to create an immutable state for JPA entities, effectively
 * making them read-only across the entire persistence context. When attached to an entity, it will
 * prevent:
 * <ul>
 *   <li>Creating new instances of the entity</li>
 *   <li>Updating existing entity instances</li>
 *   <li>Removing existing entity instances</li>
 * </ul>
 *
 * <p>Use cases include:
 * <ul>
 *   <li>Implementing audit or historical records that should never be modified</li>
 *   <li>Enforcing strict immutability for certain critical domain objects</li>
 *   <li>Preventing accidental modifications to entities with sensitive data</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @Entity
 * @EntityListeners(PreventAnyUpdateEntityListener.class)
 * public class AuditRecord {
 *     // This entity cannot be persisted, updated, or removed once created
 * }
 * }
 * </pre>
 *
 * @see jakarta.persistence.EntityListeners
 * @see jakarta.persistence.PrePersist
 * @see jakarta.persistence.PreUpdate
 * @see jakarta.persistence.PreRemove
 */
public class PreventAnyUpdateEntityListener {

  /**
   * Custom exception thrown when any persistence operation is attempted. Indicates that the entity
   * is immutable and cannot be modified.
   */
  public static class ForbiddenEntityUpdateException extends RuntimeException {

    public ForbiddenEntityUpdateException(String message) {
      super(message);
    }
  }

  @PrePersist
  void onPrePersist(Object o) {
    throw new ForbiddenEntityUpdateException(
        "JPA tried to persist '" + (isNull(o) ? "null" : o.getClass() + "'"));
  }

  @PreUpdate
  void onPreUpdate(Object o) {
    throw new ForbiddenEntityUpdateException(
        "JPA tried to update '" + (isNull(o) ? "null" : o.getClass() + "'"));
  }

  @PreRemove
  void onPreRemove(Object o) {
    throw new ForbiddenEntityUpdateException(
        "JPA tried to remove '" + (isNull(o) ? "null" : o.getClass() + "'"));
  }


}
