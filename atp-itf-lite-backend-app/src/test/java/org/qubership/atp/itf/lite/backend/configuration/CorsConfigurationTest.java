package org.qubership.atp.itf.lite.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

class CorsConfigurationTest {
    @Test
    public void should_check_presence_of_FilterRegistrationBean() {
        ApplicationContextRunner context = new ApplicationContextRunner()
                .withUserConfiguration(FilterRegistrationBean.class);
        context.run(t -> {
            assertThat(t).hasSingleBean(FilterRegistrationBean.class);
        });
    }

}