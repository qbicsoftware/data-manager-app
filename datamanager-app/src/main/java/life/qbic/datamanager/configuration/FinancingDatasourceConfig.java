package life.qbic.datamanager.configuration;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
 * Configures the datasource handling entities in package {@link life.qbic.finance}.
 * <p>
 * The DataSource configured by this class is called {@code datasource} and is injectable through
 * {@code @Qualifier("financeDataSource")}.
 * <p>
 * Further the configuration registeres a {@link EntityManagerFactory} looking for entities from
 * {@link life.qbic.finance} and a {@link org.springframework.transaction.TransactionManager}.
 *
 * <p>
 * It is injectable through {@code @Qualifier("financeDataSource")}
 *
 * @see <a
 * href="https://docs.spring.io/spring-boot/how-to/data-access.html#howto.data-access.configure-two-datasources">Spring
 * Boot multiple datasources</a>
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = {"life.qbic.finance"},
    entityManagerFactoryRef = "financeEntityManagerFactory",
    transactionManagerRef = "financeTransactionManager")
public class FinancingDatasourceConfig {

  @Value("${qbic.finance.datasource.ddl-auto}")
  String hibernateDdlAuto;

  @Bean(name = "financeDataSourceProperties")
  @ConfigurationProperties("qbic.finance.datasource")
  public DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean(name = "financeDataSource")
  public DataSource dataSource() {
    return dataSourceProperties()
        .initializeDataSourceBuilder()
        .type(BasicDataSource.class)
        .build();
  }

  @Bean(name = "financeEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      EntityManagerFactoryBuilder builder,
      @Qualifier("financeDataSource") DataSource dataSource) {
    return builder
        .dataSource(dataSource)
        .packages("life.qbic.finance")
        .properties(Map.of(
            "hibernate.hbm2ddl.auto", hibernateDdlAuto
        ))
        .build();
  }

  @Bean(name = "financeTransactionManager")
  public PlatformTransactionManager transactionManager(
      @Qualifier("financeEntityManagerFactory") LocalContainerEntityManagerFactoryBean factoryBean) {
    Objects.requireNonNull(factoryBean);
    var factory = Objects.requireNonNull(factoryBean.getObject());
    return new JpaTransactionManager(factory);
  }
}
