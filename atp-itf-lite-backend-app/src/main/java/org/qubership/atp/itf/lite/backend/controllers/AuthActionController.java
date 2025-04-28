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

import org.qubership.atp.itf.lite.backend.feign.dto.GetAccessCodeParametersDto;
import org.qubership.atp.itf.lite.backend.service.GetAccessTokenByCodeService;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class AuthActionController {

    private final GetAccessTokenByCodeService getAccessTokenByCodeService;

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/atp-itf-lite/api/v1/authAction/saveCode/{sseId}"
    )
    @PreAuthorize("@entityAccess.checkAccess(#projectId,'READ')")
    public ResponseEntity<Void> saveCode(@PathVariable("sseId") UUID sseId,
                                         @RequestParam(value = "code") String code,
                                         @RequestParam(value = "state", required = false) String state,
                                         @RequestHeader(Constants.PROJECT_ID_HEADER_NAME) UUID projectId) {
        getAccessTokenByCodeService.saveCode(sseId, code, state);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/atp-itf-lite/api/v1/authAction/saveParamsForGetAccessCode",
            consumes = { "application/json" }
    )
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#getAccessCodeParametersDto.getProjectId(),'EXECUTE')")
    public ResponseEntity<Void> saveParamsForGetAccessCode(GetAccessCodeParametersDto getAccessCodeParametersDto) {
        getAccessTokenByCodeService.saveParamsForGetAccessCode(getAccessCodeParametersDto);
        return ResponseEntity.ok().build();
    }
}
