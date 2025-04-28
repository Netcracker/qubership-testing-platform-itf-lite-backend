package org.qubership.atp.itf.lite.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequestEntitySaveRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.crypt.exception.AtpEncryptException;
import org.qubership.atp.itf.lite.backend.components.auth.AbstractAuthorizationStrategy;
import org.qubership.atp.itf.lite.backend.components.auth.BearerAuthorizationStrategy;
import org.qubership.atp.itf.lite.backend.components.auth.RequestAuthorizationRegistry;
import org.qubership.atp.itf.lite.backend.configuration.MapperConfiguration;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.BearerAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth2AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;
import org.qubership.atp.itf.lite.backend.utils.RequestTestUtils;
import org.qubership.atp.macros.core.processor.Evaluator;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
class RequestAuthorizationServiceTest {

    private final ThreadLocal<RequestAuthorizationRegistry> authRegistry = new ThreadLocal<>();
    private final ThreadLocal<RequestAuthorizationService> service = new ThreadLocal<>();

    @BeforeAll
    public static void init() {
        AuthorizationUtils.setObjectMapper(new ObjectMapper());
        AuthorizationUtils.setTemplateResolverService(mock(TemplateResolverService.class));
        AuthorizationUtils.setModelMapper(new MapperConfiguration().modelMapper());
    }

    @BeforeEach
    public void setUp() throws AtpEncryptException, IOException {
        RequestAuthorizationRegistry requestAuthorizationRegistryMock = mock(RequestAuthorizationRegistry.class);
        authRegistry.set(requestAuthorizationRegistryMock);
        service.set(new RequestAuthorizationService(requestAuthorizationRegistryMock));
    }

    private OAuth2AuthorizationSaveRequest createOAuth2AuthorizationSaveRequest() {
        OAuth2AuthorizationSaveRequest authorization = new OAuth2AuthorizationSaveRequest();
        authorization.setType(RequestAuthorizationType.OAUTH2);
        authorization.setUrl("https://atp-keycloak-dev222.dev-atp-cloud.com/auth/realms/atp2/protocol/openid-connect/token");
        authorization.setClientId("itf-lite-backend");
        return authorization;
    }

    @Test
    public void processRequestAuthorization() throws AtpDecryptException, JsonProcessingException {
        // given
        final UUID projectId = UUID.randomUUID();
        HttpRequestEntitySaveRequest  httpRequest = EntitiesGenerator.generateRandomHttpRequestEntitySaveRequest();
        AuthorizationSaveRequest auth = new BearerAuthorizationSaveRequest("token");
        auth.setType(RequestAuthorizationType.BEARER);
        httpRequest.setAuthorization(auth);
        Evaluator evaluator = mock(Evaluator.class);

        // when
        when(authRegistry.get().getRequestAuthorizationStrategy(RequestAuthorizationType.BEARER))
                .thenReturn(new BearerAuthorizationStrategy(null));
        String result = service.get().processRequestAuthorization(projectId, httpRequest, httpRequest,
                null, evaluator, null);

        // then
        Assertions.assertEquals("Bearer token", result);
        Assertions.assertEquals("Authorization", httpRequest.getRequestHeaders().get(1).getKey());
        Assertions.assertEquals("Bearer token", httpRequest.getRequestHeaders().get(1).getValue());
    }

    @Test
    void invalidAuthorizationTypeSet_processRequestAuthorization() {
        // given
        OAuth2AuthorizationSaveRequest authorization = createOAuth2AuthorizationSaveRequest();
        authorization.setType(null);
        HttpRequestEntitySaveRequest request = generateRandomHttpRequestEntitySaveRequest();
        request.setAuthorization(authorization);
        HttpRequestEntitySaveRequest historyRequest = generateRandomHttpRequestEntitySaveRequest();
        Evaluator evaluator = mock(Evaluator.class);

        // when
        when(authRegistry.get().getRequestAuthorizationStrategy(eq(null))).thenCallRealMethod();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.get().processRequestAuthorization(UUID.randomUUID(), request, historyRequest,
                        UUID.randomUUID(), evaluator, RequestTestUtils.generateContext())
        );
        assertEquals("Failed to find request authorization strategy implementation for auth type: null", exception.getMessage());
    }

    @Test
    public void parseAuthorizationFromMapTest_successfullyParsed() {
        // when
        AbstractAuthorizationStrategy authStrategy = mock(AbstractAuthorizationStrategy.class);
        when(authRegistry.get().getRequestAuthorizationStrategy(any())).thenReturn(authStrategy);

        service.get().parseAuthorizationFromMap(new HashMap<>(), RequestAuthorizationType.BEARER);
        // then
        verify(authStrategy, times(1)).parseAuthorizationFromMap(any());
    }

    @Test
    public void generateAuthorizationHeader_successfullyGenerated() {
        // given
        RequestAuthorization requestAuthorization = new RequestAuthorization();
        requestAuthorization.setType(RequestAuthorizationType.BASIC);

        // when
        AbstractAuthorizationStrategy authStrategy = mock(AbstractAuthorizationStrategy.class);
        when(authRegistry.get().getRequestAuthorizationStrategy(any())).thenReturn(authStrategy);

        service.get().generateAuthorizationHeader(requestAuthorization);
        // then
        verify(authStrategy, times(1)).generateAuthorizationHeader(any());
    }

    @Test
    public void generateAuthorizationHeader_authTypeNotSpecified_nullReturned() {
        // given
        RequestAuthorization requestAuthorization = new RequestAuthorization();

        // when
        AbstractAuthorizationStrategy authStrategy = mock(AbstractAuthorizationStrategy.class);
        when(authRegistry.get().getRequestAuthorizationStrategy(any())).thenReturn(authStrategy);

        service.get().generateAuthorizationHeader(requestAuthorization);
        // then
        verify(authStrategy, times(0)).generateAuthorizationHeader(any());
    }
}
