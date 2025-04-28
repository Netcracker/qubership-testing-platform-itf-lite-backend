/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.itf.lite.backend.configuration;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
@EnableTransactionManagement
public class HibernateConnectorConfiguration {

    public static final String BASE_PACKAGE = "org.qubership.atp.itf.lite.backend";

    private final DataSource dataSource;

    /**
     * Hibernate {@link Properties}.
     */
    @Bean
    public Properties hibernateProperties() {
        Properties hibernateProp = new Properties();
        hibernateProp.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL9Dialect");
        hibernateProp.put("hibernate.hbm2ddl.auto", "none");
        hibernateProp.put("hibernate.connection.CharSet", "utf8");
        hibernateProp.put("hibernate.connection.characterEncoding", "utf8");
        hibernateProp.put("hibernate.connection.useUnicode", "true");
        hibernateProp.put("hibernate.generate_statistics", "true");
        hibernateProp.put("hibernate.session.events.log", "false");
        return hibernateProp;
    }

    /**
     * Create {@link JpaVendorAdapter}.
     */
    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        return new HibernateJpaVendorAdapter();
    }

    /**
     * Create {@link EntityManagerFactory}.
     */
    @Bean
    public EntityManagerFactory entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setPackagesToScan(BASE_PACKAGE);
        factoryBean.setDataSource(dataSource);
        factoryBean.setJpaProperties(hibernateProperties());
        factoryBean.setJpaVendorAdapter(jpaVendorAdapter());
        factoryBean.afterPropertiesSet();
        return factoryBean.getNativeEntityManagerFactory();
    }

    /**
     * Create {@link PlatformTransactionManager}.
     */
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new JpaTransactionManager(entityManagerFactory());
    }
}
