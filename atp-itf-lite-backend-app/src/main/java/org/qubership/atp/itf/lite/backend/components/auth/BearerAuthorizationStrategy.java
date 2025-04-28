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

import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyRequest;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyResponse;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.BearerAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.auth.OAuth2AuthrizationResponse;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BearerRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

@Component
public class BearerAuthorizationStrategy extends AbstractAuthorizationStrategy implements RequestAuthorizationStrategy {

    private static final String BEARER_PREFIX = "Bearer ";

    public BearerAuthorizationStrategy(EncryptionService encryptionService) {
        super(encryptionService);
    }

    @Override
    public AuthorizationStrategyResponse getAuthorizationToken(
            AuthorizationStrategyRequest request)
            throws AtpDecryptException {
        final BearerAuthorizationSaveRequest unsafeBearerAuthorization =
                (BearerAuthorizationSaveRequest) request.getUnsafeAuthorizationRequest();
        BearerAuthorizationSaveRequest safeBearerAuthorization =
                (BearerAuthorizationSaveRequest) request.getSafeAuthorizationRequest();
        return new AuthorizationStrategyResponse(getBearerToken(unsafeBearerAuthorization),
                getBearerToken(safeBearerAuthorization));
    }

    @Override
    public void encryptParameters(AuthorizationSaveRequest requestAuthorization) {
    }

    @Override
    public void decryptParameters(AuthorizationSaveRequest requestAuthorization) {
    }

    @Override
    public OAuth2AuthrizationResponse performAuthorization(UUID projectId, String url,
                                                           MultiValueMap<String, String> map) {
        return null;
    }

    @Override
    public RequestAuthorizationType getAuthorizationType() {
        return RequestAuthorizationType.BEARER;
    }

    @Override
    public RequestAuthorization parseAuthorizationFromMap(Map<String, String> authorizationInfo) {
        BearerRequestAuthorization bearerRequestAuthorization = new BearerRequestAuthorization();
        bearerRequestAuthorization.setType(RequestAuthorizationType.BEARER);
        if (authorizationInfo.containsKey(Constants.TOKEN)) {
            bearerRequestAuthorization.setToken(authorizationInfo.get(Constants.TOKEN));
        }
        if (bearerRequestAuthorization.getToken() == null
                || bearerRequestAuthorization.getToken().equals("")) {
            return null;
        }
        return bearerRequestAuthorization;
    }

    private String getBearerToken(BearerAuthorizationSaveRequest bearerAuthorizationSaveRequest) {
        if (bearerAuthorizationSaveRequest != null) {
            return BEARER_PREFIX + bearerAuthorizationSaveRequest.getToken();
        }
        return null;
    }

    /**
     * Generates a header to be displayed on the UI.
     * @param authorization request authorization
     * @return {@link RequestHeader} generated request header
     */
    @Nullable
    public RequestHeader generateAuthorizationHeader(RequestAuthorization authorization) {
        BearerAuthorizationSaveRequest bearerAuthorization =
                (BearerAuthorizationSaveRequest) AuthorizationUtils.castToAuthorizationSaveRequest(authorization);
        final String headerValue = getBearerToken(bearerAuthorization);
        return new RequestHeader(null, AUTH_HEADER_KEY, headerValue, "", false, true);
    }
}
