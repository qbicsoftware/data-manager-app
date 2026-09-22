package life.qbic.usergroups.domain.repository;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import life.qbic.usergroups.domain.model.GroupId;
import life.qbic.usergroups.domain.model.GroupName;
import life.qbic.usergroups.domain.model.UserGroup;

/**
 * <b>Provides stateless access and storage functionality for {@link UserGroup} entities.</b>
 *
 * <p>Mirrors the {@code UserRepository} singleton fabric pattern of the identity context. The
 * application layer obtains a {@link GroupRepository} instance via
 * {@link #getInstance(GroupDataStorage)}.
 *
 * @since 1.0.0
 */
public class GroupRepository implements Serializable {

  @Serial
  private static final long serialVersionUID = 9123456789012345678L;

  private static GroupRepository instance;

  private final GroupDataStorage dataStorage;

  protected GroupRepository(GroupDataStorage dataStorage) {
    this.dataStorage = dataStorage;
  }

  /**
   * Retrieves a Singleton instance of a group {@link GroupRepository}. In case this method is
   * called the first time, a new instance is created.
   *
   * @param dataStorage an implementation of {@link GroupDataStorage}, handling the low level
   *                    persistence layer access.
   * @return a Singleton instance of a group repository.
   * @since 1.0.0
   */
  public static GroupRepository getInstance(GroupDataStorage dataStorage) {
    if (instance == null) {
      instance = new GroupRepository(dataStorage);
    }
    return instance;
  }

  /**
   * Finds a group matching a provided group id.
   *
   * @param groupId the group id, e.g. obtained from the caller
   * @return the group if present in the repository, else an {@link Optional#empty()}.
   * @since 1.0.0
   */
  public Optional<UserGroup> findById(GroupId groupId) {
    return dataStorage.findById(groupId);
  }

  /**
   * Finds a group by its display name, ignoring case.
   *
   * @param name the group name to search for
   * @return the group if present, else an {@link Optional#empty()}.
   * @since 1.0.0
   */
  public Optional<UserGroup> findByNameIgnoreCase(GroupName name) {
    return dataStorage.findByNameIgnoreCase(name.value());
  }

  /**
   * Retrieves all active groups within the user groups context.
   *
   * @return list of {@link UserGroup} objects with their status set to {@code ACTIVE}
   * @since 1.0.0
   */
  public List<UserGroup> findAllActive() {
    return dataStorage.findAllActive();
  }

  /**
   * Retrieves all active groups the given user is a member of.
   *
   * @param userId the user id to match memberships for
   * @return list of active {@link UserGroup} objects the user belongs to
   * @since 1.0.0
   */
  public List<UserGroup> findActiveGroupsByUserId(String userId) {
    return dataStorage.findActiveGroupsByUserId(userId);
  }

  /**
   * Adds a group to the repository, delegating to the underlying data storage.
   *
   * <p>This is the write path used by the domain service for both creation and update of a group.
   *
   * @param group the group to store
   * @throws GroupStorageException if the group could not be stored
   * @since 1.0.0
   */
  public void store(UserGroup group) throws GroupStorageException {
    try {
      dataStorage.save(group);
    } catch (Exception e) {
      throw new GroupStorageException(e);
    }
  }

  /**
   * Indicates a failure to persist a {@link UserGroup}.
   *
   * @since 1.0.0
   */
  public static class GroupStorageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 912345678901234567L;

    public GroupStorageException() {
      super();
    }

    public GroupStorageException(Throwable cause) {
      super(cause);
    }
  }
}