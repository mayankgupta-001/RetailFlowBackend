package com.creator.RetailFlow.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FlywayMigrationConfiguration {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .validateOnMigrate(true)
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor flywayBeforeJpa() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                return;
            }

            BeanDefinition entityManagerFactory =
                    beanFactory.getBeanDefinition("entityManagerFactory");
            String[] existingDependencies = entityManagerFactory.getDependsOn();
            String[] dependencies = new String[(existingDependencies == null ? 0 : existingDependencies.length) + 1];

            if (existingDependencies != null) {
                System.arraycopy(existingDependencies, 0, dependencies, 0, existingDependencies.length);
            }
            dependencies[dependencies.length - 1] = "flyway";
            entityManagerFactory.setDependsOn(dependencies);
        };
    }
}
