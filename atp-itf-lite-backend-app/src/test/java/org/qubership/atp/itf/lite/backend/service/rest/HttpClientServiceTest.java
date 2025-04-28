package org.qubership.atp.itf.lite.backend.service.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.UUID;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.feign.dto.CertificateDto;
import org.qubership.atp.itf.lite.backend.configuration.HttpClientProperties;
import org.qubership.atp.itf.lite.backend.exceptions.internal.ItfLiteSslCertificateVerificationFileException;
import org.qubership.atp.itf.lite.backend.exceptions.internal.ItfLiteSslClientVerificationFileException;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.RequestRuntimeOptions;
import org.qubership.atp.itf.lite.backend.service.CertificateService;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;

@ExtendWith(MockitoExtension.class)
public class HttpClientServiceTest {

    private final ThreadLocal<HttpClientProperties> httpClientProperties = new ThreadLocal<>();
    private final ThreadLocal<CertificateService> certificateService = new ThreadLocal<>();
    private final ThreadLocal<EncryptionService> encryptionService = new ThreadLocal<>();
    private final ThreadLocal<HttpClientService> httpClientService = new ThreadLocal<>();

    @BeforeEach
    public void setUp() {
        HttpClientProperties httpClientPropertiesMock = mock(HttpClientProperties.class);
        CertificateService certificateServiceMock = mock(CertificateService.class);
        EncryptionService encryptionServiceMock = mock(EncryptionService.class);
        httpClientProperties.set(httpClientPropertiesMock);
        certificateService.set(certificateServiceMock);
        encryptionService.set(encryptionServiceMock);
        httpClientService.set(new HttpClientService(httpClientPropertiesMock, certificateServiceMock, encryptionServiceMock));
    }

    @Test
    public void getHttpClient_whenCertVerificationEnabled() throws AtpDecryptException {
        //given
        final UUID projectId = UUID.randomUUID();
        CertificateDto cert = EntitiesGenerator.generateRandomCertificate();
        cert.setEnableCertificateVerification(true);
        cert.setEnableClientCertificate(false);

        //when
        when(httpClientProperties.get().getMaxTotalConnections()).thenReturn(2);
        when(encryptionService.get().decryptIfEncrypted(any())).thenReturn("password");
        when(certificateService.get().getCertificate(any())).thenReturn(cert);
        when(certificateService.get().getCertificateVerificationFile(any()))
                .thenReturn(new File("./src/test/resources/identity.p12"));
        CloseableHttpClient result  = httpClientService.get().getHttpClient(projectId);

        //then
        assertNotNull(result);
    }

    @Test
    public void getHttpClient_whenClientCertVerificationEnabled() throws AtpDecryptException {
        //given
        final UUID projectId = UUID.randomUUID();
        CertificateDto cert = EntitiesGenerator.generateRandomCertificate();
        cert.setEnableClientCertificate(true);
        cert.setEnableCertificateVerification(false);

        //when
        when(httpClientProperties.get().getMaxTotalConnections()).thenReturn(2);
        when(encryptionService.get().decryptIfEncrypted(any())).thenReturn("password");
        when(certificateService.get().getCertificate(any())).thenReturn(cert);
        when(certificateService.get().getClientCertificateFile(any()))
                .thenReturn(new File("./src/test/resources/identity.p12"));
        CloseableHttpClient result  = httpClientService.get().getHttpClient(projectId);

        //then
        assertNotNull(result);
    }

    @Test
    public void getHttpClient_whenFollowingRedirectDisabled() throws AtpDecryptException {
        //given
        final UUID projectId = UUID.randomUUID();
        CertificateDto cert = EntitiesGenerator.generateRandomCertificate();
        cert.setEnableClientCertificate(true);
        cert.setEnableCertificateVerification(false);

        //when
        when(httpClientProperties.get().getMaxTotalConnections()).thenReturn(2);
        when(encryptionService.get().decryptIfEncrypted(any())).thenReturn("password");
        when(certificateService.get().getCertificate(any())).thenReturn(cert);
        when(certificateService.get().getClientCertificateFile(any()))
                .thenReturn(new File("./src/test/resources/identity.p12"));

        RequestRuntimeOptions runtimeOptions = new RequestRuntimeOptions();
        runtimeOptions.setDisableFollowingRedirect(true);
        CloseableHttpClient result  = httpClientService.get().getHttpClient(projectId, runtimeOptions);

        //then
        assertNotNull(result);
        //assertFalse(((InternalHttpClient)result).getConfig().isRedirectsEnabled());
    }

    @Test
    public void getHttpClient_whenSslCertificateVerificationDisabled() throws AtpDecryptException {
        //given
        final UUID projectId = UUID.randomUUID();
        CertificateDto cert = EntitiesGenerator.generateRandomCertificate();
        cert.setEnableClientCertificate(true);
        cert.setEnableCertificateVerification(true);

        //when
        when(httpClientProperties.get().getMaxTotalConnections()).thenReturn(2);
        when(encryptionService.get().decryptIfEncrypted(any())).thenReturn("password");
        when(certificateService.get().getCertificate(any())).thenReturn(cert);
        when(certificateService.get().getClientCertificateFile(any()))
                .thenReturn(new File("./src/test/resources/identity.p12"));

        RequestRuntimeOptions runtimeOptions = new RequestRuntimeOptions();
        runtimeOptions.setDisableSslCertificateVerification(true);
        CloseableHttpClient result  = httpClientService.get().getHttpClient(projectId, runtimeOptions);

        //then
        assertNotNull(result);
    }

    @Test
    public void getHttpClient_whenSslClientCertificateDisabled() {
        //given
        final UUID projectId = UUID.randomUUID();
        CertificateDto cert = EntitiesGenerator.generateRandomCertificate();
        cert.setEnableClientCertificate(true);
        cert.setEnableCertificateVerification(false);

        //when
        when(httpClientProperties.get().getMaxTotalConnections()).thenReturn(2);
        when(certificateService.get().getCertificate(any())).thenReturn(cert);

        RequestRuntimeOptions runtimeOptions = new RequestRuntimeOptions();
        runtimeOptions.setDisableSslClientCertificate(true);
        CloseableHttpClient result  = httpClientService.get().getHttpClient(projectId, runtimeOptions);

        //then
        assertNotNull(result);
    }

    @Test
    public void whenNoCertFile_returnError() throws AtpDecryptException {
        //given
        final UUID projectId = UUID.randomUUID();
        CertificateDto cert = EntitiesGenerator.generateRandomCertificate();
        cert.setEnableCertificateVerification(true);
        cert.setEnableClientCertificate(false);

        //when
        when(encryptionService.get().decryptIfEncrypted(any())).thenReturn("password");
        when(certificateService.get().getCertificate(any())).thenReturn(cert);
        ItfLiteSslCertificateVerificationFileException exception = assertThrows(
                ItfLiteSslCertificateVerificationFileException.class,
                () -> httpClientService.get().getHttpClient(projectId));

        //then
        String expectedErrorMessage = ItfLiteSslCertificateVerificationFileException.DEFAULT_MESSAGE;
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    @Test
    public void whenNoClientCertFile_returnError() throws AtpDecryptException {
        //given
        final UUID projectId = UUID.randomUUID();
        CertificateDto cert = EntitiesGenerator.generateRandomCertificate();
        cert.setEnableCertificateVerification(false);
        cert.setEnableClientCertificate(true);

        //when
        when(encryptionService.get().decryptIfEncrypted(any())).thenReturn("password");
        when(certificateService.get().getCertificate(any())).thenReturn(cert);
        ItfLiteSslClientVerificationFileException exception = assertThrows(
                ItfLiteSslClientVerificationFileException.class,
                () -> httpClientService.get().getHttpClient(projectId));

        //then
        String expectedErrorMessage = ItfLiteSslClientVerificationFileException.DEFAULT_MESSAGE;
        assertEquals(expectedErrorMessage, exception.getMessage());
    }
}
