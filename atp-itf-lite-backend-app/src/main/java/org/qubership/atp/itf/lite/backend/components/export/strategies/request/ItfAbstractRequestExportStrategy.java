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

package org.qubership.atp.itf.lite.backend.components.export.strategies.request;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfExportRequestEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.entities.ExportRequestEntity;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestItfExportRequest;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaExportEventSendingService;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public abstract class ItfAbstractRequestExportStrategy implements RequestExportStrategy {

    private final KafkaExportEventSendingService kafkaExportEventSendingService;

    /**
     * Sends request to kafka for export request into ITF.
     *
     * @param request request
     */
    public void sendExportRequestEvent(UUID exportRequestId, RequestItfExportRequest requestItfExportRequest,
                                       ExportRequestEntity request) {
        log.debug("Send export request for exportRequestId = {}, requestId = {}", exportRequestId, request.getId());
        ItfExportRequestEvent itfExportRequestEvent = new ItfExportRequestEvent();
        itfExportRequestEvent.setId(exportRequestId);
        itfExportRequestEvent.setProjectId(requestItfExportRequest.getProjectId());
        itfExportRequestEvent.setItfUrl(requestItfExportRequest.getItfUrl());
        itfExportRequestEvent.setSystemId(requestItfExportRequest.getSystemId().toString());
        itfExportRequestEvent.setOperationId(requestItfExportRequest.getOperationId().toString());
        if (isReceiversMapContainRequestIdKey(requestItfExportRequest.getRequestIdsReceiversMap(), request.getId())) {
            BigInteger receiverId = requestItfExportRequest.getRequestIdsReceiversMap().get(request.getId());
            if (receiverId != null) {
                itfExportRequestEvent.setReceiver(receiverId.toString());
            }
        }
        itfExportRequestEvent.setRequest(request);
        kafkaExportEventSendingService.itfExportRequestEventSend(exportRequestId, itfExportRequestEvent);
    }

    private boolean isReceiversMapContainRequestIdKey(Map<UUID, BigInteger> receiversMap, UUID requestId) {
        return !CollectionUtils.isEmpty(receiversMap) && receiversMap.containsKey(requestId);
    }
}
