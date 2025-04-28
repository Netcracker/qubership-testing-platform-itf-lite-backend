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

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;

@Configuration
@RequiredArgsConstructor
public class LockManagerConfiguration {

    private final DataSource dataSource;

    /**
     * Bean for lock manager.
     * Changing provider to keep lock records in database
     * @return LockProvider
     */
    @Bean
    @Primary
    public JdbcTemplateLockProvider lockProvider() {
        JdbcTemplateLockProvider.Configuration configuration = JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build();
        return new JdbcTemplateLockProvider(configuration);
    }
}
