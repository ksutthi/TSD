package com.tsd.platform.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
class MultiDataSourceConfig {

    // ==========================================
    // 1️⃣ PRIMARY DB CONFIG (Registry)
    // ==========================================
    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(
        // 👇 Scans BOTH App (for your logic) and Platform (for SuspendedAction)
        basePackages = ["com.tsd.app", "com.tsd.platform"],
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
    )
    class PrimaryConfig {

        @Primary
        @Bean
        @ConfigurationProperties("spring.datasource")
        fun primaryDataSourceProperties(): DataSourceProperties = DataSourceProperties()

        // 👇 CRITICAL FIX: We give this TWO names.
        //    1. "primaryDataSource" (for us)
        //    2. "dataSource" (for Spring Batch, so it stops complaining)
        @Primary
        @Bean(name = ["dataSource", "primaryDataSource"])
        fun primaryDataSource(): DataSource = primaryDataSourceProperties().initializeDataSourceBuilder().build()

        @Primary
        @Bean
        fun entityManagerFactory(
            builder: EntityManagerFactoryBuilder,
            @Qualifier("primaryDataSource") dataSource: DataSource
        ): LocalContainerEntityManagerFactoryBean {
            return builder
                .dataSource(dataSource)
                // 👇 Scans ALL locations for Entities (Registry, App, Persistence)
                .packages("com.tsd.platform.model.registry", "com.tsd.app", "com.tsd.platform.persistence")
                .persistenceUnit("tsdRegistry")
                .build()
        }

        @Primary
        @Bean
        fun transactionManager(
            @Qualifier("entityManagerFactory") entityManagerFactory: jakarta.persistence.EntityManagerFactory
        ): PlatformTransactionManager {
            return JpaTransactionManager(entityManagerFactory)
        }
    }

    // ==========================================
    // 2️⃣ SECONDARY DB CONFIG (OneID)
    // ==========================================
    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(
        basePackages = ["com.tsd.platform.model.oneid"],
        entityManagerFactoryRef = "oneIdEntityManagerFactory",
        transactionManagerRef = "oneIdTransactionManager"
    )
    class OneIdConfig {

        @Bean
        @ConfigurationProperties("spring.datasource.oneid")
        fun oneIdDataSourceProperties(): DataSourceProperties = DataSourceProperties()

        @Bean
        fun oneIdDataSource(): DataSource = oneIdDataSourceProperties().initializeDataSourceBuilder().build()

        @Bean
        fun oneIdEntityManagerFactory(
            builder: EntityManagerFactoryBuilder,
            @Qualifier("oneIdDataSource") dataSource: DataSource
        ): LocalContainerEntityManagerFactoryBean {
            return builder
                .dataSource(dataSource)
                .packages("com.tsd.platform.model.oneid")
                .persistenceUnit("setOneId")
                .build()
        }

        @Bean
        fun oneIdTransactionManager(
            @Qualifier("oneIdEntityManagerFactory") oneIdEntityManagerFactory: jakarta.persistence.EntityManagerFactory
        ): PlatformTransactionManager {
            return JpaTransactionManager(oneIdEntityManagerFactory)
        }
    }
}