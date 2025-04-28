package org.qubership.atp.itf.lite.backend.components.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.components.auth.OAuth2RequestAuthorizationStrategy.CLIENT_ID;
import static org.qubership.atp.itf.lite.backend.components.auth.OAuth2RequestAuthorizationStrategy.CLIENT_SECRET;
import static org.qubership.atp.itf.lite.backend.components.auth.OAuth2RequestAuthorizationStrategy.GRANT_TYPE;
import static org.qubership.atp.itf.lite.backend.components.auth.OAuth2RequestAuthorizationStrategy.PASSWORD;
import static org.qubership.atp.itf.lite.backend.components.auth.OAuth2RequestAuthorizationStrategy.USERNAME;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateOAuth2AuthMap;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.crypt.exception.AtpEncryptException;
import org.qubership.atp.itf.lite.backend.configuration.MapperConfiguration;
import org.qubership.atp.itf.lite.backend.enums.auth.OAuth2GrantType;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyRequest;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyResponse;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth2AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.auth.OAuth2AuthrizationResponse;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth2RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.qubership.atp.itf.lite.backend.service.TemplateResolverService;
import org.qubership.atp.itf.lite.backend.service.rest.RestTemplateService;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.qubership.atp.macros.core.processor.Evaluator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

public class OAuth2RequestAuthorizationStrategyTest {


    // 1. getAuthorizationToken
    // 3. parseAuthorizationFromMap

    private final ThreadLocal<RestTemplateService> restTemplateService = new ThreadLocal<>();
    private final ThreadLocal<EncryptionService> encryptionService = new ThreadLocal<>();
    private final ThreadLocal<OAuth2RequestAuthorizationStrategy> oauthStrategy = new ThreadLocal<>();

    private static final String username = "username";
    private static final String password = "password";
    private static final String encryptedParameter = "{ENC}{9s637qdqkLj6xCvA5yXzQw==}{QBvxG0Rfmp+N360gW7HKuA==}";
    private static final String clientSecret = "290238dd-5e4b-40ff-9b18-8b5ebcd628d5";
    private static final String headerPrefix = "Bearer";
    private static final String accessToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIi...";
    private static final String url = "https://atp-keycloak-dev222.dev-atp-cloud.com/auth/realms/atp2/protocol/openid-connect/token";

    @BeforeEach
    public void setUp() throws AtpEncryptException {
        RestTemplateService restTemplateServiceMock = mock(RestTemplateService.class);
        EncryptionService encryptionServiceMock = mock(EncryptionService.class);
        restTemplateService.set(restTemplateServiceMock);
        encryptionService.set(encryptionServiceMock);
        oauthStrategy.set(new OAuth2RequestAuthorizationStrategy(restTemplateServiceMock, encryptionServiceMock,
                new MapperConfiguration().modelMapper()));

        TemplateResolverService restTemplateService = mock(TemplateResolverService.class);
        AuthorizationUtils.setObjectMapper(new ObjectMapper());
        AuthorizationUtils.setTemplateResolverService(restTemplateService);
        AuthorizationUtils.setModelMapper(new MapperConfiguration().modelMapper());
    }

    private OAuth2AuthorizationSaveRequest createOAuth2AuthorizationSaveRequest() {
        OAuth2AuthorizationSaveRequest authorization = new OAuth2AuthorizationSaveRequest();
        authorization.setType(RequestAuthorizationType.OAUTH2);
        authorization.setUrl("https://atp-keycloak-dev222.dev-atp-cloud.com/auth/realms/atp2/protocol/openid-connect/token");
        authorization.setClientId("itf-lite-backend");
        return authorization;
    }

    @Test
    void encryptParameters_expectEncryptedClientSecret() throws AtpEncryptException {
        // given
        OAuth2AuthorizationSaveRequest authorization = createOAuth2AuthorizationSaveRequest();
        authorization.setGrantType(OAuth2GrantType.CLIENT_CREDENTIALS);
        authorization.setClientSecret("{2ENC}" + Base64.getEncoder().encodeToString(clientSecret.getBytes(StandardCharsets.UTF_8)));

        // when
        when(encryptionService.get().decodeBase64(any())).thenCallRealMethod();
        when(encryptionService.get().encrypt(anyString())).thenReturn(encryptedParameter);
        oauthStrategy.get().encryptParameters(authorization);

        assertNotNull(authorization, "Processed request entity shouldn't be null");
        String processedClientSecret = authorization.getClientSecret();
        assertNotNull(processedClientSecret, "Processed client secret shouldn't be null");
        assertEquals(encryptedParameter, processedClientSecret, "Processed client secret should be encrypted");
    }

    /**
     * Given:
     * - authorizationType = OAUTH2
     * - grantType = PASSWORD_CREDENTIALS
     * - NO Authorization header in the executed request
     *
     * Expected:
     * - OAUTH2 should be successfully sent with the expected form params
     * - Received Bearer token should be added to request headers
     */
    @Test
    void getAuthorizationToken_passwordGrantType() throws AtpDecryptException {
        // given
        OAuth2AuthorizationSaveRequest authorization = createOAuth2AuthorizationSaveRequest();
        authorization.setGrantType(OAuth2GrantType.PASSWORD_CREDENTIALS);
        authorization.setUsername(username);
        authorization.setPassword(password);
        authorization.setHeaderPrefix(headerPrefix);
        Evaluator evaluator = mock(Evaluator.class);
        AuthorizationStrategyRequest request = new AuthorizationStrategyRequest(authorization, authorization,
                null, UUID.randomUUID(), UUID.randomUUID(), evaluator, null);
        OAuth2AuthrizationResponse response = new OAuth2AuthrizationResponse(accessToken);

        // when
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(anyString(), any(), any())).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
        when(restTemplateService.get().restTemplate(any())).thenReturn(restTemplate);
        AuthorizationStrategyResponse res = oauthStrategy.get().getAuthorizationToken(request);

        // then
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(1))
                .postForEntity(urlCaptor.capture(), httpEntityCaptor.capture(), any());

        final String processedUrl = urlCaptor.getValue();
        assertProcessedUrl(processedUrl);

        final HttpEntity<MultiValueMap<String, String>> processedHttpEntity = httpEntityCaptor.getValue();
        assertOAuth2FormParamsPresence(processedHttpEntity, GRANT_TYPE, CLIENT_ID, USERNAME, PASSWORD);

        assertEquals(headerPrefix + " " + accessToken, res.getUnsafeAuthorizationToken());
    }

    /**
     * Given:
     * - authorizationType = OAUTH2
     * - grantType = CLIENT_CREDENTIALS
     * - Authorization header ALREADY PRESENTS in the executed request
     *
     * Expected:
     * - OAUTH2 should be successfully sent with the expected form params
     * - Received Bearer token should be overwritten in the request headers
     */
    @Test
    void getAuthorizationToken_clientCredentialsGrantType() throws AtpDecryptException {
        // given
        OAuth2AuthorizationSaveRequest authorization = createOAuth2AuthorizationSaveRequest();
        authorization.setGrantType(OAuth2GrantType.CLIENT_CREDENTIALS);
        authorization.setClientSecret(clientSecret);
        Evaluator evaluator = mock(Evaluator.class);
        AuthorizationStrategyRequest request = new AuthorizationStrategyRequest(authorization, authorization,
                null, UUID.randomUUID(), UUID.randomUUID(), evaluator, null);
        OAuth2AuthrizationResponse response = new OAuth2AuthrizationResponse(accessToken);

        // when
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(anyString(), any(), any())).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
        when(restTemplateService.get().restTemplate(any())).thenReturn(restTemplate);
        AuthorizationStrategyResponse res = oauthStrategy.get().getAuthorizationToken(request);

        // then
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(1))
                .postForEntity(urlCaptor.capture(), httpEntityCaptor.capture(), any());

        final String processedUrl = urlCaptor.getValue();
        assertProcessedUrl(processedUrl);

        final HttpEntity<MultiValueMap<String, String>> processedHttpEntity = httpEntityCaptor.getValue();
        assertOAuth2FormParamsPresence(processedHttpEntity, GRANT_TYPE, CLIENT_ID, CLIENT_SECRET);

        assertEquals(accessToken, res.getUnsafeAuthorizationToken());
    }

    /**
     * Given:
     * - authorizationType = OAUTH2
     * - grantType = AUTHORIZATION_CODE
     *
     * Expected:
     * - OAUTH2 should return token
     */
    @Test
    void getAuthorizationToken_authorizationCodeGrantType() throws AtpDecryptException {
        // given
        OAuth2AuthorizationSaveRequest authorization = createOAuth2AuthorizationSaveRequest();
        authorization.setGrantType(OAuth2GrantType.AUTHORIZATION_CODE);
        authorization.setHeaderPrefix("Bearer");
        authorization.setToken("token");
        Evaluator evaluator = mock(Evaluator.class);
        AuthorizationStrategyRequest request = new AuthorizationStrategyRequest(authorization, authorization,
                null, UUID.randomUUID(), UUID.randomUUID(), evaluator, null);

        // when
        AuthorizationStrategyResponse res = oauthStrategy.get().getAuthorizationToken(request);

        // then
        assertEquals("Bearer token", res.getUnsafeAuthorizationToken());
    }

    @Test
    public void parseAuthorizationFromMap() throws AtpEncryptException {
        // given
        Map<String, String> passwordCredentialsMap = generateOAuth2AuthMap(OAuth2GrantType.PASSWORD_CREDENTIALS);
        Map<String, String> clientCredentialsMap = generateOAuth2AuthMap(OAuth2GrantType.CLIENT_CREDENTIALS);
        // remove camel case header prefix
        clientCredentialsMap.remove(Constants.HEADER_PREFIX_CAMEL_CASE);
        // add snake case header prefix
        clientCredentialsMap.put(Constants.HEADER_PREFIX_SNAKE_CASE, "Bearer");
        // when
        OAuth2RequestAuthorization actualPasswordAuthorization = (OAuth2RequestAuthorization)
                oauthStrategy.get().parseAuthorizationFromMap(passwordCredentialsMap);
        OAuth2RequestAuthorization actualClientAuthorization = (OAuth2RequestAuthorization)
                oauthStrategy.get().parseAuthorizationFromMap(clientCredentialsMap);
        // then
        assertEquals(RequestAuthorizationType.OAUTH2, actualPasswordAuthorization.getType());
        assertEquals(OAuth2GrantType.PASSWORD_CREDENTIALS, actualPasswordAuthorization.getGrantType());
        assertEquals(passwordCredentialsMap.get(Constants.HEADER_PREFIX_CAMEL_CASE),
                actualPasswordAuthorization.getHeaderPrefix());
        assertEquals(passwordCredentialsMap.get(Constants.ACCESS_TOKEN_URL), actualPasswordAuthorization.getUrl());
        assertEquals(passwordCredentialsMap.get(Constants.CLIENT_ID), actualPasswordAuthorization.getClientId());
        assertEquals(encryptionService.get().encrypt(passwordCredentialsMap.get(Constants.CLIENT_SECRET)),
                actualPasswordAuthorization.getClientSecret());
        assertEquals(passwordCredentialsMap.get(Constants.USERNAME), actualPasswordAuthorization.getUsername());
        assertEquals(encryptionService.get().encrypt(passwordCredentialsMap.get(Constants.PASSWORD)),
                actualPasswordAuthorization.getPassword());
        assertEquals(passwordCredentialsMap.get(Constants.SCOPE), actualPasswordAuthorization.getScope());
        assertEquals(RequestAuthorizationType.OAUTH2, actualClientAuthorization.getType());
        assertEquals(OAuth2GrantType.CLIENT_CREDENTIALS, actualClientAuthorization.getGrantType());
        assertEquals(clientCredentialsMap.get(Constants.HEADER_PREFIX_SNAKE_CASE),
                actualPasswordAuthorization.getHeaderPrefix());
        assertEquals(clientCredentialsMap.get(Constants.ACCESS_TOKEN_URL), actualClientAuthorization.getUrl());
        assertEquals(clientCredentialsMap.get(Constants.CLIENT_ID), actualClientAuthorization.getClientId());
        assertEquals(encryptionService.get().encrypt(clientCredentialsMap.get(Constants.CLIENT_SECRET)),
                actualClientAuthorization.getClientSecret());
        assertNull(actualClientAuthorization.getUsername());
        assertNull(actualClientAuthorization.getPassword());
        assertEquals(clientCredentialsMap.get(Constants.SCOPE), actualClientAuthorization.getScope());
    }

    @Test
    public void generateAuthHeader_passwordCredentialsGrantType() {
        OAuth2RequestAuthorization OAuth2WithPasswordCredentials =
                EntitiesGenerator.generateOAuth2RequestAuthorization(OAuth2GrantType.PASSWORD_CREDENTIALS);
        RequestHeader authHeader = oauthStrategy.get().generateAuthorizationHeader(OAuth2WithPasswordCredentials);

        // then
        Assertions.assertNotNull(authHeader);
        Assertions.assertEquals("<calculated when request is sent>", authHeader.getValue());
    }

    @Test
    public void generateAuthHeader_clientCredentialsGrantType() {
        OAuth2RequestAuthorization OAuth2WithPasswordCredentials =
                EntitiesGenerator.generateOAuth2RequestAuthorization(OAuth2GrantType.CLIENT_CREDENTIALS);
        RequestHeader authHeader = oauthStrategy.get().generateAuthorizationHeader(OAuth2WithPasswordCredentials);

        // then
        Assertions.assertNotNull(authHeader);
        Assertions.assertEquals("<calculated when request is sent>", authHeader.getValue());
    }

    @Test
    public void generateAuthHeader_AuthorizationCodeGrantType() {
        OAuth2RequestAuthorization OAuth2WithPasswordCredentials =
                EntitiesGenerator.generateOAuth2RequestAuthorization(OAuth2GrantType.AUTHORIZATION_CODE);
        RequestHeader authHeader = oauthStrategy.get().generateAuthorizationHeader(OAuth2WithPasswordCredentials);

        // then
        Assertions.assertNotNull(authHeader);
        Assertions.assertEquals("token", authHeader.getValue());
    }

    private void assertProcessedUrl(String processedUrl) {
        assertNotNull(processedUrl, "Processed URL shouldn't be null");
        assertEquals(url, processedUrl, "Processed URL should be equal to the request URL");
    }

    private void assertOAuth2FormParamsPresence(HttpEntity<MultiValueMap<String, String>> processedHttpEntity, String... params) {
        assertNotNull(processedHttpEntity, "Processed auth http entity shouldn't be null");

        final MultiValueMap<String, String> processedParams = processedHttpEntity.getBody();
        assertNotNull(processedParams, "Processed params shouldn't be null");
        assertFalse(processedParams.isEmpty(), "Processed params shouldn't be empty");
        assertEquals(params.length, processedParams.size(), "Expected processed params size is invalid");

        for (String param : params) {
            assertTrue(processedParams.containsKey(param), "'" + param + "' param should be present in auth OAuth2 request");
        }
    }
}
