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

import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.ACTIONS_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SERVICE_API_V1_PATH;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.itf.lite.backend.model.entities.Action;
import org.qubership.atp.itf.lite.backend.service.ActionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping(SERVICE_API_V1_PATH + ACTIONS_PATH)
@AllArgsConstructor
public class ActionController {

    private final ActionService actionService;

    @PreAuthorize("@entityAccess.checkAccess(#projectId,'READ')")
    @GetMapping
    public ResponseEntity<List<Action>> getActions(@RequestParam UUID projectId) {
        return ResponseEntity.ok(actionService.getActions(projectId));
    }
}
