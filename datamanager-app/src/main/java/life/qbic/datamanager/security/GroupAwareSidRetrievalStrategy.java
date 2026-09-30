package life.qbic.datamanager.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.identity.api.AuthenticationToUserIdTranslator;
import life.qbic.usergroups.api.GroupSidProvider;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;

/**
 * <b>Group-aware SID retrieval strategy</b>
 *
 * <p>Derives the Spring Security ACL sids of the current caller: the default sids (principal plus
 * one {@link GrantedAuthoritySid} per authority) <em>plus</em> one {@code GrantedAuthoritySid} per
 * live user-group membership, resolved through the {@link GroupSidProvider} facade.</p>
 *
 * <p>User groups are intentionally <b>not</b> injected into the {@link Authentication} at login
 * (strategy §4.3); they are derived fresh on every permission check so that grant and revocation
 * are effective at the next {@code hasPermission(...)} evaluation. Group sids follow the reserved
 * {@code GROUP_<groupId>} prefix keyed on the stable group id (never the display name) so persisted
 * ACEs survive group renames.</p>
 *
 * <p>Groups for which the caller cannot be resolved to a QBiC user id are ignored: no user id, no
 * group sids. Sids are resolved lazily (not at bean construction) so groups created after startup
 * are still recognised.</p>
 *
 * @since 1.19.0
 */
public class GroupAwareSidRetrievalStrategy implements SidRetrievalStrategy {

  private final SidRetrievalStrategyImpl defaultSidRetrievalStrategy = new SidRetrievalStrategyImpl();
  private final GroupSidProvider groupSidProvider;
  private final AuthenticationToUserIdTranslator userIdTranslator;

  public GroupAwareSidRetrievalStrategy(GroupSidProvider groupSidProvider,
      AuthenticationToUserIdTranslator userIdTranslator) {
    this.groupSidProvider = Objects.requireNonNull(groupSidProvider,
        "groupSidProvider must not be null");
    this.userIdTranslator = Objects.requireNonNull(userIdTranslator,
        "userIdTranslator must not be null");
  }

  @Override
  public List<Sid> getSids(Authentication authentication) {
    List<Sid> sids = new ArrayList<>(defaultSidRetrievalStrategy.getSids(authentication));
    userIdTranslator.translateToUserId(authentication)
        .ifPresent(userId -> groupSidProvider.listGroupSidsForUser(userId)
            .stream()
            .map(groupSid -> new GrantedAuthoritySid(groupSid))
            .forEach(sids::add));
    return sids;
  }
}