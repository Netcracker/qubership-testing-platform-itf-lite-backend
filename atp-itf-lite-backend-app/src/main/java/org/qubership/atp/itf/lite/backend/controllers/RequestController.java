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

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.collections.CollectionUtils;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.itf.lite.backend.exceptions.file.ItfLiteSaveResponseAsFileException;
import org.qubership.atp.itf.lite.backend.feign.service.RamService;
import org.qubership.atp.itf.lite.backend.mdc.ItfLiteMdcField;
import org.qubership.atp.itf.lite.backend.model.api.ApiPath;
import org.qubership.atp.itf.lite.backend.model.api.request.ContextVariable;
import org.qubership.atp.itf.lite.backend.model.api.request.CurlStringImportRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.ExecutionCollectionRequestExecuteRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.ImportContextRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitiesBulkDelete;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitiesCopyRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitiesMoveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityCopyRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityCreateRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityEditRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityMoveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestOrderChangeRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.Settings;
import org.qubership.atp.itf.lite.backend.model.api.response.ContextResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.ImportContextResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExportResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.collections.ExecuteStepResponse;
import org.qubership.atp.itf.lite.backend.model.entities.FileBody;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.service.ActionService;
import org.qubership.atp.itf.lite.backend.service.ConcurrentModificationService;
import org.qubership.atp.itf.lite.backend.service.FormDataPartService;
import org.qubership.atp.itf.lite.backend.service.RequestService;
import org.qubership.atp.itf.lite.backend.service.RequestSnapshotService;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(ApiPath.SERVICE_API_V1_PATH + ApiPath.REQUESTS_PATH)
@AllArgsConstructor
@Slf4j
public class RequestController {

    private final RequestService requestService;
    private final RamService ramService;
    private final ActionService actionService;
    private final FormDataPartService formDataPartService;
    private final ConcurrentModificationService concurrentModificationService;
    private final RequestSnapshotService requestSnapshotService;

    @AuditAction(auditAction = "Get all requests. Filters: project id = {{#projectId}}, folder id = {{#folderId}}")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#projectId,'READ')")
    @GetMapping
    public ResponseEntity<Collection<Request>> getAllRequests(@RequestParam UUID projectId,
                                                              @RequestParam(required = false) UUID folderId) {
        return ResponseEntity.ok(requestService.getAllRequests(projectId, folderId));
    }

    @AuditAction(auditAction = "Create request with name '{{#request.name}}', transport type "
            + "'{{#request.transportType}}' in the '{{#request.projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#request.getProjectId(), 'CREATE') and "
            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),"
            + "#request.getFolderId() != null ? "
            + "@folderService.getFolder(#request.getFolderId()).getPermissionFolderId() : null,'UPDATE')")
    @PostMapping
    public ResponseEntity<Request> createRequest(@RequestBody @Valid RequestEntityCreateRequest request) {
        return new ResponseEntity<>(requestService.createRequest(request), HttpStatus.CREATED);
    }

    /**
     * Get request by request id.
     * @param requestId request id.
     * @return request entity
     */
    @AuditAction(auditAction = "Get request by id '{{#requestId}}'")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#projectId != null ? #projectId : @requestService.getRequest(#requestId).getProjectId(),'READ')")
    @GetMapping(value = ApiPath.REQUEST_ID_PATH)
    public ResponseEntity<Request> getRequest(@RequestParam(required = false) UUID projectId,
                                              @PathVariable(value = ApiPath.REQUEST_ID) UUID requestId) {
        Request request = requestService.getRequest(requestId, projectId);
        if (request instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) request;
            RequestHeader authHeader = requestService.generateAuthorizationHeader(request.getAuthorization());
            if (nonNull(authHeader)) {
                httpRequest.getRequestHeaders().add(0, authHeader);
            }

            List<org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam> requestParams =
                    requestService.generateAuthorizationParams(request.getAuthorization());
            if (!CollectionUtils.isEmpty(requestParams)) {
                httpRequest.getRequestParams().addAll(0, requestParams);
            }
        }
        return ResponseEntity.ok(request);
    }

    @AuditAction(auditAction = "Get request setting by id '{{#requestId}}'")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "@requestService.getRequest(#requestId).getProjectId(),'READ')")
    @GetMapping(value = ApiPath.REQUEST_ID_PATH + ApiPath.SETTINGS_PATH)
    public ResponseEntity<Settings> getRequestSetting(@PathVariable(ApiPath.REQUEST_ID) UUID requestId) {
        return ResponseEntity.ok(requestService.getSettings(requestId));
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "@requestService.getRequest(#requestId).getProjectId(),'READ')")
    @GetMapping(value = ApiPath.REQUEST_ID_PATH + ApiPath.BINARY_PATH)
    public void getRequestBinaryFile(@PathVariable(ApiPath.REQUEST_ID) UUID requestId,
                                             HttpServletResponse response) throws IOException {
       requestService.getRequestBinaryFile(requestId, response);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "@requestService.getRequest(#requestId).getProjectId(), 'CREATE') and "
            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "@requestService.getRequest(#requestId).getProjectId(),"
            + "@requestService.getRequest(#requestId).getPermissionFolderId(),'UPDATE')")
    @PutMapping(value = ApiPath.REQUEST_ID_PATH + ApiPath.BINARY_PATH + ApiPath.UPLOAD_PATH)
    public void uploadBinaryFile(@PathVariable(ApiPath.REQUEST_ID) UUID requestId,
                                 @RequestPart(name = "file") MultipartFile file) {
        requestService.uploadBinaryFile(requestId, file);
    }

    /**
     * Saves request entity and file to file system and grid fs if exists.
     * @param requestId request id
     * @param file dictionary
     * @param requestEntity request entity
     * @return Request
     */
    @AuditAction(auditAction = "Save request with id '{{#requestId}}' and name '{{#requestEntity.name}}'")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#requestEntity.getProjectId(), 'UPDATE') and "
            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#requestEntity.getProjectId(),"
            + "@requestService.getRequest(#requestId).getPermissionFolderId(),'UPDATE')")
    @PutMapping(value = ApiPath.REQUEST_ID_PATH)
    public ResponseEntity<Request> saveRequest(@PathVariable(ApiPath.REQUEST_ID) UUID requestId,
                                               @RequestParam(required = false) UUID sessionId,
                                               @RequestPart(name = "file", required = false) MultipartFile file,
                                               @RequestPart(name = "files", required = false) List<MultipartFile> files,
                                               @RequestPart(name = "requestEntity") @Valid
                                               RequestEntitySaveRequest requestEntity) throws IOException {
        HttpStatus status = concurrentModificationService.getConcurrentModificationHttpStatus(
                requestId, requestEntity.getModifiedWhen(), requestService);
        if (sessionId != null) {
            requestSnapshotService.preSaveRequestProcessing(sessionId, requestId);
        }
        Optional<FileBody> fileInfo = Optional.empty();
        if (file != null) {
            fileInfo = requestService.saveFileToFileSystemAndGridFs(requestId, file, requestEntity.getTransportType());
        }
        return new ResponseEntity<>(requestService.saveRequest(requestId, requestEntity, files, fileInfo), status);
    }

    /**
     * Update request entity configuration.
     * @param requestId request id.
     * @param request body with params for update.
     * @return updated request.
     */
    @AuditAction(auditAction = "Update request with id '{{#requestId}}' and name '{{#requestEntity.name}}'")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#request.getProjectId(), 'UPDATE') and "
            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),"
            + "@requestService.getRequest(#requestId).getPermissionFolderId(),'UPDATE')")
    @PatchMapping(value = ApiPath.REQUEST_ID_PATH)
    public ResponseEntity<Request> editRequest(@PathVariable(ApiPath.REQUEST_ID) UUID requestId,
                                               @RequestBody @Valid RequestEntityEditRequest request) {
        HttpStatus status = concurrentModificationService.getConcurrentModificationHttpStatus(
                requestId, request.getModifiedWhen(), requestService);
        return new ResponseEntity<>(requestService.editRequest(requestId, request), status);
    }

    @AuditAction(auditAction = "Copy request with id '{{#requestId}}'")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#request.getProjectId(), 'CREATE') and "
            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),"
            + "#request.getFolderId() != null ? "
            + "@folderService.getFolder(#request.getFolderId()).getPermissionFolderId() : null,'UPDATE')")
    @PostMapping(value = ApiPath.REQUEST_ID_PATH + ApiPath.COPY_PATH)
    public ResponseEntity<Request> copyRequest(@PathVariable(ApiPath.REQUEST_ID) UUID requestId,
                                               @RequestBody @Valid RequestEntityCopyRequest request) {
        return ResponseEntity.ok(requestService.copyRequest(requestId, request));
    }

    @AuditAction(auditAction = "Copy requests with ids [{{#request.requestIds}}]")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#request.getProjectId(), 'CREATE') and "

            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),"
            + "#request.getFolderId() != null ? "
            + "@folderService.getFolder(#request.getFolderId()).getPermissionFolderId() : null,'UPDATE')")
    @PostMapping(value = ApiPath.COPY_PATH)
    public ResponseEntity<Void> copyRequests(@RequestBody @Valid RequestEntitiesCopyRequest request) {
        requestService.copyRequests(request);
        return ResponseEntity.ok().build();
    }

    @AuditAction(auditAction = "Move request with id '{{#requestId}}'")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#request.getProjectId(), 'UPDATE') and "

            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),"
            + "@requestService.getRequest(#requestId).getPermissionFolderId(),'UPDATE') and "

            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),"
            + "#request.getFolderId() != null ? "
            + "@folderService.getFolder(#request.getFolderId()).getPermissionFolderId() : null,'UPDATE')")
    @PostMapping(value = ApiPath.REQUEST_ID_PATH + ApiPath.MOVE_PATH)
    public ResponseEntity<Request> moveRequest(@PathVariable(ApiPath.REQUEST_ID) UUID requestId,
                                               @RequestBody @Valid RequestEntityMoveRequest request) {
        return ResponseEntity.ok(requestService.moveRequest(requestId, request));
    }

    /**
     * Change parent folder for request.
     * @param request body with list of requests and new parent folder.
     * @return list of request ids.
     */
    @AuditAction(auditAction = "Move requests with ids [{#request.requestIds}}] to folder '{{#request.folderId}}' "
            + "in the '{{#request.projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#request.getProjectId(), "
            + "'UPDATE') and "

            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),"
            + "@requestService.getPermissionFolderIdsByIdsWithModifiedWhen(#request.getRequestIds()),"
            + "'UPDATE') and "

            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),"
            + "#request.getFolderId() != null ? "
            + "@folderService.getFolder(#request.getFolderId()).getPermissionFolderId() : null,"
            + "'UPDATE')")
    @PostMapping(value = ApiPath.MOVE_PATH)
    public ResponseEntity<List<UUID>> moveRequests(
            @RequestBody @Valid RequestEntitiesMoveRequest request) {
        Pair<HttpStatus, List<UUID>> concurrentModificationRes = concurrentModificationService
                .getConcurrentModificationHttpStatus(request.getRequestIds(), requestService);
        requestService.moveRequests(request);
        return ResponseEntity.status(concurrentModificationRes.getFirst()).body(concurrentModificationRes.getSecond());
    }

    @AuditAction(auditAction = "Delete request with id '{{#requestId}}'")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "@requestService.getRequest(#requestId).getProjectId(), 'DELETE') and "
            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "@requestService.getRequest(#requestId).getProjectId(),"
            + "@requestService.getRequest(#requestId).getPermissionFolderId(),'UPDATE')")
    @DeleteMapping(value = ApiPath.REQUEST_ID_PATH)
    public ResponseEntity<Void> deleteRequest(@PathVariable(ApiPath.REQUEST_ID) UUID requestId) {
        requestService.deleteRequest(requestId);
        return ResponseEntity.ok().build();
    }

    @AuditAction(auditAction = "Delete requests with ids [{{#request.requestIds}}] "
            + "in the project '{{#request.projectId}}'")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#request.getProjectId(), 'DELETE') and "
            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),"
            + "@requestService.getPermissionFolderIdsByRequestIds(#request.getRequestIds()),'UPDATE')")
    @DeleteMapping
    public ResponseEntity<Void> deleteRequests(
            @RequestBody @Valid RequestEntitiesBulkDelete request) {
        requestService.bulkDeleteRequests(request);
        return ResponseEntity.ok().build();
    }

    @AuditAction(auditAction = "Export request with id '{{#requestId}}'")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "@requestService.getRequest(#requestId).getProjectId(),'READ')")
    @GetMapping(value = ApiPath.REQUEST_ID_PATH + ApiPath.EXPORT_PATH)
    @Deprecated
    public ResponseEntity<RequestExportResponse> exportRequest(@PathVariable(ApiPath.REQUEST_ID) UUID requestId,
                                                               @RequestParam(required = false) UUID environmentId,
                                                               @RequestParam(required = false) String context)
            throws URISyntaxException {
        String curlRequest = requestService.exportRequest(requestId, environmentId, context, null);
        return ResponseEntity.ok(new RequestExportResponse(curlRequest));
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "@requestService.getRequest(#requestId).getProjectId(),'READ')")
    @PostMapping(value = ApiPath.REQUEST_ID_PATH + ApiPath.EXPORT_PATH)
    public ResponseEntity<RequestExportResponse> exportRequest(@PathVariable(ApiPath.REQUEST_ID) UUID requestId,
                                                               @RequestParam(required = false) UUID environmentId,
                                                               @RequestParam(required = false) String context,
                                                               @RequestBody(required = false)
                                                               List<ContextVariable> contextVariables)
            throws URISyntaxException {
        String curlRequest = requestService.exportRequest(requestId, environmentId, context, contextVariables);
        return ResponseEntity.ok(new RequestExportResponse(curlRequest));
    }

    /**
     * Imports request from curl string.
     * @param importRequest request id and curl request string
     * @return imported request
     */
    @AuditAction(auditAction = "Import request with id '{{#importRequest.requestId}}'")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "@requestService.getRequest(#importRequest.getRequestId()).getProjectId(), 'UPDATE') and "
            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "@requestService.getRequest(#importRequest.getRequestId()).getProjectId(),"
            + "@requestService.getRequest(#importRequest.getRequestId()).getPermissionFolderId(),'UPDATE')")
    @PostMapping(value = ApiPath.IMPORT_PATH)
    public ResponseEntity<Request> importRequest(@RequestBody @Valid CurlStringImportRequest importRequest) {
        MdcUtils.put(ItfLiteMdcField.REQUEST_ID.toString(), importRequest.getRequestId());
        Request importedRequest = requestService.importRequest(importRequest);
        return ResponseEntity.ok(importedRequest);
    }

    @AuditAction(auditAction = "Get context by url '{{#context}}' in the '{{#request.projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#projectId,'READ')")
    @GetMapping(value = ApiPath.CONTEXT_PATH)
    public ResponseEntity<ContextResponse> getContext(@RequestParam UUID projectId, @RequestParam String context)
            throws URISyntaxException {
        String response = requestService.getContext(projectId, context);
        return ResponseEntity.ok(new ContextResponse(response));
    }

    @AuditAction(auditAction = "Change order for the request '{{#requestId}}' in the '{{#request.projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#request.getProjectId(), 'UPDATE') and "
            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "#request.getProjectId(),@requestService.getRequest(#requestId).getPermissionFolderId(),'UPDATE')")
    @PostMapping(value = ApiPath.REQUEST_ID_PATH + ApiPath.ORDER_PATH)
    public ResponseEntity<Void> order(@PathVariable(ApiPath.REQUEST_ID) UUID requestId,
                                      @RequestBody RequestOrderChangeRequest request) {
        requestService.order(requestId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint to execute single request from collection.
     */
    @PreAuthorize("@entityAccess.checkAccess(#requestExecuteRequest.getProjectId(),'EXECUTE')")
    @PostMapping(value = ApiPath.EXECUTE_PATH)
    public ResponseEntity<ExecuteStepResponse> execute(
            @RequestParam(required = false) UUID environmentId,
            @RequestBody ExecutionCollectionRequestExecuteRequest requestExecuteRequest)
            throws JsonProcessingException {
        return ResponseEntity.ok(actionService.executeAction(requestExecuteRequest, environmentId));
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "@requestService.getRequest(#requestId).getProjectId(), 'UPDATE') and "
            + "@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).FOLDER.getName(),"
            + "@requestService.getRequest(#requestId).getProjectId(),"
            + "@requestService.getRequest(#requestId).getPermissionFolderId(),'UPDATE')")
    @PutMapping(value = ApiPath.REQUEST_ID_PATH + ApiPath.FILE_UPLOAD_PATH)
    public ResponseEntity<Void> uploadFormDataFile(@PathVariable(ApiPath.REQUEST_ID) UUID requestId,
                                                   @PathVariable UUID formDataPartId,
                                                   @RequestPart(name = "file") MultipartFile file) {
        formDataPartService.uploadFile(requestId, formDataPartId, file);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "@requestService.getRequest(#requestId).getProjectId(),'READ')")
    @GetMapping(value = ApiPath.REQUEST_ID_PATH + ApiPath.FILE_DOWNLOAD_PATH)
    public void downloadFile(@PathVariable(ApiPath.REQUEST_ID) UUID requestId,
                             @PathVariable(ApiPath.FILE_ID) UUID fileId,
                             HttpServletResponse response) throws IOException {
        requestService.getFile(requestId, fileId, response);
    }

    @PreAuthorize("@entityAccess.checkAccess(#importContextRequest.getProjectId(),'READ')")
    @PostMapping(value = ApiPath.IMPORT_PATH + ApiPath.CONTEXT_VARIABLES_PATH)
    public ResponseEntity<ImportContextResponse> importContextFromRam(
            @RequestBody ImportContextRequest importContextRequest) {
        return ResponseEntity.ok(ramService.importContextVariables(importContextRequest));
    }

    /**
     * Sends response as file.
     * @param requestId request id
     * @param executionId  execution id
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "@requestService.getRequest(#requestId).getProjectId(),'READ')")
    @GetMapping(value = ApiPath.REQUEST_ID_PATH + ApiPath.DOWNLOAD_RESPONSE_PATH)
    public void downloadResponseAsFile(@PathVariable(ApiPath.REQUEST_ID) UUID requestId,
                                       @RequestParam UUID executionId,
                                       HttpServletResponse response) {
        try {
            requestService.writeResponseAsFile(requestId, executionId, response);
        } catch (Exception e) {
            log.error("Can't download response as file.", e);
            throw new ItfLiteSaveResponseAsFileException();
        }
    }
}
