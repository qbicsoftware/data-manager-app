package life.qbic.datamanager.security

import life.qbic.projectmanagement.application.authorization.authorities.UserAuthorityProvider
import org.springframework.security.core.authority.SimpleGrantedAuthority
import spock.lang.Specification

/**
 * Unit tests for the {@link QbicGroupAdministrationPermission} adapter.
 *
 * <p>Covers the mapping from the {@link UserAuthorityProvider} authorities to the admin
 * decision: users holding {@code ROLE_ADMIN} are administrators, everyone else (regular users
 * and unknown ids) is not.</p>
 */
class QbicGroupAdministrationPermissionSpec extends Specification {

  UserAuthorityProvider userAuthorityProvider = Mock(UserAuthorityProvider)
  QbicGroupAdministrationPermission permission =
      new QbicGroupAdministrationPermission(userAuthorityProvider)

  def "a user with ROLE_ADMIN is an administrator"() {
    given: "an admin user id with the ROLE_ADMIN authority"
    String adminUserId = "admin-1"
    userAuthorityProvider.getAuthoritiesByUserId(adminUserId) >> [
        new SimpleGrantedAuthority("ROLE_ADMIN"),
        new SimpleGrantedAuthority("acl:change-owner")]

    expect:
    permission.isAdmin(adminUserId)
  }

  def "a regular user without ROLE_ADMIN is not an administrator"() {
    given: "a regular user with only a USER role"
    String userId = "user-1"
    userAuthorityProvider.getAuthoritiesByUserId(userId) >> [
        new SimpleGrantedAuthority("ROLE_USER")]

    expect:
    !permission.isAdmin(userId)
  }

  def "an unknown user id is not an administrator"() {
    given: "a user id with no authorities at all"
    String ghostUserId = "ghost-user"
    userAuthorityProvider.getAuthoritiesByUserId(ghostUserId) >> []

    expect:
    !permission.isAdmin(ghostUserId)
  }

  def "a blank or null user id is never an administrator"() {
    expect:
    !permission.isAdmin("  ")
    !permission.isAdmin(null)
  }
}