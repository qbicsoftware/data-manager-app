package life.qbic.usergroups.application

import java.util.Optional
import life.qbic.application.commons.SortOrder
import life.qbic.identity.api.UserInfo
import life.qbic.identity.api.UserInformationService

/**
 * In-memory {@link UserInformationService} stub for the user-groups application tests.
 *
 * <p>For the user-existence check in {@link GroupService#addMember} the stub must answer
 * {@code findById} for the users the test scenario treats as existing. By default every
 * user id <em>is</em> considered to exist (so existing tests written before the existence
 * check keep passing); tests that exercise the invalid-user path pass a set of known ids.</p>
 */
class InMemoryUserInformationService implements UserInformationService {

  private final Set<String> knownUserIds

  /** Default: every user exists. */
  InMemoryUserInformationService() {
    this.knownUserIds = null
  }

  /**
   * @param knownUserIds the only user ids that exist; {@code null} means "every user exists"
   */
  InMemoryUserInformationService(Set<String> knownUserIds) {
    this.knownUserIds = knownUserIds
  }

  @Override
  Optional<UserInfo> findByEmail(String emailAddress) {
    return Optional.empty()
  }

  @Override
  Optional<UserInfo> findById(String userId) {
    if (knownUserIds == null || knownUserIds.contains(userId)) {
      return Optional.of(new UserInfo(userId, userId, userId + "@example.org", userId, true, null,
          null))
    }
    return Optional.empty()
  }

  @Override
  boolean isUserNameAvailable(String userName) {
    return true
  }

  @Override
  boolean isEmailAvailable(String email) {
    return true
  }

  @Override
  Optional<UserInfo> findByOidc(String oidcId, String oidcIssuer) {
    return Optional.empty()
  }

  @Override
  List<UserInfo> queryActiveUsersWithFilter(String filter, int offset, int limit,
      List<SortOrder> sortOrders) {
    return []
  }
}