package life.qbic.datamanager.configuration;


import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configures the datasource handling entities in package {@link life.qbic.identity}.
 * <p>
 * The DataSource configured by this class is called {@code datasource} and is injectable through
 * {@code @Qualifier("identityDataSource")}.
 * <p>
 * Further the configuration registeres a {@link EntityManagerFactory} looking for entities from
 * {@link life.qbic.identity} and a {@link org.springframework.transaction.TransactionManager}.
 *
 * <p>
 * It is injectable through {@code @Qualifier("identityDataSource")}
 *
 * @see <a
 * href="https://docs.spring.io/spring-boot/docs/2.1.x/reference/html/howto-data-access.html#howto-two-datasources">Spring
 * Boot multiple datasources</a>
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = {"life.qbic.identity"},
    entityManagerFactoryRef = "identityEntityManagerFactory",
    transactionManagerRef = "identityTransactionManager")
public class IdentityDatasourceConfig {

  @Bean(name = "identityDataSourceProperties")
  @ConfigurationProperties("qbic.identity.datasource")
  public DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean(name = "identityDataSource")
  public DataSource dataSource() {
    return dataSourceProperties()
        .initializeDataSourceBuilder()
        .type(BasicDataSource.class)
        .build();
  }

  @Bean(name = "identityEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      EntityManagerFactoryBuilder builder,
      @Qualifier("identityDataSource") DataSource dataSource) {
    return builder
        .dataSource(dataSource)
        .packages("life.qbic.identity")
        .build();
  }

  @Bean(name = "identityTransactionManager")
  public PlatformTransactionManager transactionManager(
      @Qualifier("identityEntityManagerFactory") LocalContainerEntityManagerFactoryBean factoryBean) {
    return new JpaTransactionManager(factoryBean.getObject());
  }
}
