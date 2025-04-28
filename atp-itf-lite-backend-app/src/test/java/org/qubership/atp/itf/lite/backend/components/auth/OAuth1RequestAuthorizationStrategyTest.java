package org.qubership.atp.itf.lite.backend.components.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.enums.auth.OAuth1AddDataType;
import org.qubership.atp.itf.lite.backend.enums.auth.OAuth1SignatureMethod;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyRequest;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyResponse;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationResolvingContext;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth1AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;

class OAuth1RequestAuthorizationStrategyTest {

    private EncryptionService encryptionService;
    private ModelMapper modelMapper;
    private OAuth1RequestAuthorizationStrategy strategy;

    private static final String CONSUMER_KEY = "BsEAipOciVhJ4VSGBbKzmWLAC";
    private static final String CONSUMER_SECRET = "6DbOwyXmIHM9q25y8OrKsmxcieYHYVbMmC4MQCgDhT9CcS1D8x";
    private static final String ACCESS_TOKEN = "330898544-RliF9rcbf2UsQiFc5C7mMbRCdHhEfkoVxKIpkkck";
    private static final String TOKEN_SECRET = "hDwhvc7sFEt8Ojo8OE3aq0znQ9nOh9X31DaTz2NULLHWX";
    private static final OAuth1SignatureMethod SIGNATURE_METHOD = OAuth1SignatureMethod.HMAC_SHA1;
    private static final OAuth1AddDataType ADD_DATA_TYPE = OAuth1AddDataType.REQUEST_HEADERS;

    @BeforeEach
    void setUp() {
        encryptionService = mock(EncryptionService.class);
        modelMapper = new ModelMapper();
        strategy = new OAuth1RequestAuthorizationStrategy(encryptionService, modelMapper);
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void testGetAuthorizationTokenTest() throws AtpDecryptException {
        // Given
        OAuth1AuthorizationSaveRequest authSaveRequest = new OAuth1AuthorizationSaveRequest();

        authSaveRequest.setAddDataType(ADD_DATA_TYPE);
        authSaveRequest.setConsumerKey(CONSUMER_KEY);
        authSaveRequest.setConsumerSecret(CONSUMER_SECRET);
        authSaveRequest.setAccessToken(ACCESS_TOKEN);
        authSaveRequest.setTokenSecret(TOKEN_SECRET);
        authSaveRequest.setSignatureMethod(SIGNATURE_METHOD);

        AuthorizationStrategyRequest request = new AuthorizationStrategyRequest();
        request.setUnsafeAuthorizationRequest(authSaveRequest);

        AuthorizationResolvingContext context = new AuthorizationResolvingContext();
        context.setUrl("https://api.twitter.com/oauth/request_token");
        context.setHttpMethod(HttpMethod.GET);
        request.setAuthResolvingContext(context);

        // When
        AuthorizationStrategyResponse response = strategy.getAuthorizationToken(request);

        // Then
        assertNotNull(response);
        String resultToken = response.getSafeAuthorizationToken();

        assertTrue(resultToken.startsWith("OAuth "));

        // Remove the OAuth  prefix
        resultToken = resultToken.substring(6);

        final Map<String, String> tokenMap = new HashMap<>();
        for (String token : resultToken.split(", ")) {
            String[] keyValue = token.split("=");
            tokenMap.put(keyValue[0], keyValue[1].replace("\"", "").trim());
        }

        assertNotNull(tokenMap);
        assertEquals(7, tokenMap.size());

        // check dynamic values
        assertTrue(tokenMap.containsKey("oauth_timestamp"));
        assertTrue(tokenMap.containsKey("oauth_nonce"));
        assertTrue(tokenMap.containsKey("oauth_signature"));

        // check static values
        assertTrue(tokenMap.containsKey("oauth_consumer_key"));
        assertEquals(CONSUMER_KEY, tokenMap.get("oauth_consumer_key"));

        assertTrue(tokenMap.containsKey("oauth_signature_method"));
        assertTrue(tokenMap.containsKey("oauth_signature_method"));

        assertTrue(tokenMap.containsKey("oauth_token"));
        assertEquals(ACCESS_TOKEN, tokenMap.get("oauth_token"));

        assertTrue(tokenMap.containsKey("oauth_version"));
        assertEquals("1.0", tokenMap.get("oauth_version"));
    }
}