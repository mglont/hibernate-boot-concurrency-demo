package random

import javax.sql.DataSource
import javax.persistence.EntityManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@ComponentScan(basePackages = ["random", "random.domain", "random.service"])
@EnableJpaRepositories(["random.service", "random.domain"])
@EnableTransactionManagement
class AppConfig {
    @Bean
    public DataSource dataSource() {
        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
        builder.setType(EmbeddedDatabaseType.H2).build()
    }

    @Bean
    public EntityManagerFactory entityManagerFactory() {
        def vendorAdapter = new HibernateJpaVendorAdapter()
        vendorAdapter.generateDdl = true

        def emfFactory = new LocalContainerEntityManagerFactoryBean()
        emfFactory.jpaVendorAdapter = vendorAdapter
        emfFactory.packagesToScan = "random"
        emfFactory.dataSource = dataSource()
        emfFactory.afterPropertiesSet()
        emfFactory.getObject()
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        def txManager = new JpaTransactionManager()
        txManager.entityManagerFactory = entityManagerFactory()
        txManager
    }
}
