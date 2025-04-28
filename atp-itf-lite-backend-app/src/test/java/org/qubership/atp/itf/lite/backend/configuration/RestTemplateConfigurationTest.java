package org.qubership.atp.itf.lite.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestTemplate;

class RestTemplateConfigurationTest {

    @Test
    public void should_check_presence_of_RestTemplate() {
        ApplicationContextRunner context = new ApplicationContextRunner()
                .withUserConfiguration(RestTemplate.class);
        context.run(t -> {
            assertThat(t).hasSingleBean(RestTemplate.class);
        });
    }
}