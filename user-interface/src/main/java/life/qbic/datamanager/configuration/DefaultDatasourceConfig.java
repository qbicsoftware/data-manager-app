package life.qbic.datamanager.configuration;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configures the default datasource handling entities in package {@link life.qbic} The DataSource
 * configured by this class is called {@code datasource} and is injectable through
 * {@code @Qualifier("datasource")}.
 * <p>
 * Further the configuration registeres a {@link EntityManagerFactory} looking for entities from
 * {@link life.qbic} and a {@link org.springframework.transaction.TransactionManager}.
 *
 * <p>
 * It is injectable through {@code @Qualifier("datasource")}
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager",
    basePackages = "life.qbic")
public class DefaultDatasourceConfig {

  @Primary
  @Bean(name = "datasourceProperties")
  @ConfigurationProperties("spring.datasource")
  public DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @Primary
  @Bean(name = "datasource")
//  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSource jobrunrDatasource(
      @Qualifier("datasourceProperties") DataSourceProperties dataSourceProperties) {
    return dataSourceProperties.initializeDataSourceBuilder().build();
  }

  @Primary
  @Bean(name = "entityManagerFactory")
  public LocalContainerEntityManagerFactoryBean jobRunrEntityManagerFactory(
      EntityManagerFactoryBuilder builder,
      @Qualifier("datasource") DataSource dataSource
  ) {
    return builder.dataSource(dataSource)
        .packages("life.qbic")
        .persistenceUnit("default")
        .build();
  }

  @Primary
  @Bean(name = "transactionManager")
  @ConfigurationProperties("spring.jpa")
  public PlatformTransactionManager transactionManager(
      @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory);
  }
}
