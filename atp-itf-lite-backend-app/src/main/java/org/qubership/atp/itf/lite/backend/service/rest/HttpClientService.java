/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.atp.itf.lite.backend.service.rest;

import static java.util.Objects.nonNull;

import java.net.HttpCookie;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.HeaderElementIterator;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.message.BasicHeaderElementIterator;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpException;
import org.qubership.atp.itf.lite.backend.configuration.HttpClientProperties;
import org.qubership.atp.itf.lite.backend.exceptions.internal.ItfLiteSslCertificateVerificationFileException;
import org.qubership.atp.itf.lite.backend.exceptions.internal.ItfLiteSslClientVerificationFileException;
import org.qubership.atp.itf.lite.backend.feign.dto.CertificateDto;
import org.qubership.atp.itf.lite.backend.model.RequestRuntimeOptions;
import org.qubership.atp.itf.lite.backend.service.CertificateService;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableConfigurationProperties(HttpClientProperties.class)
@AllArgsConstructor
@Configuration
@Slf4j
public class HttpClientService {

    private final HttpClientProperties httpClientProperties;
    private final CertificateService certificateService;
    private final EncryptionService encryptionService;

    /**
     * Configuration for http client.
     *
     * @return http client
     */
    public CloseableHttpClient getHttpClient(UUID projectId) {
        return getHttpClient(projectId, new RequestRuntimeOptions(), StringUtils.EMPTY, null);
    }

    /**
     * Configuration for http client.
     *
     * @return http client
     */
    public CloseableHttpClient getHttpClient(UUID projectId, RequestRuntimeOptions runtimeOptions) {
        return getHttpClient(projectId, runtimeOptions, StringUtils.EMPTY, null);
    }

    /**
     * Configuration for http client.
     *
     * @return http client
     */
    public CloseableHttpClient getHttpClient(UUID projectId,
                                             RequestRuntimeOptions runtimeOptions,
                                             String requestUrl,
                                             CookieStore cookieStore) {
        RequestConfig.Builder configBuilder = RequestConfig.custom()
                .setConnectionRequestTimeout(httpClientProperties.getRequestTimeout(), TimeUnit.MILLISECONDS)
                .setConnectTimeout(httpClientProperties.getConnectionTimeout(), TimeUnit.MILLISECONDS)
                .setResponseTimeout(httpClientProperties.getSocketTimeout(), TimeUnit.MILLISECONDS);
        if (runtimeOptions.isDisableFollowingRedirect()) {
            configBuilder.setRedirectsEnabled(false);
        }
        RequestConfig requestConfig = configBuilder.build();

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(poolingConnectionManager(projectId, runtimeOptions, requestUrl))
                .setKeepAliveStrategy(connectionKeepAliveStrategy());
        if (runtimeOptions.isDisableFollowingRedirect()) {
            httpClientBuilder.disableRedirectHandling();
        }
        if (nonNull(cookieStore)) {
            httpClientBuilder.setDefaultCookieStore(cookieStore);
        }
        return httpClientBuilder.build();
    }

    private PoolingHttpClientConnectionManager poolingConnectionManager(UUID projectId) {
        return poolingConnectionManager(projectId, new RequestRuntimeOptions(), StringUtils.EMPTY);
    }

    private PoolingHttpClientConnectionManager poolingConnectionManager(UUID projectId,
                                                                        RequestRuntimeOptions runtimeOptions) {
        return poolingConnectionManager(projectId, runtimeOptions, StringUtils.EMPTY);
    }

    /**
     * Configuration for pooling connection manager.
     *
     * @return configured poolingConnectionManager
     */
    private PoolingHttpClientConnectionManager poolingConnectionManager(UUID projectId,
                                                                        RequestRuntimeOptions runtimeOptions,
                                                                        String requestUrl) {
        boolean enableSslCertificateVerification = !runtimeOptions.isDisableSslCertificateVerification();
        boolean enableSslClientCertificate = !runtimeOptions.isDisableSslClientCertificate();
        SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
        if (enableSslCertificateVerification || enableSslClientCertificate) {
            CertificateDto cert = certificateService.getCertificate(projectId);
            enableSslCertificateVerification = !runtimeOptions.isDisableSslCertificateVerification()
                    && BooleanUtils.toBoolean(cert.getEnableCertificateVerification());
            enableSslClientCertificate = !runtimeOptions.isDisableSslClientCertificate()
                    && BooleanUtils.toBoolean(cert.getEnableClientCertificate());

            if (enableSslCertificateVerification || enableSslClientCertificate) {
                /* If requestUrl is empty - I think, it should be permissible here - no domain filtering is performed.
                    Why should it be permissible?
                    Because there could be use case when the only HttpClient is used to execute a series of requests,
                    for performance considerations, for example.
                    In that case, request details are determined later than HttpClient is initialized.
                */
                if (StringUtils.isNotEmpty(requestUrl)) {
                    String host = UriComponentsBuilder.fromHttpUrl(requestUrl).build().getHost();
                    if (enableSslCertificateVerification && !CollectionUtils.isEmpty(cert.getTrustStoreDomainNames())
                            && !matchesAnyOfDomainsArray(cert.getTrustStoreDomainNames(), host)) {
                        enableSslCertificateVerification = false;
                    }
                    if (enableSslClientCertificate && !CollectionUtils.isEmpty(cert.getKeyStoreDomainNames())
                            && !matchesAnyOfDomainsArray(cert.getKeyStoreDomainNames(), host)) {
                        enableSslClientCertificate = false;
                    }
                }
            }
            if (enableSslCertificateVerification) {
                try {
                    sslContextBuilder.setProtocol(cert.getProtocol());
                    char[] pass = encryptionService.decryptIfEncrypted(cert.getTrustStorePassphrase()).toCharArray();
                    sslContextBuilder.loadTrustMaterial(
                            certificateService.getCertificateVerificationFile(projectId), pass);
                } catch (AtpException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("Failed to apply SSL verification file for project '{}'", projectId, e);
                    throw new ItfLiteSslCertificateVerificationFileException();
                }
            }
            if (enableSslClientCertificate) {
                try {
                    char[] pass = encryptionService.decryptIfEncrypted(cert.getKeyStorePassphrase()).toCharArray();
                    sslContextBuilder.loadKeyMaterial(
                            certificateService.getClientCertificateFile(projectId), pass, pass);
                } catch (AtpException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("Failed to apply SSL client certificate file for project '{}'", projectId, e);
                    throw new ItfLiteSslClientVerificationFileException();
                }
            }
        }
        if (!enableSslCertificateVerification && !enableSslClientCertificate) {
            try {
                //ALLOW ALL
                sslContextBuilder.loadTrustMaterial(null, new TrustAllStrategy());
            } catch (NoSuchAlgorithmException | KeyStoreException e) {
                log.error("Pooling Connection Manager Initialisation failure because of " + e.getMessage(), e);
            }
        }

        SSLConnectionSocketFactory sslConnectionSocketFactory = null;
        try {
            sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build(),
                    NoopHostnameVerifier.INSTANCE);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            log.error("Pooling Connection Manager Initialisation failure because of " + e.getMessage(), e);
        }

        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder
                .<ConnectionSocketFactory>create()
                .register("http", new PlainConnectionSocketFactory());
        if (sslConnectionSocketFactory != null) {
            registryBuilder.register("https", sslConnectionSocketFactory);
        }

        Registry<ConnectionSocketFactory> socketFactoryRegistry = registryBuilder.build();

        PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager(
                socketFactoryRegistry);
        poolingConnectionManager.setMaxTotal(httpClientProperties.getMaxTotalConnections());
        return poolingConnectionManager;
    }

    private boolean matchesAnyOfDomainsArray(List<String> domains, String host) {
        return domains.stream().anyMatch(domain -> HttpCookie.domainMatches(domain, host));
    }

    /**
     * Configuration for keep alive strategy.
     *
     * @return configured connectionKeepAliveStrategy
     */
    private ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
        return new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(ClassicHttpResponse response, HttpContext context) {
                HeaderElementIterator responseHeaderIterator = new BasicHeaderElementIterator(
                        response.headerIterator(HttpHeaders.KEEP_ALIVE));
                while (responseHeaderIterator.hasNext()) {
                    HeaderElement headerElement = responseHeaderIterator.nextElement();
                    String param = headerElement.getName();
                    String value = headerElement.getValue();

                    if (value != null && param.equalsIgnoreCase("timeout")) {
                        return Long.parseLong(value) * 1000;
                    }
                }
                return httpClientProperties.getDefaultKeepAliveTimeMillis();
            }
        };
    }

    /**
     * Monitor to close expired connections.
     *
     * @param connectionManager connection manager
     * @return runnable
     */
    @Bean
    public Runnable idleConnectionMonitor(final PoolingHttpClientConnectionManager connectionManager) {
        return new Runnable() {
            @Override
            @Scheduled(fixedDelay = 10000)
            public void run() {
                try {
                    if (connectionManager != null) {
                        log.trace("run IdleConnectionMonitor - Closing expired and idle connections...");
                        connectionManager.closeExpiredConnections();
                        connectionManager.closeIdleConnections(
                                httpClientProperties.getCloseIdleConnectionWaitTimeSecs(), TimeUnit.SECONDS);
                    } else {
                        log.trace("run IdleConnectionMonitor - Http Client Connection manager is not initialised");
                    }
                } catch (Exception e) {
                    log.error("run IdleConnectionMonitor - Exception occurred. msg={}", e.getMessage(), e);
                }
            }
        };
    }
}
