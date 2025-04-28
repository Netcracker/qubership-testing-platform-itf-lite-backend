package org.qubership.atp.itf.lite.backend.components.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.configuration.MapperConfiguration;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyRequest;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyResponse;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.BasicAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BasicRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.qubership.atp.itf.lite.backend.service.TemplateResolverService;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.atp.macros.core.processor.Evaluator;

public class BasicAuthorizationStrategyTest {

    private final ThreadLocal<EncryptionService> encryptionService = new ThreadLocal<>();
    private final ThreadLocal<BasicRequestAuthorizationStrategy> basicStrategy = new ThreadLocal<>();

    private static final String username = "username";
    private static final String password = "password";
    private static final String base64AuthToken = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

    @BeforeEach
    public void setUp() {
        EncryptionService encryptionServiceMock = mock(EncryptionService.class);
        encryptionService.set(encryptionServiceMock);
        basicStrategy.set(new BasicRequestAuthorizationStrategy(encryptionServiceMock));

        TemplateResolverService restTemplateService = mock(TemplateResolverService.class);
        AuthorizationUtils.setObjectMapper(new ObjectMapper());
        AuthorizationUtils.setTemplateResolverService(restTemplateService);
        AuthorizationUtils.setModelMapper(new MapperConfiguration().modelMapper());
    }

    @Test
    void getAuthorizationToken() throws AtpDecryptException {
        // given
        AuthorizationSaveRequest basicAuthRequest = new BasicAuthorizationSaveRequest(username, password);
        Evaluator evaluator = mock(Evaluator.class);
        AuthorizationStrategyRequest request = new AuthorizationStrategyRequest(basicAuthRequest, basicAuthRequest,
                null, UUID.randomUUID(), UUID.randomUUID(), evaluator, null);

        // when
        AuthorizationStrategyResponse response = basicStrategy.get().getAuthorizationToken(request);

        // then
        assertEquals(base64AuthToken, response.getSafeAuthorizationToken());
    }

    @Test
    void getAuthorizationToken_withEncryptedParameters() throws AtpDecryptException {
        // given
        String encryptedParameter = "{ENC}{9s637qdqkLj6xCvA5yXzQw==}{QBvxG0Rfmp+N360gW7HKuA==}";
        AuthorizationSaveRequest basicAuthRequest = new BasicAuthorizationSaveRequest(username, encryptedParameter);
        Evaluator evaluator = mock(Evaluator.class);
        AuthorizationStrategyRequest request = new AuthorizationStrategyRequest(basicAuthRequest, basicAuthRequest,
                null, UUID.randomUUID(), UUID.randomUUID(), evaluator, null);

        // when
        basicStrategy.get().getAuthorizationToken(request);

        // then
        verify(encryptionService.get(), times(1)).decrypt(encryptedParameter);
    }

    @Test
    void getAuthorizationToken_withDecodedParams() throws AtpDecryptException {
        // given
        String base64Password = "{2ENC}" + Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
        AuthorizationSaveRequest basicAuthRequest = new BasicAuthorizationSaveRequest(username, base64Password);
        Evaluator evaluator = mock(Evaluator.class);
        AuthorizationStrategyRequest request = new AuthorizationStrategyRequest(basicAuthRequest, basicAuthRequest,
                null, UUID.randomUUID(), UUID.randomUUID(), evaluator, null);

        // when
        when(encryptionService.get().decodeBase64(any())).thenCallRealMethod();
        AuthorizationStrategyResponse response = basicStrategy.get().getAuthorizationToken(request);

        // then
        assertEquals(base64AuthToken, response.getSafeAuthorizationToken());
    }

    @Test
    public void parseAuthorizationFromMap() {
        Map<String, String> authParams = new HashMap<>();
        authParams.put("username", username);
        authParams.put("password", password);

        RequestAuthorization requestAuthorization = basicStrategy.get().parseAuthorizationFromMap(authParams);
        assertEquals(RequestAuthorizationType.BASIC, requestAuthorization.getType());
        assertEquals(username, ((BasicRequestAuthorization) requestAuthorization).getUsername());
        assertEquals(password, ((BasicRequestAuthorization) requestAuthorization).getPassword());
    }

    @Test
    public void generateAuthHeader() {
        BasicRequestAuthorization basicAuth = EntitiesGenerator.generateBasicRequestAuthorization();
        RequestHeader authHeader = basicStrategy.get().generateAuthorizationHeader(basicAuth);

        // then
        Assertions.assertNotNull(authHeader);
        Assertions.assertEquals("Basic dXNlcm5hbWU6cGFzc3dvcmQ=", authHeader.getValue());
    }
}
