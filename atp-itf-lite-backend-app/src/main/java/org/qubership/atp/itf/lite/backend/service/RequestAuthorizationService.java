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

package org.qubership.atp.itf.lite.backend.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.components.auth.RequestAuthorizationRegistry;
import org.qubership.atp.itf.lite.backend.components.auth.RequestAuthorizationStrategy;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyRequest;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyResponse;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpParamSaveRequest;
import org.qubership.atp.itf.lite.backend.model.context.SaveRequestResolvingContext;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;
import org.qubership.atp.macros.core.processor.Evaluator;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestAuthorizationService {

    private final RequestAuthorizationRegistry registry;

    /**
     * Processes and applies authorization to the current request and its history copy. Applies unsafe token to the
     * current request, safe token to the history request, and adds authorization parameters if available.
     *
     * @param projectId          the project ID
     * @param httpRequest        the current request
     * @param httpHistoryRequest the historical copy of the request
     * @param environmentId      the environment ID
     * @param evaluator          the evaluator for dynamic values
     * @param resolvingContext   the context for resolving variables
     * @return the unsafe authorization token, or null if not available
     * @throws AtpDecryptException     if decryption fails
     * @throws JsonProcessingException if JSON processing fails
     */
    public String processRequestAuthorization(UUID projectId, HttpRequestEntitySaveRequest httpRequest,
                                              HttpRequestEntitySaveRequest httpHistoryRequest, UUID environmentId,
                                              Evaluator evaluator, SaveRequestResolvingContext resolvingContext)
            throws AtpDecryptException, JsonProcessingException {
        log.debug("Processing authorization for the request: '{}'", httpRequest);
        AuthorizationSaveRequest unsafeAuthorization = httpRequest.getAuthorization();
        if (unsafeAuthorization == null) {
            log.debug("Request hasn't configured authorization");
            return null;
        }
        AuthorizationStrategyRequest strategyRequest = AuthorizationUtils.createAuthStrategyRequest(
                unsafeAuthorization, evaluator, resolvingContext, projectId, environmentId, httpRequest.getUrl(),
                httpRequest.getHttpMethod());
        RequestAuthorizationStrategy strategy = registry.getRequestAuthorizationStrategy(unsafeAuthorization.getType());
        log.debug("Found request authorization with type: {}", strategy.getAuthorizationType());
        AuthorizationStrategyResponse strategyResponse = strategy.getAuthorizationToken(strategyRequest);
        if (strategyResponse == null) {
            return null;
        }
        String requestAuthorizationToken = strategyResponse.getUnsafeAuthorizationToken();
        if (requestAuthorizationToken != null && !requestAuthorizationToken.isEmpty()) {
            updateAuthorizationHeader(httpRequest, requestAuthorizationToken);
            log.debug("Applied unsafe authorization token to request headers");
        }
        String safeAuthorizationToken = strategyResponse.getSafeAuthorizationToken();
        if (safeAuthorizationToken != null && !safeAuthorizationToken.isEmpty()) {
            updateAuthorizationHeader(httpHistoryRequest, safeAuthorizationToken);
            log.debug("Applied safe authorization token to history headers");
        }
        Map<String, String> authorizationParams = strategyResponse.getAuthorizationParams();

        if (authorizationParams != null && !authorizationParams.isEmpty()) {
            addAuthorizationParamsToRequest(httpRequest, authorizationParams);
            log.debug("Added {} authorization params to request", authorizationParams.size());
        }

        return requestAuthorizationToken;
    }

    /**
     * Adds the given authorization parameters to the request. Existing parameters are not removed or overwritten.
     *
     * @param request    the HTTP request to update
     * @param authParams the authorization parameters to add
     */
    private void addAuthorizationParamsToRequest(HttpRequestEntitySaveRequest request, Map<String, String> authParams) {
        if (request == null || authParams == null || authParams.isEmpty()) {
            return;
        }
        List<HttpParamSaveRequest> requestParams = request.getRequestParams();
        authParams.forEach((key, value) ->
                requestParams.add(new HttpParamSaveRequest(key, value, "", false)));
    }

    /**
     * Updates the request with the given authorization header. Removes any existing Authorization header before
     * adding the new one.
     *
     * @param request                  the HTTP request to update
     * @param authorizationHeaderValue the new Authorization header value
     */
    private void updateAuthorizationHeader(HttpRequestEntitySaveRequest request, String authorizationHeaderValue) {
        if (request == null || authorizationHeaderValue == null || authorizationHeaderValue.isEmpty()) {
            return;
        }
        List<HttpHeaderSaveRequest> requestHeaders = request.getRequestHeaders();
        requestHeaders.removeIf(header -> HttpHeaders.AUTHORIZATION.equalsIgnoreCase(header.getKey()));
        requestHeaders.add(new HttpHeaderSaveRequest(HttpHeaders.AUTHORIZATION, authorizationHeaderValue.trim(),
                "", false, true));
    }

    /**
     * Encrypts authorization parameters using the appropriate strategy.
     *
     * @param authorization the request authorization
     */
    public void encryptAuthorizationParameters(AuthorizationSaveRequest authorization) {
        RequestAuthorizationType type = authorization.getType();
        log.debug("Found request authorization with type: {}", type);
        RequestAuthorizationStrategy authorizationStrategy = registry.getRequestAuthorizationStrategy(type);
        log.debug("Encrypting authorization parameters");
        authorizationStrategy.encryptParameters(authorization);
    }

    /**
     * Parses authorization parameters from a Postman-style map.
     *
     * @param auth the map containing authorization data
     * @param type the authorization type
     * @return the parsed request authorization
     */
    public RequestAuthorization parseAuthorizationFromMap(Map<String, String> auth, RequestAuthorizationType type) {
        RequestAuthorizationStrategy authorizationStrategy = registry.getRequestAuthorizationStrategy(type);
        return authorizationStrategy.parseAuthorizationFromMap(auth);
    }

    /**
     * Generates an authorization header for display.
     *
     * @param authorization the request authorization
     * @return the generated request header, or null if type is missing
     */
    @Nullable
    public RequestHeader generateAuthorizationHeader(RequestAuthorization authorization) {
        RequestAuthorizationStrategy strategy = resolveStrategy(authorization, "authorization header");
        return strategy != null ? strategy.generateAuthorizationHeader(authorization) : null;
    }

    /**
     * Generates authorization parameters for display.
     *
     * @param authorization the request authorization
     * @return the generated request parameters, or null if type is missing
     */
    @Nullable
    public List<RequestParam> generateAuthorizationParams(RequestAuthorization authorization) {
        RequestAuthorizationStrategy strategy = resolveStrategy(authorization, "authorization params");
        return strategy != null ? strategy.generateAuthorizationParams(authorization) : null;
    }

    /**
     * Resolves the strategy for the given authorization. Logs a warning and returns null if the type is missing.
     *
     * @param authorization the request authorization
     * @param context the context used for logging
     * @return the resolved strategy, or null if type is missing
     */
    private RequestAuthorizationStrategy resolveStrategy(RequestAuthorization authorization, String context) {
        if (authorization == null || authorization.getType() == null) {
            log.warn("Authorization or type is null. Skipping {} generation", context);
            return null;
        }
        return registry.getRequestAuthorizationStrategy(authorization.getType());
    }
}
