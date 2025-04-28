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

import static org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType.BASIC;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyRequest;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyResponse;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.BasicAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.auth.OAuth2AuthrizationResponse;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BasicRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BasicRequestAuthorizationStrategy extends AbstractAuthorizationStrategy
        implements RequestAuthorizationStrategy {

    private Base64.Encoder base64Encoder;

    public BasicRequestAuthorizationStrategy(EncryptionService encryptionService) {
        super(encryptionService);
        this.base64Encoder = Base64.getEncoder();
    }

    @Override
    public AuthorizationStrategyResponse getAuthorizationToken(AuthorizationStrategyRequest request)
            throws AtpDecryptException {
        log.debug("Get authorization token for request");
        final BasicAuthorizationSaveRequest authorization = (BasicAuthorizationSaveRequest)
                request.getUnsafeAuthorizationRequest();
        final String authHeader = generateAuthHeaderValue(authorization);
        log.debug("Result Authorization header: {}", authHeader);
        return new AuthorizationStrategyResponse(authHeader, authHeader);
    }

    private String generateAuthHeaderValue(BasicAuthorizationSaveRequest authorization) {
        decryptParameters(authorization);
        decodeParameters(authorization);
        decryptParameters(authorization);

        final String username = authorization.getUsername();
        final String password = authorization.getPassword();
        final String toEncode = username + ":" + password;

        return "Basic " + base64Encoder.encodeToString(toEncode.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode parameters.
     */
    public void decodeParameters(AuthorizationSaveRequest authorization) {
        log.debug("Decode authorization parameters");
        final BasicAuthorizationSaveRequest oAuth2Authorization = (BasicAuthorizationSaveRequest) authorization;
        decodeParameter(oAuth2Authorization::getPassword, oAuth2Authorization::setPassword);
        log.debug("Password has been successfully decoded");
    }

    @Override
    public void decryptParameters(AuthorizationSaveRequest authorization) {
        log.debug("Decrypt authorization parameters");
        final BasicAuthorizationSaveRequest basicAuthSaveRequest = (BasicAuthorizationSaveRequest) authorization;
        decryptParameter(basicAuthSaveRequest::getUsername, basicAuthSaveRequest::setUsername);
        log.debug("Password has been successfully decrypted");
        decryptParameter(basicAuthSaveRequest::getPassword, basicAuthSaveRequest::setPassword);
        log.debug("Client secret has been successfully decrypted");
    }

    @Override
    public void encryptParameters(AuthorizationSaveRequest authorization) {
        log.debug("Encrypt authorization parameters");
        final BasicAuthorizationSaveRequest basicAuthSaveRequest = (BasicAuthorizationSaveRequest) authorization;
        encryptParameter(basicAuthSaveRequest::getUsername, basicAuthSaveRequest::setUsername);
        log.debug("Password has been successfully encrypted");
        encryptParameter(basicAuthSaveRequest::getPassword, basicAuthSaveRequest::setPassword);
        log.debug("Client secret has been successfully encrypted");
    }

    @Override
    public OAuth2AuthrizationResponse performAuthorization(UUID projectId, String url,
                                                           MultiValueMap<String, String> map) {
        return null;
    }

    @Override
    public RequestAuthorizationType getAuthorizationType() {
        return BASIC;
    }

    @Override
    public RequestAuthorization parseAuthorizationFromMap(Map<String, String> authorizationInfo) {
        BasicRequestAuthorization basicRequestAuthorization = new BasicRequestAuthorization();
        basicRequestAuthorization.setType(BASIC);

        if (authorizationInfo.containsKey(Constants.USERNAME)) {
            basicRequestAuthorization.setUsername(authorizationInfo.get(Constants.USERNAME));
        }

        if (authorizationInfo.containsKey(Constants.PASSWORD)) {
            basicRequestAuthorization.setPassword(authorizationInfo.get(Constants.PASSWORD));
        }

        return basicRequestAuthorization;
    }

    @Override
    public RequestHeader generateAuthorizationHeader(RequestAuthorization authorization) {
        BasicAuthorizationSaveRequest basicAuthorization =
                (BasicAuthorizationSaveRequest) AuthorizationUtils.castToAuthorizationSaveRequest(authorization);
        final String headerValue = generateAuthHeaderValue(basicAuthorization);
        return new RequestHeader(null, AUTH_HEADER_KEY, headerValue, "", false, true);
    }

}
