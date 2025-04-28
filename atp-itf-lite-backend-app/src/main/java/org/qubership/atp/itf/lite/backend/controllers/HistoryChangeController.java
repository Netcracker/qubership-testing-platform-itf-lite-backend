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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;

import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.exceptions.history.ItfLiteRevisionHistoryIncorrectTypeException;
import org.qubership.atp.itf.lite.backend.feign.dto.history.CompareEntityResponseDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.HistoryItemResponseDto;
import org.qubership.atp.itf.lite.backend.service.history.iface.RestoreHistoryService;
import org.qubership.atp.itf.lite.backend.service.history.iface.RetrieveHistoryService;
import org.qubership.atp.itf.lite.backend.service.history.impl.HistoryServiceFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class HistoryChangeController {

    private final HistoryServiceFactory historyServiceFactory;
    private final ModelMapper modelMapper;

    /**
     * Get all history.
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/atp-itf-lite/api/v1/history/{projectId}/{itemType}/{id}",
            produces = { "application/json" }
    )
    @PreAuthorize("@entityAccess.checkAccess(#itemType, #projectId, 'READ')")
    public ResponseEntity<HistoryItemResponseDto> getAllHistory(@PathVariable UUID projectId,
                                                                @PathVariable String itemType,
                                                                @PathVariable UUID id,
                                                                @RequestParam(value = "offset", required = false,
                                                                        defaultValue = "0") Integer offset,
                                                                @RequestParam(value = "limit", required = false,
                                                                        defaultValue = "10") Integer limit) {
        Optional<RetrieveHistoryService> historyServiceOptional =
                historyServiceFactory.getRetrieveHistoryService(itemType, id);

        if (historyServiceOptional.isPresent()) {
            RetrieveHistoryService retrieveHistoryService = historyServiceOptional.get();
            HistoryItemResponseDto response = retrieveHistoryService.getAllHistory(id, offset, limit);
            return ResponseEntity.ok(response);
        } else {
            throw new ItfLiteRevisionHistoryIncorrectTypeException(itemType);
        }
    }

    /**
     * Get entities by version.
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/atp-itf-lite/api/v1/entityversioning/{projectId}/{itemType}/{id}",
            produces = { "application/json" }
    )
    @PreAuthorize("@entityAccess.checkAccess(#itemType, #projectId, 'READ')")
    public ResponseEntity<List<CompareEntityResponseDto>> getEntitiesByVersion(@PathVariable UUID projectId,
                                                                               @PathVariable String itemType,
                                                                               @PathVariable UUID id,
                                                                               @RequestParam List<String> versions) {
        Optional<RetrieveHistoryService> historyServiceOptional =
                historyServiceFactory.getRetrieveHistoryService(itemType, id);

        if (historyServiceOptional.isPresent()) {
            return ResponseEntity.ok(historyServiceOptional.get().getEntitiesByVersions(id, versions));
        } else {
            throw new ItfLiteRevisionHistoryIncorrectTypeException(itemType);
        }
    }

    /**
     * Restore to revision.
     */
    @RequestMapping(
            method = RequestMethod.POST,
            value = "/atp-itf-lite/api/v1/history/restore/{projectId}/{itemType}/{id}/revision/{revisionId}"
    )
    @PreAuthorize("@entityAccess.checkAccess(#itemType,#projectId,'UPDATE')")
    public ResponseEntity<Void> restoreToRevision(@PathVariable UUID projectId,
                                                  @PathVariable String itemType,
                                                  @PathVariable UUID id,
                                                  @PathVariable Integer revisionId) {
        Optional<RestoreHistoryService> historyServiceOptional =
                historyServiceFactory.getRestoreHistoryService(itemType, id);

        if (historyServiceOptional.isPresent()) {
            historyServiceOptional.get().restoreToRevision(id, revisionId);
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            throw new ItfLiteRevisionHistoryIncorrectTypeException(itemType);
        }
    }

}
