package life.qbic.datamanager.configuration;


import javax.sql.DataSource;
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

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
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
