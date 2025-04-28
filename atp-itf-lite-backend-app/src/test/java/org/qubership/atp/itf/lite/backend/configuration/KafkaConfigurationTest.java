package org.qubership.atp.itf.lite.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaAdmin;

class KafkaConfigurationTest {

    @Test
    public void should_check_presence_of_KafkaAdmin() {
        ApplicationContextRunner context = new ApplicationContextRunner()
                .withUserConfiguration(KafkaAdmin.class);
        context.run(t -> {
            assertThat(t).hasSingleBean(KafkaAdmin.class);
        });
    }
}