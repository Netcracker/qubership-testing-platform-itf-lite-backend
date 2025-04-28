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

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.itf.lite.backend.mdc.ItfLiteMdcField;
import org.qubership.atp.itf.lite.backend.model.api.ApiPath;
import org.qubership.atp.itf.lite.backend.service.RequestParamService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(ApiPath.SERVICE_API_V1_PATH + ApiPath.REQUEST_PARAMS_PATH)
@AllArgsConstructor
@Slf4j
public class RequestParamsController {

    private final RequestParamService service;

    /**
     * Disables parameter with paramId.
     * @param projectId project id
     * @param requestId request id
     * @param paramId param id
     */
    @AuditAction(auditAction = "Disable request '{{#requestId}}' param '{{#paramId}}' in the '{{#projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#projectId,@requestService.getRequest(#requestId).getPermissionFolderId(),'UPDATE')")
    @PutMapping(value = ApiPath.ID_PATH + ApiPath.DISABLE_PATH)
    public void disableRequestParam(@RequestParam UUID projectId,
                                    @RequestParam UUID requestId,
                                    @PathVariable(ApiPath.ID) UUID paramId) {
        MdcUtils.put(ItfLiteMdcField.REQUEST_ID.toString(), requestId);
        log.info("Request to disable param with id '{}' for request with id '{}'", paramId, requestId);
        service.disableRequestParam(paramId);
    }

    /**
     * Enables parameter with paramId.
     * @param projectId project id
     * @param requestId request id
     * @param paramId param id
     */
    @AuditAction(auditAction = "Enable request '{{#requestId}}' param '{{#paramId}}' in the '{{#projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#projectId,@requestService.getRequest(#requestId).getPermissionFolderId(),'UPDATE')")
    @PutMapping(value = ApiPath.ID_PATH + ApiPath.ENABLE_PATH)
    public void enableRequestParam(@RequestParam UUID projectId,
                                   @RequestParam UUID requestId,
                                   @PathVariable(ApiPath.ID) UUID paramId) {
        MdcUtils.put(ItfLiteMdcField.REQUEST_ID.toString(), requestId);
        log.info("Request to enable param with id '{}' for request with id '{}'", paramId, requestId);
        service.enableRequestParam(paramId);
    }
}
