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

package org.qubership.atp.itf.lite.backend.utils;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationResolvingContext;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.BasicAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.BearerAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.InheritFromParentAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth1AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth2AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.context.SaveRequestResolvingContext;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BasicRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BearerRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.InheritFromParentRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth2RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;
import org.qubership.atp.itf.lite.backend.service.TemplateResolverService;
import org.qubership.atp.macros.core.processor.Evaluator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class AuthorizationUtils {

    @Setter
    private static ObjectMapper objectMapper;
    @Setter
    private static TemplateResolverService templateResolverService;
    @Setter
    private static ModelMapper modelMapper;

    /**
     * Creates request for authorization strategy.
     *
     * @param authorization authorization
     * @param evaluator macros evaluator
     * @param resolvingContext context variables
     * @param projectId project id
     * @param environmentId env id
     * @return request for authorization strategy
     */
    public static AuthorizationStrategyRequest createAuthStrategyRequest(AuthorizationSaveRequest authorization,
                                                                         Evaluator evaluator,
                                                                         SaveRequestResolvingContext resolvingContext,
                                                                         UUID projectId, UUID environmentId,
                                                                         String authUrl, HttpMethod httpMethod)
            throws JsonProcessingException {
        templateResolverService.resolveTemplatesWithOrder(authorization, resolvingContext, evaluator);
        AuthorizationSaveRequest safeAuthorization = objectMapper.readValue(
                objectMapper.writeValueAsString(authorization),
                authorization.getClass());
        templateResolverService.processEncryptedValues(authorization, false);
        templateResolverService.processEncryptedValues(safeAuthorization, true);
        AuthorizationResolvingContext authResolvingContext = new AuthorizationResolvingContext(authUrl, httpMethod);
        return new AuthorizationStrategyRequest(safeAuthorization, authorization, authResolvingContext, projectId,
                environmentId, evaluator, resolvingContext);
    }

    public static AuthorizationSaveRequest castToAuthorizationSaveRequest(RequestAuthorization requestAuthorization) {
        return modelMapper.map(requestAuthorization, getAuthorizationSaveRequestClassByAuthorizationRequest(
                requestAuthorization));
    }

    private static Class<? extends AuthorizationSaveRequest> getAuthorizationSaveRequestClassByAuthorizationRequest(
            RequestAuthorization authorization) {
        RequestAuthorizationType type = authorization.getType();
        switch (type) {
            case BEARER:
                return BearerAuthorizationSaveRequest.class;
            case OAUTH1:
                return OAuth1AuthorizationSaveRequest.class;
            case OAUTH2:
                return OAuth2AuthorizationSaveRequest.class;
            case INHERIT_FROM_PARENT:
                return InheritFromParentAuthorizationSaveRequest.class;
            case BASIC:
                return BasicAuthorizationSaveRequest.class;
            default:
                log.error("Failed to get AuthorizationSaveRequest class by AuthorizationRequestType {}", type);
                throw new IllegalArgumentException(
                        "Failed to get AuthorizationSaveRequest class by AuthorizationRequestType");
        }
    }

    /**
     * Define RequestAuthorization class according to the AuthorizationSaveRequest.
     */
    public static Class<? extends RequestAuthorization> getRequestAuthorizationClassByAuthorizationSaveRequest(
            AuthorizationSaveRequest authorization) {
        RequestAuthorizationType type = authorization.getType();
        switch (type) {
            case BEARER:
                return BearerRequestAuthorization.class;
            case OAUTH2:
                return OAuth2RequestAuthorization.class;
            case INHERIT_FROM_PARENT:
                return InheritFromParentRequestAuthorization.class;
            case BASIC:
                return BasicRequestAuthorization.class;
            default:
                log.error("Failed to get RequestAuthorization class by AuthorizationSaveRequest type {}",
                        type);
                throw new IllegalArgumentException(
                        "Failed to get RequestAuthorization class by AuthorizationRequestType");
        }
    }
}
