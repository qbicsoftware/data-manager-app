package life.qbic.usergroups.infrastructure;

import java.util.List;
import java.util.Optional;
import life.qbic.usergroups.domain.model.GroupId;
import life.qbic.usergroups.domain.model.GroupStatus;
import life.qbic.usergroups.domain.model.UserGroup;
import life.qbic.usergroups.domain.repository.GroupDataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * <b>Group JPA repository</b>
 *
 * <p>Implementation for the {@link GroupDataStorage} interface.
 *
 * <p>This class serves as an adapter and proxies requests to an JPA implementation to interact
 * with persistent {@link UserGroup} data in the storage layer.
 *
 * <p>The actual JPA implementation is done by {@link QbicGroupRepo}, which is injected as
 * dependency upon creation.
 *
 * <p>{@link org.springframework.dao.DataIntegrityViolationException}s raised by the underlying
 * storage (e.g. the unique index on {@code user_group.name}) are deliberately <b>not</b> caught
 * here — the application layer maps them to a user-friendly duplicate-name error.
 *
 * @since 1.17.0
 */
@Component
public class GroupJpaRepository implements GroupDataStorage {

  private final QbicGroupRepo groupRepo;

  @Autowired
  public GroupJpaRepository(QbicGroupRepo groupRepo) {
    this.groupRepo = groupRepo;
  }

  @Override
  public void save(UserGroup group) {
    groupRepo.save(group);
  }

  @Override
  public Optional<UserGroup> findById(GroupId id) {
    return groupRepo.findById(id);
  }

  @Override
  public Optional<UserGroup> findByNameIgnoreCase(String name) {
    return groupRepo.findByNameIgnoreCase(name);
  }

  @Override
  public List<UserGroup> findAllActive() {
    return groupRepo.findByStatus(GroupStatus.ACTIVE);
  }

  @Override
  public List<UserGroup> findActiveGroupsByUserId(String userId) {
    return groupRepo.findByMemberAndStatus(userId, GroupStatus.ACTIVE);
  }
}