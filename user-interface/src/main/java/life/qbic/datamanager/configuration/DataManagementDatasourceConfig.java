package life.qbic.datamanager.configuration;


import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Objects;
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
 * Configures the datasource handling entities in package {@link life.qbic.projectmanagement} and
 * {@link life.qbic.identity}.
 * <p>
 * The DataSource configured by this class is called {@code datasource} and is injectable through
 * {@code @Qualifier("dataManagementDataSource")}.
 * <p>
 * Further the configuration registeres a {@link EntityManagerFactory} looking for entities from
 * {@link life.qbic.projectmanagement} and a
 * {@link org.springframework.transaction.TransactionManager}.
 *
 * <p>
 * It is injectable through {@code @Qualifier("dataManagementDataSource")}
 *
 * @see <a
 * href="https://docs.spring.io/spring-boot/how-to/data-access.html#howto.data-access.configure-two-datasources">Spring
 * Boot multiple datasources</a>
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = {"life.qbic.projectmanagement", "life.qbic.identity"},
    entityManagerFactoryRef = "dataManagementEntityManagerFactory",
    transactionManagerRef = "dataManagementTransactionManager")
public class DataManagementDatasourceConfig {

  @Value("${qbic.data-management.datasource.ddl-auto}")
  String hibernateDdlAuto;

  @Primary
  @Bean(name = "dataManagementDataSourceProperties")
  @ConfigurationProperties("qbic.data-management.datasource")
  public DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @Primary
  @Bean(name = "dataManagementDataSource")
  public DataSource dataSource() {
    return dataSourceProperties()
        .initializeDataSourceBuilder()
        .type(HikariDataSource.class)
        .build();
  }

  @Primary
  @Bean(name = "dataManagementEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      EntityManagerFactoryBuilder builder,
      @Qualifier("dataManagementDataSource") DataSource dataSource) {
    return builder
        .dataSource(dataSource)
        .packages("life.qbic.projectmanagement", "life.qbic.identity")
        .properties(Map.of(
            "hibernate.hbm2ddl.auto", hibernateDdlAuto
        ))
        .build();
  }

  @Primary
  @Bean(name = "dataManagementTransactionManager")
  public PlatformTransactionManager transactionManager(
      @Qualifier("dataManagementEntityManagerFactory") LocalContainerEntityManagerFactoryBean factoryBean) {
    Objects.requireNonNull(factoryBean);
    var factory = Objects.requireNonNull(factoryBean.getObject());
    return new JpaTransactionManager(factory);
  }
}
