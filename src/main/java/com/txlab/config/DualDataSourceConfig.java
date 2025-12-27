package com.txlab.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
@Profile("dual-mysql")
@EnableTransactionManagement
public class DualDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("app.datasource.core")
    public DataSourceProperties coreDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource coreDataSource() {
        return coreDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean coreEntityManagerFactory(EntityManagerFactoryBuilder builder,
                                                                           @Qualifier("coreDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.txlab.core", "com.txlab.billing")
                .persistenceUnit("core")
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager coreTransactionManager(@Qualifier("coreEntityManagerFactory") LocalContainerEntityManagerFactoryBean factoryBean) {
        return new JpaTransactionManager(factoryBean.getObject());
    }

    @Bean(initMethod = "migrate")
    public Flyway coreFlyway(@Qualifier("coreDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/core")
                .schemas("txlab_core")
                .baselineOnMigrate(true)
                .load();
    }

    @Bean
    @ConfigurationProperties("app.datasource.audit")
    public DataSourceProperties auditDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource auditDataSource() {
        return auditDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean auditEntityManagerFactory(EntityManagerFactoryBuilder builder,
                                                                            @Qualifier("auditDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.txlab.audit")
                .persistenceUnit("audit")
                .build();
    }

    @Bean
    public PlatformTransactionManager auditTransactionManager(@Qualifier("auditEntityManagerFactory") LocalContainerEntityManagerFactoryBean factoryBean) {
        return new JpaTransactionManager(factoryBean.getObject());
    }

    @Bean(initMethod = "migrate")
    public Flyway auditFlyway(@Qualifier("auditDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/audit")
                .schemas("txlab_audit")
                .baselineOnMigrate(true)
                .load();
    }

    @Bean
    @Primary
    public TransactionTemplate coreTransactionTemplate(@Qualifier("coreTransactionManager") PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    public TransactionTemplate auditTransactionTemplate(@Qualifier("auditTransactionManager") PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Configuration
    @Profile("dual-mysql")
    @EnableJpaRepositories(
            basePackages = {"com.txlab.core", "com.txlab.billing"},
            entityManagerFactoryRef = "coreEntityManagerFactory",
            transactionManagerRef = "coreTransactionManager"
    )
    static class CoreJpaConfig {
    }

    @Configuration
    @Profile("dual-mysql")
    @EnableJpaRepositories(
            basePackages = "com.txlab.audit",
            entityManagerFactoryRef = "auditEntityManagerFactory",
            transactionManagerRef = "auditTransactionManager"
    )
    static class AuditJpaConfig {
    }
}
