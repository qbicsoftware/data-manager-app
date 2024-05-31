package life.qbic.datamanager.configuration;


import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
 * Configures the datasource handling entities in package {@link life.qbic.projectmanagement}.
 * <p>
 * The DataSource configured by this class is called {@code datasource} and is injectable through
 * {@code @Qualifier("projectManagementDataSource")}.
 * <p>
 * Further the configuration registeres a {@link EntityManagerFactory} looking for entities from
 * {@link life.qbic.projectmanagement} and a
 * {@link org.springframework.transaction.TransactionManager}.
 *
 * <p>
 * It is injectable through {@code @Qualifier("projectManagementDataSource")}
 *
 * @see <a
 * href="https://docs.spring.io/spring-boot/how-to/data-access.html#howto.data-access.configure-two-datasources">Spring
 * Boot multiple datasources</a>
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = {"life.qbic.projectmanagement"},
    entityManagerFactoryRef = "projectManagementEntityManagerFactory",
    transactionManagerRef = "projectManagementTransactionManager")
public class ProjectManagementDatasourceConfig {

  @Primary
  @Bean(name = "projectManagementDataSourceProperties")
  @ConfigurationProperties("qbic.project-management.datasource")
  public DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @Primary
  @Bean(name = "projectManagementDataSource")
  public DataSource dataSource() {
    return dataSourceProperties()
        .initializeDataSourceBuilder()
        .type(HikariDataSource.class)
        .build();
  }

  @Primary
  @Bean(name = "projectManagementEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      EntityManagerFactoryBuilder builder,
      @Qualifier("projectManagementDataSource") DataSource dataSource) {
    return builder
        .dataSource(dataSource)
        .packages("life.qbic.projectmanagement")
        .build();
  }

  @Primary
  @Bean(name = "projectManagementTransactionManager")
  public PlatformTransactionManager transactionManager(
      @Qualifier("projectManagementEntityManagerFactory") LocalContainerEntityManagerFactoryBean factoryBean) {
    return new JpaTransactionManager(factoryBean.getObject());
  }
}
