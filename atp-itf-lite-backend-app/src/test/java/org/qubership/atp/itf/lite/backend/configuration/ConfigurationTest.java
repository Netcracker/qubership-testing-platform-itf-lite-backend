package org.qubership.atp.itf.lite.backend.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(
        classes = {SpringLiquibaseConfiguration.class, CorsConfiguration.class},
        loader = AnnotationConfigContextLoader.class
)
@EnableConfigurationProperties(value = HttpClientProperties.class)
@TestPropertySource("classpath:application.properties")
public class ConfigurationTest {

    @Autowired
    SpringLiquibaseConfiguration springLiquibaseConfiguration;

    @Autowired
    CorsConfiguration corsConfiguration;

    @MockBean
    DataSource dataSource;

    @Test
    public void beansCreation_shouldBeCreated() {
        assertNotNull(springLiquibaseConfiguration);
        assertNotNull(corsConfiguration);
    }
}
