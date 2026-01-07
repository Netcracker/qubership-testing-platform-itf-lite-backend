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

import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.COLLECTION_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.EXECUTE_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.IMPORT_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SERVICE_API_V1_PATH;

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.itf.lite.backend.model.api.request.CollectionExecuteRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.ImportCollectionsRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.ImportCollectionsResponse;
import org.qubership.atp.itf.lite.backend.service.CollectionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

//import clover.com.google.common.net.HttpHeaders;
import org.springframework.http.HttpHeaders;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping(SERVICE_API_V1_PATH + COLLECTION_PATH)
@AllArgsConstructor
public class CollectionController {

    private final CollectionsService collectionsService;

    @AuditAction(auditAction = "Import collection {{#requestEntity.name}} to project {{#requestEntity.projectId}}")
    @PreAuthorize("#requestEntity.getTargetFolderId() != null ? @entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#requestEntity.getProjectId(),"
            + "@folderService.getFolder(#requestEntity.getTargetFolderId()).getPermissionFolderId(), 'UPDATE') : true "
            + "and @entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#requestEntity.getProjectId(),'CREATE') and "
            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#requestEntity.getProjectId(),'CREATE')")
    @PostMapping(value = IMPORT_PATH)
    public ResponseEntity<List<ImportCollectionsResponse>> importCollections(
            @RequestPart(name = "file") MultipartFile collections,
            @RequestPart(name = "requestEntity") @Valid ImportCollectionsRequest requestEntity) {
        return ResponseEntity.ok(collectionsService.importCollections(collections, requestEntity));
    }

    @PreAuthorize("@entityAccess.checkAccess(#request.getProjectId(),'EXECUTE')")
    @PostMapping(value = EXECUTE_PATH)
    public ResponseEntity<List<UUID>> executeCollection(@RequestHeader(value = HttpHeaders.AUTHORIZATION) String token,
                                                        @RequestBody CollectionExecuteRequest request) {
        return ResponseEntity.ok(collectionsService.executeCollection(token, request));
    }
}
