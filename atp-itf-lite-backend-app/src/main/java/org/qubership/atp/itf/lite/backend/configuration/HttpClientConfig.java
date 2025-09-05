package org.qubership.atp.itf.lite.backend.configuration;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {

    /**
     * Creates and configures a {@link PoolingHttpClientConnectionManager} bean
     * with maximum total connections and per-route limits.
     *
     * @param properties the configuration properties for the HTTP client,
     *                   injected with a specific qualifier to resolve ambiguity
     *                   in case multiple {@link HttpClientProperties} beans exist.
     * @return a fully configured instance of {@link PoolingHttpClientConnectionManager}
     */
    @Bean
    public PoolingHttpClientConnectionManager defaultConnectionManager(
            @Qualifier("atp.itf.lite-org.qubership.atp.itf.lite.backend.configuration.HttpClientProperties")
            HttpClientProperties properties) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(properties.getMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(20);
        return connectionManager;
    }
}
