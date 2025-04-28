package org.qubership.atp.itf.lite.backend.configuration;

import org.qubership.atp.itf.lite.backend.components.auth.RequestAuthorizationRegistry;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.qubership.atp.itf.lite.backend.service.RequestAuthorizationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.qubership.atp.itf.lite.backend.components.auth")
public class RequestAuthorizationTestConfiguration {

    @Bean
    public RequestAuthorizationService requestAuthorizationService(RequestAuthorizationRegistry registry) {
        return new RequestAuthorizationService(registry);
    }

    @Bean
    public EncryptionService encryptionService() {
        return new EncryptionService();
    }
}
