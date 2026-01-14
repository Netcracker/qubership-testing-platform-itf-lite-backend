/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
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

package org.qubership.atp.itf.lite.backend.components.auth;

import static java.util.Objects.nonNull;
import static org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType.OAUTH2;
import static org.qubership.atp.itf.lite.backend.utils.Constants.EMPTY_STRING;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.utils.ExceptionUtils;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.enums.auth.OAuth2GrantType;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteRequestIllegalAuthorizationGrantTypeException;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyRequest;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyResponse;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth2AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.auth.OAuth2AuthrizationResponse;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth2RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.qubership.atp.itf.lite.backend.service.rest.RestTemplateService;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OAuth2RequestAuthorizationStrategy extends AbstractAuthorizationStrategy
        implements RequestAuthorizationStrategy {

    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String AUTH_HEADER_PREFIX_PATTERN = "%s %s";

    public static final String TO_ENCRYPT_FLAG = "{2ENC}";
    private static final String IS_ENCRYPTED_FLAG = "{ENC}";

    private final RestTemplateService restTemplateService;
    private final ModelMapper modelMapper;

    /**
     * Constructor for OAuth2RequestAuthorizationStrategy.
     */
    public OAuth2RequestAuthorizationStrategy(RestTemplateService restTemplateService,
                                              EncryptionService encryptionService,
                                              ModelMapper modelMapper) {
        super(encryptionService);
        this.restTemplateService = restTemplateService;
        this.modelMapper = modelMapper;
    }

    @Override
    public AuthorizationStrategyResponse getAuthorizationToken(
            AuthorizationStrategyRequest request)
            throws AtpDecryptException {
        AuthorizationSaveRequest authorization = request.getUnsafeAuthorizationRequest();
        final OAuth2AuthorizationSaveRequest oAuth2Authorization = (OAuth2AuthorizationSaveRequest) authorization;
        log.debug("Get Authorization header using OAuth2 type and params: {}", oAuth2Authorization);
        decryptParameters(authorization);
        decodeParameters(authorization);
        decryptParameters(authorization);
        final OAuth2GrantType grantType = oAuth2Authorization.getGrantType();
        final String username = oAuth2Authorization.getUsername();
        final String password = oAuth2Authorization.getPassword();
        final String clientId = oAuth2Authorization.getClientId();
        final String clientSecret = oAuth2Authorization.getClientSecret();
        final String url = oAuth2Authorization.getUrl();
        final String headerPrefix = oAuth2Authorization.getHeaderPrefix();
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(CLIENT_ID, clientId);
        switch (grantType) {
            case CLIENT_CREDENTIALS:
                params.add(GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
                params.add(CLIENT_SECRET, clientSecret);
                break;
            case PASSWORD_CREDENTIALS:
                params.add(GRANT_TYPE, AuthorizationGrantType.PASSWORD.getValue());
                params.add(USERNAME, username);
                params.add(PASSWORD, password);
                break;
            case AUTHORIZATION_CODE:
                String authHeader = oAuth2Authorization.getToken();
                if (StringUtils.isEmpty(authHeader)) {
                    log.warn("Token for AUTHORIZATION_CODE auth type not generated. Generating auth header skipped");
                    return null;
                }
                if (StringUtils.isNotEmpty(headerPrefix)) {
                    authHeader = String.format(AUTH_HEADER_PREFIX_PATTERN, headerPrefix, authHeader);
                }
                log.debug("Result Authorization header: {}", authHeader);
                return new AuthorizationStrategyResponse(authHeader, authHeader);
            default:
                ExceptionUtils.throwWithLog(log, new ItfLiteRequestIllegalAuthorizationGrantTypeException(grantType));
        }
        OAuth2AuthrizationResponse response = performAuthorization(request.getProjectId(),
                url, params);
        String authHeader = response.getAccessToken();
        if (StringUtils.isNotEmpty(headerPrefix)) {
            authHeader = String.format(AUTH_HEADER_PREFIX_PATTERN, headerPrefix, authHeader);
        }
        log.debug("Result Authorization header: {}", authHeader);
        return new AuthorizationStrategyResponse(authHeader, authHeader);
    }

    @Override
    public void decryptParameters(AuthorizationSaveRequest authorization) {
        log.debug("Decrypt authorization parameters");
        final OAuth2AuthorizationSaveRequest oAuth2Authorization = (OAuth2AuthorizationSaveRequest) authorization;
        decryptParameter(oAuth2Authorization::getPassword, oAuth2Authorization::setPassword);
        log.debug("Password has been successfully decrypted");
        decryptParameter(oAuth2Authorization::getClientSecret, oAuth2Authorization::setClientSecret);
        log.debug("Client secret has been successfully decrypted");
    }

    @Override
    public void encryptParameters(AuthorizationSaveRequest authorization) {
        log.debug("Encrypt authorization parameters");
        final OAuth2AuthorizationSaveRequest oAuth2Authorization = (OAuth2AuthorizationSaveRequest) authorization;
        encryptParameter(oAuth2Authorization::getPassword, oAuth2Authorization::setPassword);
        log.debug("Password has been successfully encrypted");
        encryptParameter(oAuth2Authorization::getClientSecret, oAuth2Authorization::setClientSecret);
        log.debug("Client secret has been successfully encrypted");
    }

    /**
     * Decode parameters.
     */
    public void decodeParameters(AuthorizationSaveRequest authorization) {
        log.debug("Decode authorization parameters");
        final OAuth2AuthorizationSaveRequest oAuth2Authorization = (OAuth2AuthorizationSaveRequest) authorization;
        decodeParameter(oAuth2Authorization::getPassword, oAuth2Authorization::setPassword);
        log.debug("Password has been successfully decoded");
        decodeParameter(oAuth2Authorization::getClientSecret, oAuth2Authorization::setClientSecret);
        log.debug("Client secret has been successfully decoded");
    }

    /**
     * Perform authorization.
     */
    public OAuth2AuthrizationResponse performAuthorization(UUID projectId, String url,
                                                           MultiValueMap<String, String> map) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> params = new HttpEntity<>(map, headers);
        Class<OAuth2AuthrizationResponse> responseType = OAuth2AuthrizationResponse.class;
        return restTemplateService.restTemplate(projectId).postForEntity(url, params, responseType).getBody();
    }

    @Override
    public RequestAuthorizationType getAuthorizationType() {
        return OAUTH2;
    }

    @Override
    public RequestAuthorization parseAuthorizationFromMap(Map<String, String> authorizationInfo) {
        OAuth2AuthorizationSaveRequest authorizationSaveRequest = new OAuth2AuthorizationSaveRequest();
        authorizationSaveRequest.setType(OAUTH2);

        if (authorizationInfo.containsKey(Constants.HEADER_PREFIX_CAMEL_CASE)) {
            authorizationSaveRequest.setHeaderPrefix(authorizationInfo.get(Constants.HEADER_PREFIX_CAMEL_CASE));
        } else if (authorizationInfo.containsKey(Constants.HEADER_PREFIX_SNAKE_CASE)) {
            authorizationSaveRequest.setHeaderPrefix(authorizationInfo.get(Constants.HEADER_PREFIX_SNAKE_CASE));
        }

        if (authorizationInfo.containsKey(Constants.GRANT_TYPE)) {
            OAuth2GrantType grantType = Arrays.stream(OAuth2GrantType.values()).filter(currentGrantType ->
                    currentGrantType.getKey().equals(authorizationInfo.get(Constants.GRANT_TYPE)))
                    .findFirst().orElse(OAuth2GrantType.PASSWORD_CREDENTIALS);
            authorizationSaveRequest.setGrantType(grantType);
        } else {
            authorizationSaveRequest.setGrantType(OAuth2GrantType.PASSWORD_CREDENTIALS);
        }
        if (authorizationInfo.containsKey(Constants.SCOPE)) {
            authorizationSaveRequest.setScope(authorizationInfo.get(Constants.SCOPE));
        }
        if (authorizationInfo.containsKey(Constants.USERNAME)) {
            authorizationSaveRequest.setUsername(authorizationInfo.get(Constants.USERNAME));
        }

        authorizationSaveRequest.setClientId(authorizationInfo.getOrDefault(Constants.CLIENT_ID, EMPTY_STRING));

        authorizationSaveRequest.setUrl(authorizationInfo.getOrDefault(Constants.ACCESS_TOKEN_URL, EMPTY_STRING));

        // add {2ENC} flag and encoding for further encryption
        if (authorizationInfo.containsKey(Constants.PASSWORD)) {
            authorizationSaveRequest.setPassword(TO_ENCRYPT_FLAG
                    + encryptionService.encodeBase64(authorizationInfo.get(Constants.PASSWORD)));
        }
        if (authorizationInfo.containsKey(Constants.CLIENT_SECRET)) {
            authorizationSaveRequest.setClientSecret(TO_ENCRYPT_FLAG
                    + encryptionService.encodeBase64(authorizationInfo.get(Constants.CLIENT_SECRET)));
        }

        // need to encrypt sensitive parameters
        encryptParameters(authorizationSaveRequest);
        return modelMapper.map(authorizationSaveRequest,
                OAuth2RequestAuthorization.class);
    }

    /**
     * Generates a header to be displayed on the UI.
     * @param authorization request authorization
     * @return {@link RequestHeader} generated request header
     */
    @Nullable
    public RequestHeader generateAuthorizationHeader(RequestAuthorization authorization) {
        OAuth2AuthorizationSaveRequest oauthAuthorization =
                (OAuth2AuthorizationSaveRequest) AuthorizationUtils.castToAuthorizationSaveRequest(authorization);
        OAuth2GrantType grantType = oauthAuthorization.getGrantType();
        if (nonNull(grantType)) {
            final String headerValue;
            switch (grantType) {
                case PASSWORD_CREDENTIALS:
                case CLIENT_CREDENTIALS:
                    headerValue = CALCULATED_VALUE;
                    break;
                case AUTHORIZATION_CODE:
                    String headerPrefix = oauthAuthorization.getHeaderPrefix();
                    String token = oauthAuthorization.getToken();
                    headerValue = StringUtils.isNotEmpty(headerPrefix)
                            ? String.format(AUTH_HEADER_PREFIX_PATTERN, headerPrefix, token)
                            : token;
                    break;
                default:
                    log.error("Found unsupported OAuth2 grant type: {}", grantType);
                    throw new ItfLiteRequestIllegalAuthorizationGrantTypeException(grantType);
            }
            return new RequestHeader(null, AUTH_HEADER_KEY, headerValue, "", false, true);
        } else {
            log.warn("Grant type for OAuth authorization not sets. Generating of authorization header skipped");
            return null;
        }
    }
}
