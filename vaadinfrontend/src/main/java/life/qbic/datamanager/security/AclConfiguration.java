package life.qbic.datamanager.security;

import javax.sql.DataSource;
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
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
@Configuration
@EnableCaching
@EnableMethodSecurity
public class AclConfiguration {

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
    return new JdbcMutableAclService(dataSource(), lookupStrategy(), aclCache());
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
  public AclAuthorizationStrategy aclAuthorizationStrategy() {
    return new AclAuthorizationStrategyImpl(() -> "read");
  }

  @Bean
  public PermissionGrantingStrategy permissionGrantingStrategy() {
    return new DefaultPermissionGrantingStrategy(new ConsoleAuditLogger());
  }

  @Bean
  public LookupStrategy lookupStrategy() {
    return new BasicLookupStrategy(
        dataSource(),
        aclCache(),
        aclAuthorizationStrategy(),
        new ConsoleAuditLogger()
    );
  }

  @Bean
  public MethodSecurityExpressionHandler defaultMethodSecurityExpressionHandler() {
    var expressionHandler = new DefaultMethodSecurityExpressionHandler();
    var permissionEvaluator = new AclPermissionEvaluator(mutableAclService());
    expressionHandler.setPermissionEvaluator(permissionEvaluator);
    return expressionHandler;
  }
}
