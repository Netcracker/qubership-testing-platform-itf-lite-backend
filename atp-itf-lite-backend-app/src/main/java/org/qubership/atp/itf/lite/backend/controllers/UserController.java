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

import java.util.List;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.itf.lite.backend.model.api.ApiPath;
import org.qubership.atp.itf.lite.backend.model.entities.user.UserSettings;
import org.qubership.atp.itf.lite.backend.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(ApiPath.SERVICE_API_V1_PATH + ApiPath.USER_SETTINGS_PATH)
@AllArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @AuditAction(auditAction = "Save settings for the user with id '{{#setting.userId}}'")
    @PreAuthorize("@entityAccess.checkAccess(#projectId, 'CREATE') || @entityAccess.checkAccess(#projectId, 'UPDATE')")
    @PostMapping
    public ResponseEntity<UserSettings> saveUserSettings(
            @RequestParam UUID projectId,
            @RequestBody UserSettings setting,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String token) {
        return new ResponseEntity<>(userService.saveUserSettings(setting, token), HttpStatus.OK);
    }

    @AuditAction(auditAction = "Get user settings")
    @PreAuthorize("@entityAccess.checkAccess(#projectId,'READ')")
    @GetMapping
    public ResponseEntity<List<UserSettings>> getSettingsByUser(
            @RequestParam UUID projectId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String token) {
        List<UserSettings> setting = userService.getSettingsByUser(token);
        return nonNull(setting) ? ResponseEntity.ok(setting) : ResponseEntity.notFound().build();
    }
}
