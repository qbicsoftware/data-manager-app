package life.qbic.projectmanagement.application.sync;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * <b>Shed Lock Config</b>
 *
 * <p>Configuration for a simple lock provider for distributed locking of resources</p>
 * <p>
 * The application will acquire the lock for synchronisations with external resources and will
 * guarantee seamless operation in case more than one instance of the application runs at the same
 * time (e.g. in a high availability environment).
 *
 * @since 1.11.0
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT45S")
// safety cap, in case a VM dies and no lock max was set
public class ShedLockConfig {

  @Bean
  public LockProvider lockProvider(DataSource dataSource) {
    return new JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(new JdbcTemplate(dataSource))
            .usingDbTime() // critical on VMs with skew
            .build());
  }
}
