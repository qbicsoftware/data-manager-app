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
    entityManagerFactoryRef = "projectManagementEntityManagerFactory",
    transactionManagerRef = "projectManagementTransactionManager")
public class ProjectManagementDatasourceConfig {

  @Bean(name = "projectManagementDataSourceProperties")
  @ConfigurationProperties("qbic.project-management.datasource")
  public DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean(name = "projectManagementDataSource")
  public DataSource dataSource() {
    return dataSourceProperties()
        .initializeDataSourceBuilder()
        .build();
  }

  @Bean(name = "projectManagementEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      EntityManagerFactoryBuilder builder,
      @Qualifier("projectManagementDataSource") DataSource dataSource) {
    return builder
        .dataSource(dataSource)
        .packages("life.qbic.projectmanagement")
        .build();
  }

  @Bean(name = "projectManagementTransactionManager")
  public PlatformTransactionManager transactionManager(
      @Qualifier("projectManagementEntityManagerFactory") LocalContainerEntityManagerFactoryBean factoryBean) {
    return new JpaTransactionManager(factoryBean.getObject());
  }
}
