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
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.model.api.ApiPath;
import org.qubership.atp.itf.lite.backend.model.entities.history.HistoryRequestDetailsResponse;
import org.qubership.atp.itf.lite.backend.model.entities.history.HistorySearchRequest;
import org.qubership.atp.itf.lite.backend.model.entities.history.PaginatedResponse;
import org.qubership.atp.itf.lite.backend.model.entities.history.RequestExecution;
import org.qubership.atp.itf.lite.backend.service.RequestExecutionHistoryService;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(ApiPath.SERVICE_API_V1_PATH + ApiPath.HISTORY_PATH)
@AllArgsConstructor
@Slf4j
public class HistoryController {

    private final RequestExecutionHistoryService requestExecutionHistoryService;

    @AuditAction(auditAction = "Get execution history in the '{{#request.projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess(#request.getProjectId(),'READ')")
    @PostMapping
    public ResponseEntity<PaginatedResponse<RequestExecution>> getExecutionsHistory(@RequestBody
                                                                                    HistorySearchRequest request) {
        return ResponseEntity.ok(requestExecutionHistoryService.getExecutionHistory(request));
    }

    @AuditAction(auditAction = "Get execution history details for the item '{{#historyItemId}}' "
            + "in the '{{#projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess(#projectId,'READ')")
    @GetMapping(value = ApiPath.ID_PATH)
    public ResponseEntity<HistoryRequestDetailsResponse> getExecutionHistoryDetails(
            @PathVariable(ApiPath.ID) UUID historyItemId,
            @RequestParam TransportType type,
            @RequestParam UUID projectId) {
        return ResponseEntity.ok(requestExecutionHistoryService
                .getExecutionHistoryDetailsByHistoryItemId(historyItemId));
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectId,'READ')")
    @GetMapping(value = ApiPath.FILE_PATH + ApiPath.ID_PATH)
    public void getBinaryFileHistory(@PathVariable(ApiPath.ID) UUID fileId,
                                     @RequestHeader(Constants.PROJECT_ID_HEADER_NAME) UUID projectId,
                                     HttpServletResponse response)
            throws IOException {
        requestExecutionHistoryService.getBinaryFileHistory(fileId, response);
    }

    @AuditAction(auditAction = "Get all executors in the execution history for the '{{#projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess(#projectId,'READ')")
    @GetMapping(value = ApiPath.EXECUTORS_PATH)
    public ResponseEntity<List<String>> getExecutorsInRequestExecutionHistory(@RequestParam UUID projectId) {
        return ResponseEntity.ok(requestExecutionHistoryService.getExecutorsInRequestExecutionHistory(projectId));
    }
}
