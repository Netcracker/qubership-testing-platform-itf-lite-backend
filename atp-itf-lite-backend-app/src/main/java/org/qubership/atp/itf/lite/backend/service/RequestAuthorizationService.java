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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;
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
     * Process authorization for the provided request.
     *
     * @param httpRequest http request
     */
    public String processRequestAuthorization(UUID projectId,
                                              HttpRequestEntitySaveRequest httpRequest,
                                              HttpRequestEntitySaveRequest httpHistoryRequest,
                                              UUID environmentId, Evaluator evaluator,
                                              SaveRequestResolvingContext resolvingContext) throws AtpDecryptException,
            JsonProcessingException {
        log.debug("Processing authorization for the request: '{}'", httpRequest);
        AuthorizationSaveRequest unsafeAuthorization = httpRequest.getAuthorization();
        if (nonNull(unsafeAuthorization)) {
            String url = httpRequest.getUrl();
            HttpMethod method = httpRequest.getHttpMethod();
            AuthorizationStrategyRequest authorizationStrategyRequest = AuthorizationUtils.createAuthStrategyRequest(
                    unsafeAuthorization, evaluator, resolvingContext, projectId, environmentId, url, method);
            RequestAuthorizationType type = unsafeAuthorization.getType();
            log.debug("Found request authorization with type: {}", type);
            RequestAuthorizationStrategy authorizationStrategy = registry.getRequestAuthorizationStrategy(type);
            AuthorizationStrategyResponse strategyResponse = authorizationStrategy
                    .getAuthorizationToken(authorizationStrategyRequest);
            if (nonNull(strategyResponse)) {

                final String unsafeAuthorizationToken = strategyResponse.getUnsafeAuthorizationToken();
                if (nonNull(unsafeAuthorizationToken)) {
                    requestHeadersProcessing(httpRequest, unsafeAuthorizationToken);
                }

                final String safeAuthorizationToken = strategyResponse.getSafeAuthorizationToken();
                if (nonNull(safeAuthorizationToken)) {
                    requestHeadersProcessing(httpHistoryRequest, safeAuthorizationToken);
                }

                final Map<String, String> authorizationParams = strategyResponse.getAuthorizationParams();
                if (nonNull(authorizationParams)) {
                    requestParamsProcessing(httpRequest, authorizationParams);
                }

                return unsafeAuthorizationToken;
            }
        } else {
            log.debug("Request hasn't configured authorization");
        }
        return null;
    }

    private void requestParamsProcessing(HttpRequestEntitySaveRequest request, Map<String, String> authParams) {
        List<HttpParamSaveRequest> requestParams = request.getRequestParams();
        authParams.forEach((key, value) -> {
            requestParams.add(new HttpParamSaveRequest(key, value, "", false));
        });
    }

    private void requestHeadersProcessing(HttpRequestEntitySaveRequest request, String authorizationHeaderValue) {
        if (nonNull(request)) {
            List<HttpHeaderSaveRequest> requestHeaders = request.getRequestHeaders();
            Map<String, List<HttpHeaderSaveRequest>> headersMap = requestHeaders.stream()
                    .collect(Collectors.groupingBy(HttpHeaderSaveRequest::getKey, Collectors.toList()));
            requestHeadersProcessing(headersMap, authorizationHeaderValue, requestHeaders);
        }
    }

    private void requestHeadersProcessing(Map<String, List<HttpHeaderSaveRequest>> headersMap,
                                          String authorizationHeaderValue, List<HttpHeaderSaveRequest> requestHeaders) {
        if (!headersMap.containsKey(HttpHeaders.AUTHORIZATION)) {
            requestHeaders.add(new HttpHeaderSaveRequest(
                    HttpHeaders.AUTHORIZATION, authorizationHeaderValue, "", false, true));
        }
    }

    /**
     * Encrypt authorization parameters.
     *
     * @param authorization request authorization
     */
    public void encryptAuthorizationParameters(AuthorizationSaveRequest authorization) {
        RequestAuthorizationType type = authorization.getType();
        log.debug("Found request authorization with type: {}", type);
        RequestAuthorizationStrategy authorizationStrategy = registry.getRequestAuthorizationStrategy(type);
        log.debug("Trying to encrypt authorization parameters");
        authorizationStrategy.encryptParameters(authorization);
    }

    /**
     * Parse authorization parameters from postman collection's request.
     * @param auth json object with attribute "type" != null/empty
     * @return request authorization
     */
    public RequestAuthorization parseAuthorizationFromMap(Map<String, String> auth, RequestAuthorizationType type) {
        RequestAuthorizationStrategy authorizationStrategy = registry.getRequestAuthorizationStrategy(type);
        return authorizationStrategy.parseAuthorizationFromMap(auth);
    }

    /**
     * Generates a header to be displayed on the UI.
     * @param authorization request authorization
     * @return {@link RequestHeader} generated request header
     */
    @Nullable
    public RequestHeader generateAuthorizationHeader(RequestAuthorization authorization) {
        RequestAuthorizationType type = authorization.getType();
        if (isNull(type)) {
            log.warn("RequestAuthorizationType not sets. Generating authorization header skipped");
            return null;
        }
        RequestAuthorizationStrategy authorizationStrategy = registry.getRequestAuthorizationStrategy(type);
        return authorizationStrategy.generateAuthorizationHeader(authorization);
    }

    /**
     * Generates a params to be displayed on the UI.
     * @param authorization request authorization
     * @return {@link RequestHeader} generated request params
     */
    @Nullable
    public List<RequestParam> generateAuthorizationParams(RequestAuthorization authorization) {
        RequestAuthorizationType type = authorization.getType();
        if (isNull(type)) {
            log.warn("RequestAuthorizationType not sets. Generating authorization params skipped");
            return null;
        }
        RequestAuthorizationStrategy authorizationStrategy = registry.getRequestAuthorizationStrategy(type);
        return authorizationStrategy.generateAuthorizationParams(authorization);
    }
}
