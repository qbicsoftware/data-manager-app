package life.qbic.datamanager.security;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategy;
import javax.sql.DataSource;
import life.qbic.identity.api.AuthenticationToUserIdTranslator;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.acl.QbicPermissionEvaluator;
import life.qbic.usergroups.api.GroupSidProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.AuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.AuditableAccessControlEntry;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.util.Assert;

/**
 * Security configuration setting up access control list beans
 */
@Configuration
@EnableCaching
@EnableMethodSecurity
public class AclSecurityConfiguration {

  private static final Logger log = logger(AclSecurityConfiguration.class);

  private final GroupSidProvider groupSidProvider;
  private final AuthenticationToUserIdTranslator authenticationToUserIdTranslator;

  public AclSecurityConfiguration(GroupSidProvider groupSidProvider,
      AuthenticationToUserIdTranslator authenticationToUserIdTranslator) {
    this.groupSidProvider = groupSidProvider;
    this.authenticationToUserIdTranslator = authenticationToUserIdTranslator;
  }

  private GroupAwareSidRetrievalStrategy groupAwareSidRetrievalStrategy() {
    return new GroupAwareSidRetrievalStrategy(groupSidProvider, authenticationToUserIdTranslator);
  }


  @Bean
  public MutableAclService mutableAclService(CacheManager cacheManager,
      @Qualifier("dataManagementDataSource") DataSource dataSource) {
    JdbcMutableAclService jdbcMutableAclService = new JdbcMutableAclService(dataSource,
        lookupStrategy(cacheManager, dataSource), aclCache(cacheManager));
    // allow for non-long type ids
    jdbcMutableAclService.setAclClassIdSupported(true);
    jdbcMutableAclService.setSecurityContextHolderStrategy(securityContextHolderStrategy());

    return jdbcMutableAclService;
  }

  @Bean
  protected AclCache aclCache(CacheManager cacheManager) {
    return new SpringCacheBasedAclCache(
        cacheManager.getCache("acl_cache"),
        permissionGrantingStrategy(),
        aclAuthorizationStrategy());
  }

  @Bean
  public AuditLogger auditLogger() {
    return (granted, ace) -> {
      Assert.notNull(ace, "AccessControlEntry required");
      if (ace instanceof AuditableAccessControlEntry auditableAce) {
        if (granted && auditableAce.isAuditSuccess()) {
          log.info("GRANTED due to ACE: " + ace);
        } else if (!granted && auditableAce.isAuditFailure()) {
          log.info("DENIED due to ACE: " + ace);
        }
      }
    };
  }


  @Bean
  public AclAuthorizationStrategy aclAuthorizationStrategy() {
    return buildAclAuthorizationStrategy();
  }

  /**
   * Builds the {@link AclAuthorizationStrategy} used both as a Spring bean and inside the
   * {@code aclCache}/{@code lookupStrategy} beans.
   *
   * <p>The strategy performs the authorization check inside {@code AclImpl.deleteAce}/
   * {@code insertAce}/{@code setOwner}. Without the group-aware SID retrieval strategy it only
   * sees the principal + raw authorities and therefore cannot authorise a group-admin member
   * who changes/revokes a group grant (see {@link GroupAwareSidRetrievalStrategy}); the group
   * SIDs are injected through Spring bean wiring (see {@code permissionEvaluator}).</p>
   */
  private AclAuthorizationStrategy buildAclAuthorizationStrategy() {
    AclAuthorizationStrategyImpl aclAuthorizationStrategy = new AclAuthorizationStrategyImpl(
        new SimpleGrantedAuthority("acl:change-owner"), //give this to ROLE_ADMIN
        new SimpleGrantedAuthority("acl:change-audit"), // give this to ROLE_ADMIN
        new SimpleGrantedAuthority("acl:change-access")
        //give this to ROLE_ADMIN, ROLE_PROJECT_MANAGER, ROLE_USER it is needed to remove yourself from a project
    );

    aclAuthorizationStrategy.setSecurityContextHolderStrategy(securityContextHolderStrategy());
    // Critical: without the group-aware SID retrieval strategy, the internal authorization check
    // inside AclImpl.deleteAce/insertAce (AclAuthorizationStrategyImpl.securityCheck) only sees
    // the principal + raw authorities. A group-admin member changing/revoking a group grant would
    // therefore fail with "Unable to locate a matching ACE" even though they hold ADMIN via a group.
    aclAuthorizationStrategy.setSidRetrievalStrategy(groupAwareSidRetrievalStrategy());
    return aclAuthorizationStrategy;
  }

  protected SecurityContextHolderStrategy securityContextHolderStrategy() {
    return new VaadinAwareSecurityContextHolderStrategy();
  }

  @Bean
  public PermissionGrantingStrategy permissionGrantingStrategy() {
    return new DefaultPermissionGrantingStrategy(auditLogger());
  }

  @Bean
  public LookupStrategy lookupStrategy(CacheManager cacheManager,
      @Qualifier("dataManagementDataSource") DataSource dataSource) {
    BasicLookupStrategy basicLookupStrategy = new BasicLookupStrategy(
        dataSource,
        aclCache(cacheManager),
        aclAuthorizationStrategy(),
        auditLogger()
    );
    basicLookupStrategy.setAclClassIdSupported(true);
    return basicLookupStrategy;
  }

  @Bean(name = "qbicPermissionEvaluator")
  AclPermissionEvaluator permissionEvaluator(
      @Qualifier("mutableAclService") MutableAclService mutableAclService,
      GroupSidProvider groupSidProvider,
      AuthenticationToUserIdTranslator authenticationToUserIdTranslator) {
    QbicPermissionEvaluator qbicPermissionEvaluator = new QbicPermissionEvaluator(
        mutableAclService);
    qbicPermissionEvaluator.setSidRetrievalStrategy(groupAwareSidRetrievalStrategy());
    return qbicPermissionEvaluator;
  }

  @Bean
  public MethodSecurityExpressionHandler defaultMethodSecurityExpressionHandler(
      @Qualifier("qbicPermissionEvaluator") PermissionEvaluator permissionEvaluator) {
    var expressionHandler = new DefaultMethodSecurityExpressionHandler();
    expressionHandler.setPermissionEvaluator(permissionEvaluator);
    return expressionHandler;
  }
}
