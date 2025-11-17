package life.qbic.datamanager.configuration;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures a datasource to be used by JobRunr.
 * <p>
 * The configured DataSource is injectable through {@code @Qualifier("jobRunrDatasource")}
 */
@Configuration
public class JobRunrDatasourceConfig {

  @Bean(name = "jobRunrDatasourceProperties")
  @ConfigurationProperties("org.jobrunr.database.datasource")
  public DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean(name = "jobRunrDatasource")
  public DataSource jobrunrDatasource(
      @Qualifier("jobRunrDatasourceProperties") DataSourceProperties dataSourceProperties
  ) {
    return dataSourceProperties.initializeDataSourceBuilder().build();
  }
}
