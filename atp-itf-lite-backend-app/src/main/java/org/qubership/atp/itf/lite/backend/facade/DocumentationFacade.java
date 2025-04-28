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

package org.qubership.atp.itf.lite.backend.facade;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderTreeSearchRequest;
import org.qubership.atp.itf.lite.backend.model.documentation.AbstractDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.RequestDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.RequestEntityEditDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.TreeFolderDocumentationResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.service.DocumentationService;
import org.qubership.atp.itf.lite.backend.service.RequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class DocumentationFacade {

    private final RequestService requestService;
    private final DocumentationService documentationService;
    private final ModelMapper modelMapper;


    public ResponseEntity<TreeFolderDocumentationResponse> getDocumentationByFolder(FolderTreeSearchRequest request,
                                                                                    int page, int pageSize) {
        return ResponseEntity.ok(documentationService.getDescription(request, page, pageSize));
    }

    public ResponseEntity<RequestDocumentation> getRequestDocumentation(UUID requestId) {
        Request temporaryRequest = requestService.getRequest(requestId);
        return ResponseEntity.ok(modelMapper.map(temporaryRequest, RequestDocumentation.class));
    }

    /**
     * Edit documentation by type itf-lite entity.
     */
    public ResponseEntity<AbstractDocumentation> editDocumentation(UUID entityId,
                                                                   RequestEntityEditDocumentation request) {
        switch (request.getType()) {
            case REQUEST:
                return ResponseEntity.ok(documentationService.editDocumentationRequest(entityId, request));
            case FOLDER:
                return ResponseEntity.ok(documentationService.editDocumentationFolder(entityId, request));
            default:
                log.error("Not Correctly type for edit documentation: {}", request.getType());
                return null;
        }
    }
}
