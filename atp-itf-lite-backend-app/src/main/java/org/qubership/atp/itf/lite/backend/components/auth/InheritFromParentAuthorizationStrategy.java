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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.exceptions.ItfLiteException;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyRequest;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyResponse;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationResolvingContext;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.InheritFromParentAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.auth.OAuth2AuthrizationResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.auth.InheritFromParentRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.qubership.atp.itf.lite.backend.service.FolderService;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InheritFromParentAuthorizationStrategy extends AbstractAuthorizationStrategy
        implements RequestAuthorizationStrategy {

    private FolderService folderService;
    private RequestAuthorizationRegistry requestAuthorizationRegistry;

    public InheritFromParentAuthorizationStrategy(EncryptionService encryptionService) {
        super(encryptionService);
    }

    // WA to avoid cyclic dependency
    // InheritFromParentAuthorizationStrategy -> RequestAuthorizationRegistry -> InheritFromParentAuthorizationStrategy
    @Autowired
    public void setRequestAuthorizationRegistry(@Lazy RequestAuthorizationRegistry requestAuthorizationRegistry) {
        this.requestAuthorizationRegistry = requestAuthorizationRegistry;
    }

    // WA to avoid cyclic dependency
    // InheritFromParentAuthorizationStrategy -> RequestAuthorizationRegistry -> InheritFromParentAuthorizationStrategy
    @Autowired
    public void setFolderService(@Lazy FolderService folderService) {
        this.folderService = folderService;
    }

    @Override
    public AuthorizationStrategyResponse getAuthorizationToken(
            AuthorizationStrategyRequest authorizationStrategyRequest) throws AtpDecryptException {
        final InheritFromParentAuthorizationSaveRequest inheritFromParentAuthorizationSaveRequest =
                (InheritFromParentAuthorizationSaveRequest) authorizationStrategyRequest
                        .getUnsafeAuthorizationRequest();
        if (nonNull(inheritFromParentAuthorizationSaveRequest.getAuthorizationFolderId())) {
            final UUID authorizationFolderId = inheritFromParentAuthorizationSaveRequest.getAuthorizationFolderId();
            final Folder authFolder = folderService.getFolder(authorizationFolderId);
            RequestAuthorization authorization = authFolder.getAuthorization();
            if (nonNull(authorization)) {
                AuthorizationSaveRequest newAuthSaveRequest =
                        AuthorizationUtils.castToAuthorizationSaveRequest(authorization);
                try {
                    AuthorizationResolvingContext authResolvingContext =
                            authorizationStrategyRequest.getAuthResolvingContext();
                    AuthorizationStrategyRequest newAuthStrategyRequest = AuthorizationUtils.createAuthStrategyRequest(
                            newAuthSaveRequest, authorizationStrategyRequest.getEvaluator(),
                            authorizationStrategyRequest.getResolvingContext(),
                            authorizationStrategyRequest.getProjectId(),
                            authorizationStrategyRequest.getEnvironmentId(),
                            nonNull(authResolvingContext) ? authResolvingContext.getUrl() : null,
                            nonNull(authResolvingContext) ? authResolvingContext.getHttpMethod() : null);
                    return requestAuthorizationRegistry.getRequestAuthorizationStrategy(authorization.getType())
                            .getAuthorizationToken(newAuthStrategyRequest);
                } catch (JsonProcessingException ex) {
                    log.error("Failed to create authorizationStrategyRequest", ex);
                    throw new ItfLiteException("Failed to evaluate authorization");
                }
            }
        }
        // no auth
        return null;
    }

    @Override
    public void encryptParameters(AuthorizationSaveRequest requestAuthorization) {
    }

    @Override
    public void decryptParameters(AuthorizationSaveRequest requestAuthorization) {
    }

    @Override
    public OAuth2AuthrizationResponse performAuthorization(UUID projectId,
                                                           String url,
                                                           MultiValueMap<String, String> map) {
        return null;
    }

    @Override
    public RequestAuthorizationType getAuthorizationType() {
        return RequestAuthorizationType.INHERIT_FROM_PARENT;
    }

    @Override
    public RequestAuthorization parseAuthorizationFromMap(Map<String, String> authorizationInfo) {
        InheritFromParentRequestAuthorization authRequest = new InheritFromParentRequestAuthorization();
        authRequest.setType(RequestAuthorizationType.INHERIT_FROM_PARENT);
        return authRequest;
    }

    /**
     * Generates a header to be displayed on the UI.
     * @param authorization request authorization
     * @return {@link RequestHeader} generated request header
     */
    @Nullable
    public RequestHeader generateAuthorizationHeader(RequestAuthorization authorization) {
        RequestAuthorization parentAuthorization = getParentAuthorization(authorization);
        if (nonNull(parentAuthorization) && nonNull(parentAuthorization.getType())) {
            return requestAuthorizationRegistry.getRequestAuthorizationStrategy(parentAuthorization.getType())
                    .generateAuthorizationHeader(parentAuthorization);
        } else {
            log.warn("Parent folder not contains authorization or auth type. Authorization header not generated");
            return null;
        }
    }

    @Override
    public List<RequestParam> generateAuthorizationParams(RequestAuthorization authorization) {
        RequestAuthorization parentAuthorization = getParentAuthorization(authorization);
        if (nonNull(parentAuthorization) && nonNull(parentAuthorization.getType())) {
            return requestAuthorizationRegistry.getRequestAuthorizationStrategy(parentAuthorization.getType())
                    .generateAuthorizationParams(parentAuthorization);
        } else {
            log.warn("Parent folder not contains authorization or auth type. Authorization header not generated");
            return null;
        }
    }

    private RequestAuthorization getParentAuthorization(RequestAuthorization authorization) {
        InheritFromParentAuthorizationSaveRequest inheritAuth = (InheritFromParentAuthorizationSaveRequest)
                AuthorizationUtils.castToAuthorizationSaveRequest(authorization);
        UUID authorizationFolderId = inheritAuth.getAuthorizationFolderId();
        if (isNull(authorizationFolderId)) {
            log.debug("Parent folder not set. Authorization header not generated");
            return null;
        }
        final Folder authFolder = folderService.getFolder(authorizationFolderId);

        return authFolder.getAuthorization();
    }
}
