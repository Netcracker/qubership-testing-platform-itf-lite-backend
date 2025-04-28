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

import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.FOLDERS_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SERVICE_API_V1_PATH;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.itf.lite.backend.dataaccess.validators.FolderCreationRequestValidator;
import org.qubership.atp.itf.lite.backend.model.api.ApiPath;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderCopyRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderDeleteRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderEditRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderMoveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderOrderChangeRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderTreeSearchRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderUpsetRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.Settings;
import org.qubership.atp.itf.lite.backend.model.api.response.GroupResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.service.ConcurrentModificationService;
import org.qubership.atp.itf.lite.backend.service.FolderService;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping(SERVICE_API_V1_PATH + FOLDERS_PATH)
@AllArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final FolderCreationRequestValidator folderCreationRequestValidator;
    private final ConcurrentModificationService concurrentModificationService;

    /**
     * Bind validators.
     *
     * @param binder binder
     */
    @InitBinder
    public void dataBindings(WebDataBinder binder) {
        Object target = binder.getTarget();
        if (target != null && FolderUpsetRequest.class.equals(target.getClass())) {
            binder.addValidators(folderCreationRequestValidator);
        }
    }

    /**
     * Get all folders under project.
     *
     * @param projectId project identifier
     * @return number of folders
     */
    @AuditAction(auditAction = "Get all folders. Filters: project id = {{#projectId}}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#projectId,'READ')")
    @GetMapping
    public ResponseEntity<Collection<Folder>> getAllFolders(@RequestParam(required = false) UUID projectId) {
        return ResponseEntity.ok(folderService.getAllFolders(projectId));
    }

    /**
     * Get folder settings.
     *
     * @param folderId folder identifier.
     * @return folder settings
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "@folderService.getFolder(#folderId).getProjectId(), 'READ')")
    @GetMapping(value = ApiPath.ID_PATH + ApiPath.SETTINGS_PATH)
    public ResponseEntity<Settings> getFolderSettings(@PathVariable(ApiPath.ID) UUID folderId) {
        return ResponseEntity.ok(folderService.getSettings(folderId));
    }

    /**
     * Create folder.
     *
     * @param request create folder request entity
     * @return created folder entity
     */
    @AuditAction(auditAction = "Create folder with name '{{#request.name}}' in the '{{#request.projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),"
            + "#request.getParentId() != null ? "
            + "@folderService.getFolder(#request.getParentId()).getPermissionFolderId() : null,'CREATE')")
    @PostMapping
    public ResponseEntity<Folder> createFolder(@RequestBody @Valid FolderUpsetRequest request) throws Exception {
        return new ResponseEntity<>(folderService.createFolder(request), HttpStatus.CREATED);
    }

    /**
     * Update folder.
     *
     * @param request update folder request entity.
     * @return update folder entity
     */
    @AuditAction(auditAction = "Edit folder with name '{{#request.name}}' in the '{{#request.projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(), @folderService.getFolder(#folderId).getPermissionFolderId(), 'UPDATE')")
    @PutMapping(value = ApiPath.ID_PATH)
    public ResponseEntity<Void> editFolder(@PathVariable(ApiPath.ID) UUID folderId,
                                           @RequestBody FolderEditRequest request) throws Exception {
        HttpStatus status = concurrentModificationService.getConcurrentModificationHttpStatus(
                folderId, request.getModifiedWhen(), folderService);
        folderService.editFolder(folderId, request);
        return ResponseEntity.status(status).build();
    }

    /**
     * Copy folder.
     *
     * @param request copy folder request entity.
     * @return operation confirmation status success
     */
    @AuditAction(auditAction = "Copy folders with ids [{#request.ids}}] in the '{{#request.projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),"
            + "#request.getToFolderId() != null ? "
            + "@folderService.getFolder(#request.getToFolderId()).getPermissionFolderId() : null,'CREATE')")
    @PostMapping(value = ApiPath.COPY_PATH)
    public ResponseEntity<Void> copyFolders(@RequestBody @Valid FolderCopyRequest request) {
        folderService.copyFolders(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Move folder.
     *
     * @param request move folder request entity.
     * @return operation confirmation status success
     */
    @AuditAction(auditAction = "Move folders with ids [{#request.ids}}] to folder '{{#request.toFolderId}}' in the "
            + "'{{#request.projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),"
            + "#request.getToFolderId() != null ? "
            + "@folderService.getFolder(#request.getToFolderId()).getPermissionFolderId() : null,'CREATE')")
    @PostMapping(value = ApiPath.MOVE_PATH)
    public ResponseEntity<List<UUID>> moveFolders(@RequestBody @Valid FolderMoveRequest request) {
        Pair<HttpStatus, List<UUID>> concurrentModificationRes =
                concurrentModificationService.getConcurrentModificationHttpStatus(request.getIds(), folderService);
        folderService.moveFolders(request);
        return ResponseEntity.status(concurrentModificationRes.getFirst()).body(concurrentModificationRes.getSecond());
    }

    /**
     * Delete folders.
     *
     * @param request delete folders request
     */
    @AuditAction(auditAction = "Delete folders with ids [{#request.ids}}] in the '{{#request.projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),@folderService.getPermissionFolderIdsByFolderIds(#request.getIds()),'DELETE')")
    @DeleteMapping
    public ResponseEntity<Void> deleteFolders(@RequestBody @Valid FolderDeleteRequest request) {
        folderService.deleteFolders(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Find folders heirs.
     *
     * @param request delete folders request
     */
    @AuditAction(auditAction = "Count heirs for folders with ids [{#request.ids}}] in the "
            + "'{{#request.projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),'READ')")
    @PostMapping(value = ApiPath.COUNT_HEIRS_PATH)
    public ResponseEntity<Long> countFolderHeirs(@RequestBody @Valid FolderDeleteRequest request) {
        return ResponseEntity.ok(folderService.countFolderHeirs(request));
    }


    /**
     * Get all folders and requests for a specific level under a project.
     * Also, should be used for requests search.
     *
     * @param request tree search request
     */
    @AuditAction(auditAction = "Get folder requests tree for the '{{#request.projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),'READ')")
    @PostMapping(value = ApiPath.TREE_PATH)
    public ResponseEntity<GroupResponse> getFolderRequestsTree(
            @RequestParam(required = false, defaultValue = "false") Boolean onlyFolders,
            @RequestBody @Valid FolderTreeSearchRequest request) {
        return ResponseEntity.ok(folderService.getFolderRequestsTree(onlyFolders, request));
    }

    @AuditAction(auditAction = "Change order for the folder '{{#folderId}}' in the '{{#request.projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),@folderService.getFolder(#folderId).getPermissionFolderId(),'UPDATE')")
    @PostMapping(value = ApiPath.ID_PATH + ApiPath.ORDER_PATH)
    public ResponseEntity<Void> order(@PathVariable(ApiPath.ID) UUID folderId,
                                      @RequestBody FolderOrderChangeRequest request) {
        folderService.order(folderId, request);
        return ResponseEntity.ok().build();
    }
}
