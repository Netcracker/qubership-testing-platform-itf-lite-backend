package org.qubership.atp.itf.lite.backend.components.auth;

import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.configuration.MapperConfiguration;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BearerRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.qubership.atp.itf.lite.backend.service.TemplateResolverService;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class BearerAuthorizationStrategyTest {

    @Mock
    private EncryptionService encryptionService;
    @InjectMocks
    private BearerAuthorizationStrategy bearerAuthorizationStrategy;

    @BeforeEach
    public void setUp() {
        TemplateResolverService restTemplateService = mock(TemplateResolverService.class);
        AuthorizationUtils.setObjectMapper(new ObjectMapper());
        AuthorizationUtils.setTemplateResolverService(restTemplateService);
        AuthorizationUtils.setModelMapper(new MapperConfiguration().modelMapper());
    }

    @Test
    void parseAuthorizationFromMap_whenImportWithCorrectlyToken() {
        Map<String, String> authorizationInfo = new HashMap<>();
        authorizationInfo.put("token", "anyToken");

        RequestAuthorization requestAuthorization = bearerAuthorizationStrategy
                .parseAuthorizationFromMap(authorizationInfo);
        BearerRequestAuthorization bearerRequestAuthorization = (BearerRequestAuthorization) requestAuthorization;

        Assertions.assertEquals(bearerRequestAuthorization.getToken(), authorizationInfo.get("token"));
    }

    @Test
    void parseAuthorizationFromMap_HaveEmptyStringValue_requestAuthorizationIsNull() {
        Map<String, String> authorizationInfo = new HashMap<>();
        authorizationInfo.put("token", "");

        RequestAuthorization requestAuthorization = bearerAuthorizationStrategy
                .parseAuthorizationFromMap(authorizationInfo);

        Assertions.assertEquals( requestAuthorization, null, "requestAuthorization is not null");
    }

    @Test
    void parseAuthorizationFromMap_HaveEmptyNullValue_requestAuthorizationIsNull() {
        Map<String, String> authorizationInfo = new HashMap<>();
        authorizationInfo.put("token", null);

        RequestAuthorization requestAuthorization = bearerAuthorizationStrategy
                .parseAuthorizationFromMap(authorizationInfo);

        Assertions.assertEquals( requestAuthorization, null, "requestAuthorization is not null");
    }

    @Test
    public void generateAuthHeader_BearerAuthorization() {
        BearerRequestAuthorization bearerAuth = EntitiesGenerator.generateBearerRequestAuthorization("token");
        RequestHeader authHeader = bearerAuthorizationStrategy.generateAuthorizationHeader(bearerAuth);

        // then
        Assertions.assertNotNull(authHeader);
        Assertions.assertEquals("Bearer token", authHeader.getValue());
    }
}
