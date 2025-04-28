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

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.itf.lite.backend.enums.ImportToolType;
import org.qubership.atp.itf.lite.backend.model.RequestRuntimeOptions;
import org.qubership.atp.itf.lite.backend.model.api.ApiPath;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestItfExportRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestMiaExportRequest;
import org.qubership.atp.itf.lite.backend.service.RequestExportService;
import org.qubership.atp.itf.lite.backend.service.RequestService;
import org.qubership.atp.itf.lite.backend.service.SseEmitterService;
import org.qubership.atp.itf.lite.backend.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(ApiPath.SERVICE_API_V1_PATH + ApiPath.SSE_PATH)
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final RequestService requestService;
    private final RequestExportService requestExportService;
    private final SseEmitterService sseEmitterService;
    private final UserService userService;

    /**
     * Endpoint to create SSE-emitter.
     *
     * @return created emitter for particular request identifier
     */
    @AuditAction(auditAction = "Connect to SSE emitter with id '{{#sseId}}' for the '{{#projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess(#projectId,'READ')")
    @GetMapping(value = ApiPath.REQUESTS_PATH + ApiPath.CONNECT_PATH,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam UUID projectId,
                              @RequestParam UUID sseId,
                              @RequestHeader(value = HttpHeaders.AUTHORIZATION) String token) throws IOException {
        SseEmitter emitter = sseEmitterService.getEmitter(sseId);
        if (emitter != null) {
            log.debug("Emitter already exists. Return existing emitter.");
            return emitter;
        }

        UUID userId = userService.getUserIdFromToken(token);
        return sseEmitterService.generateAndConfigureEmitter(sseId, userId);
    }

    /**
     * Endpoint to send caught response via created emitter.
     *
     * @param requestId request identifier
     */
    @AuditAction(auditAction = "Execute request with id '{{#requestId}}'")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "@requestService.getRequest(#requestId).getProjectId(),'EXECUTE')")
    @PostMapping(value = ApiPath.REQUESTS_PATH + ApiPath.REQUEST_ID_PATH + ApiPath.EXECUTE_PATH)
    public void executeRequest(@PathVariable(ApiPath.REQUEST_ID) UUID requestId,
                               @RequestHeader(value = HttpHeaders.AUTHORIZATION) String token,
                               @RequestParam(required = false) UUID environmentId,
                               @RequestParam(required = false) String context,
                               @RequestParam(required = false) UUID sessionId,
                               @RequestParam UUID sseId,
                               @RequestPart(name = "file", required = false) MultipartFile dictionary,
                               @RequestPart(name = "files", required = false) List<MultipartFile> files,
                               @RequestPart(name = "requestEntity") @Valid RequestEntitySaveRequest requestEntity) {
        log.debug("Check if request with requestId {} exists", requestId);
        // EntityNotFoundException will be thrown if not found
        RequestRuntimeOptions runtimeOptions = requestService.retrieveRuntimeOptions(requestId);
        requestEntity.setId(requestId);
        sseEmitterService.processRequestExecution(requestEntity, context, token, sseId,
                Optional.ofNullable(dictionary), files, environmentId, runtimeOptions, sessionId);
    }

    /**
     * Endpoint to send response for notification during export into MIA.
     *
     * @param miaExportRequest mia export requests
     */
    @AuditAction(auditAction = "Export requests [{{#miaExportRequest.requestIds}}] to MIA")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#miaExportRequest.getProjectId(),"
            + "@requestService.getPermissionFolderIdsByRequestIds(#miaExportRequest.getRequestIds()),'READ')")
    @PostMapping(value = ApiPath.REQUESTS_PATH + ApiPath.MIA_PATH + ApiPath.EXPORT_PATH)
    public void exportRequestsToMia(@RequestParam UUID sseId,
                                    @RequestParam(required = false) UUID environmentId,
                                    @RequestParam(required = false) String context,
                                    @RequestBody @Valid RequestMiaExportRequest miaExportRequest,
                                    @RequestHeader(value = HttpHeaders.AUTHORIZATION) String token) {
        UUID userId = userService.getUserIdFromToken(token);
        requestExportService.exportRequests(sseId, userId, miaExportRequest, ImportToolType.MIA, context,
                environmentId);
    }

    /**
     * Endpoint to export requests into ITF.
     *
     * @param itfExportRequest itf export requests
     */
    @AuditAction(auditAction = "Export requests [{{#itfExportRequest.requestIds}}] to ITF")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#itfExportRequest.getProjectId(),"
            + "@requestService.getPermissionFolderIdsByRequestIds(#itfExportRequest.getRequestIds()),'READ')")
    @PostMapping(value = ApiPath.REQUESTS_PATH + ApiPath.ITF_PATH + ApiPath.EXPORT_PATH)
    public void exportRequestsToItf(@RequestParam UUID sseId,
                                    @RequestParam(required = false) UUID environmentId,
                                    @RequestBody @Valid RequestItfExportRequest itfExportRequest,
                                    @RequestHeader(value = HttpHeaders.AUTHORIZATION) String token) {
        UUID userId = userService.getUserIdFromToken(token);
        requestExportService.exportRequests(sseId, userId, itfExportRequest, ImportToolType.ITF, null, environmentId);
    }
}
