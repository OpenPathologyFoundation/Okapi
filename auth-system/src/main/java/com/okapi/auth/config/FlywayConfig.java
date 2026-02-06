package com.okapi.auth.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean(name = "flywayMigrate")
    public InitializingBean flywayMigrate(Flyway flyway) {
        return flyway::migrate;
    }

    @Bean
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor dependsOnFlywayMigration() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                    return;
                }
                BeanDefinition definition = beanFactory.getBeanDefinition("entityManagerFactory");
                String[] existing = definition.getDependsOn();
                if (existing == null) {
                    definition.setDependsOn(new String[] { "flywayMigrate" });
                    return;
                }
                for (String dependency : existing) {
                    if ("flywayMigrate".equals(dependency)) {
                        return;
                    }
                }
                String[] updated = java.util.Arrays.copyOf(existing, existing.length + 1);
                updated[updated.length - 1] = "flywayMigrate";
                definition.setDependsOn(updated);
            }
        };
    }
}
