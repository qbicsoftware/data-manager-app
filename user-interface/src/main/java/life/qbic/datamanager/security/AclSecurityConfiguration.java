package life.qbic.datamanager.security;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategy;
import javax.sql.DataSource;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.acl.QbicPermissionEvaluator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
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

  @Value("${spring.datasource.url}")
  String url;
  @Value("${spring.datasource.username}")
  String name;
  @Value("${spring.datasource.password}")
  String password;

  @Bean
  public DataSource dataSource() {
    var ds = new DriverManagerDataSource();
    ds.setUrl(url);
    ds.setUsername(name);
    ds.setPassword(password);
    return ds;
  }

  @Bean
  public MutableAclService mutableAclService() {
    JdbcMutableAclService jdbcMutableAclService = new JdbcMutableAclService(dataSource(),
        lookupStrategy(), aclCache());
    // allow for non-long type ids
    jdbcMutableAclService.setAclClassIdSupported(true);
    jdbcMutableAclService.setSecurityContextHolderStrategy(securityContextHolderStrategy());

    return jdbcMutableAclService;
  }

  @Bean
  protected AclCache aclCache() {
    CacheManager cacheManager = new ConcurrentMapCacheManager();
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
    AclAuthorizationStrategyImpl aclAuthorizationStrategy = new AclAuthorizationStrategyImpl(
        new SimpleGrantedAuthority("acl:change-owner"), //give this to ROLE_ADMIN
        new SimpleGrantedAuthority("acl:change-audit"), // give this to ROLE_ADMIN
        new SimpleGrantedAuthority("acl:change-access")
        //give this to ROLE_ADMIN, ROLE_PROJECT_MANAGER, ROLE_USER it is needed to remove yourself from a project
    );

    aclAuthorizationStrategy.setSecurityContextHolderStrategy(securityContextHolderStrategy());
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
  public LookupStrategy lookupStrategy() {
    BasicLookupStrategy basicLookupStrategy = new BasicLookupStrategy(
        dataSource(),
        aclCache(),
        aclAuthorizationStrategy(),
        auditLogger()
    );
    basicLookupStrategy.setAclClassIdSupported(true);
    return basicLookupStrategy;
  }

  @Bean(name = "qbicPermissionEvaluator")
  AclPermissionEvaluator permissionEvaluator() {
    return new QbicPermissionEvaluator(mutableAclService());
  }

  @Bean
  public MethodSecurityExpressionHandler defaultMethodSecurityExpressionHandler() {
    var expressionHandler = new DefaultMethodSecurityExpressionHandler();
    expressionHandler.setPermissionEvaluator(permissionEvaluator());
    return expressionHandler;
  }
}
