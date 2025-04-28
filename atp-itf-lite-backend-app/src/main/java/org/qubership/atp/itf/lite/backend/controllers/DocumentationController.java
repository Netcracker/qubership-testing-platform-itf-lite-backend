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

package org.qubership.atp.itf.lite.backend.controllers;

import java.util.UUID;

import javax.validation.Valid;

import org.qubership.atp.itf.lite.backend.facade.DocumentationFacade;
import org.qubership.atp.itf.lite.backend.model.api.ApiPath;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderTreeSearchRequest;
import org.qubership.atp.itf.lite.backend.model.documentation.AbstractDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.RequestDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.RequestEntityEditDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.TreeFolderDocumentationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping(ApiPath.SERVICE_API_V1_PATH + ApiPath.DOCUMENTATION_PATH)
@AllArgsConstructor
public class DocumentationController {

    private final DocumentationFacade documentationFacade;

    /**
     * Get lazy tree doc.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),'READ')")
    @PostMapping(value = ApiPath.FOLDER_PATH)
    public ResponseEntity<TreeFolderDocumentationResponse> getFolderRequestsTree(
            @RequestParam int page,
            @RequestParam int pageSize,
            @RequestBody @Valid FolderTreeSearchRequest request) {
        return documentationFacade.getDocumentationByFolder(request, page, pageSize);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "@requestService.getRequest(#requestId).getProjectId(),'READ')")
    @GetMapping(value = ApiPath.REQUEST_PATH + ApiPath.REQUEST_ID_PATH)
    public ResponseEntity<RequestDocumentation> getRequest(@PathVariable(ApiPath.REQUEST_ID) UUID requestId) {
        return documentationFacade.getRequestDocumentation(requestId);
    }

    @PreAuthorize("T(org.qubership.atp.itf.lite.backend.enums.EntityType).REQUEST.equals(#request.getType()) ? "
            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),"
            + "@requestService.get(#id).getPermissionFolderId(),"
            + "'UPDATE') and @entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(), "
            + "#request.getProjectId(), "
            + "'UPDATE')"
            + ": @entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),"
            + "@folderService.getFolder(#id).getPermissionFolderId(),"
            + "'UPDATE')")
    @PatchMapping(value = ApiPath.EDIT_PATH + ApiPath.ID_PATH)
    public ResponseEntity<AbstractDocumentation> editDocumentation(
            @PathVariable(ApiPath.ID) UUID id, @RequestBody @Valid RequestEntityEditDocumentation request) {
        return documentationFacade.editDocumentation(id, request);
    }
}
