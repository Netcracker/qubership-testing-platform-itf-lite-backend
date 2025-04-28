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

package org.qubership.atp.itf.lite.backend.service.history.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestRepository;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.feign.dto.history.HistoryItemTypeDto;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.service.history.iface.RestoreHistoryService;
import org.qubership.atp.itf.lite.backend.service.history.iface.RetrieveHistoryService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HistoryServiceFactory {
    private final List<RestoreHistoryService> restoreHistoryServices;
    private final List<RetrieveHistoryService> retrieveHistoryServices;
    private final RequestRepository requestRepository;

    /**
     * Returns the concrete implementation of RestoreHistoryService depending of entity type.
     *
     * @param itemType type of domain entity with supported history
     * @return RestoreHistoryService implementation
     */
    public Optional<RestoreHistoryService> getRestoreHistoryService(String itemType, UUID itemId) {
        if (HistoryItemTypeDto.REQUEST.toString().equalsIgnoreCase(itemType)) {
            List<RestoreHistoryService> restoreRequestHistoryServices =
                    restoreHistoryServices.stream()
                            .filter(service -> service.getItemType().toString().equalsIgnoreCase(itemType))
                            .collect(Collectors.toList());
            TransportType type = requestRepository.findTransportType(itemId);

            return restoreRequestHistoryServices.stream()
                    .filter(service -> HttpRequest.class.equals(service.getEntityClass()))
                    .findFirst();
        }

        return restoreHistoryServices.stream()
                .filter(service -> service.getItemType().toString().equalsIgnoreCase(itemType))
                .findFirst();
    }

    /**
     * Returns the concrete implementation of RetrieveHistoryService depending on entity type.
     *
     * @param itemType type of domain entity with supported history
     * @return RetrieveHistoryService implementation
     */
    public Optional<RetrieveHistoryService> getRetrieveHistoryService(String itemType, UUID itemId) {
        if (HistoryItemTypeDto.REQUEST.toString().equalsIgnoreCase(itemType)) {
            List<RetrieveHistoryService> retrieveRequestHistoryServiced =
                    retrieveHistoryServices.stream()
                            .filter(service -> service.getItemType().toString().equalsIgnoreCase(itemType))
                            .collect(Collectors.toList());
            TransportType type = requestRepository.findTransportType(itemId);
            return retrieveRequestHistoryServiced.stream()
                    .filter(service -> HttpRequest.class.equals(service.getEntityClass()))
                    .findFirst();
        }

        return retrieveHistoryServices.stream()
                .filter(service -> service.getItemType().toString().equalsIgnoreCase(itemType))
                .findFirst();
    }
}