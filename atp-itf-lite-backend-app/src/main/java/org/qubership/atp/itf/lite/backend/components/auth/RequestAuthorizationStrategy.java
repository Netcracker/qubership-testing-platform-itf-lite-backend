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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyRequest;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyResponse;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.auth.OAuth2AuthrizationResponse;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;
import org.springframework.util.MultiValueMap;

public interface RequestAuthorizationStrategy {

    AuthorizationStrategyResponse getAuthorizationToken(AuthorizationStrategyRequest authorizationStrategyRequest)
            throws AtpDecryptException;

    void encryptParameters(AuthorizationSaveRequest requestAuthorization);

    void decryptParameters(AuthorizationSaveRequest requestAuthorization);

    OAuth2AuthrizationResponse performAuthorization(UUID projectId, String url, MultiValueMap<String, String> map);

    RequestAuthorizationType getAuthorizationType();

    RequestAuthorization parseAuthorizationFromMap(Map<String, String> authorizationInfo);

    /**
     * Generates a header to be displayed on the UI.
     * @param authorization request authorization
     * @return {@link RequestHeader} generated request header
     */
    @Nullable
    RequestHeader generateAuthorizationHeader(RequestAuthorization authorization);

    /**
     * Generates a params to be displayed on the UI.
     * @param authorization request authorization
     * @return {@link RequestParam} generated request params
     */
    default List<RequestParam> generateAuthorizationParams(RequestAuthorization authorization) {
        return Collections.emptyList();
    }
}