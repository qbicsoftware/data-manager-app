package life.qbic.datamanager.security

import life.qbic.identity.api.AuthenticationToUserIdTranslator
import life.qbic.usergroups.api.GroupSidProvider
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.model.Sid
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import spock.lang.Specification

/**
 * Unit tests for the {@link GroupAwareSidRetrievalStrategy}.
 *
 * <p>Verifies that the caller's default sids are extended with one {@code GrantedAuthoritySid}
 * per active group membership, resolved lazily through the {@link GroupSidProvider} facade, and
 * that users who cannot be resolved to a QBiC user id get no group sids (strategy §4.3).</p>
 */
class GroupAwareSidRetrievalStrategySpec extends Specification {

  GroupSidProvider groupSidProvider = Mock()
  AuthenticationToUserIdTranslator userIdTranslator = Mock()
  GroupAwareSidRetrievalStrategy strategy

  def setup() {
    strategy = new GroupAwareSidRetrievalStrategy(groupSidProvider, userIdTranslator)
  }

  def "extends the default sids with one GrantedAuthoritySid per active group membership"() {
    given: "an authenticated user with one authority and two group memberships"
    Authentication authentication = Mock()
    authentication.getName() >> "user-1"
    authentication.getPrincipal() >> "user-1"
    authentication.getAuthorities() >> [new SimpleGrantedAuthority("ROLE_EXAMPLE")]
    userIdTranslator.translateToUserId(authentication) >> Optional.of("user-1")
    groupSidProvider.listGroupSidsForUser("user-1") >> ["GROUP_group-1", "GROUP_group-2"]

    when:
    List<Sid> sids = strategy.getSids(authentication)

    then: "the principal sid and authority sid are present"
    sids.contains(new PrincipalSid("user-1"))
    sids.contains(new GrantedAuthoritySid("ROLE_EXAMPLE"))

    and: "each group membership contributes exactly one GROUP_ authority sid"
    sids.contains(new GrantedAuthoritySid("GROUP_group-1"))
    sids.contains(new GrantedAuthoritySid("GROUP_group-2"))
    sids.count { it instanceof GrantedAuthoritySid && it.grantedAuthority.startsWith("GROUP_") } == 2
  }

  def "returns only default sids when the user cannot be resolved"() {
    given: "an authentication that cannot be translated to a QBiC user id"
    Authentication authentication = Mock()
    authentication.getName() >> "user-1"
    authentication.getPrincipal() >> "user-1"
    authentication.getAuthorities() >> []
    userIdTranslator.translateToUserId(authentication) >> Optional.empty()

    when:
    List<Sid> sids = strategy.getSids(authentication)

    then: "no group sids are provided"
    0 * groupSidProvider.listGroupSidsForUser(*_)
    sids.count { it instanceof GrantedAuthoritySid && it.grantedAuthority.startsWith("GROUP_") } == 0
  }

  def "resolves group sids lazily via the provider (never at construction)"() {
    given: "an authenticated user with no memberships at construction time"
    Authentication authentication = Mock()
    authentication.getName() >> "user-1"
    authentication.getPrincipal() >> "user-1"
    authentication.getAuthorities() >> []
    userIdTranslator.translateToUserId(authentication) >> Optional.of("user-1")

    when: "the strategy is asked for sids after the user joined a group"
    groupSidProvider.listGroupSidsForUser("user-1") >> ["GROUP_newly-joined"]
    List<Sid> sids = strategy.getSids(authentication)

    then: "the newly joined group is recognised even though it did not exist at construction"
    sids.contains(new GrantedAuthoritySid("GROUP_newly-joined"))
  }
}