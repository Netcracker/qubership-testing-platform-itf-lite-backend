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

import java.net.URISyntaxException;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.model.api.kafka.MiaExportRequestEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.entities.HttpRequestExportEntity;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestMiaExportRequest;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.service.RequestService;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaExportEventSendingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public abstract class MiaAbstractRequestExportStrategy implements RequestExportStrategy {

    private final KafkaExportEventSendingService kafkaExportEventSendingService;
    private final RequestService requestService;
    private final ModelMapper modelMapper;

    /**
     * Sends request to kafka for export request into MIA.
     *
     * @param request request
     */
    public void sendExportRequestEvent(UUID exportRequestId, RequestMiaExportRequest requestMiaExportRequest,
                                  Request request, String context) throws URISyntaxException {
        log.debug("Send export request for exportRequestId = {}, requestId = {}", exportRequestId, request.getId());
        HttpRequest httpRequest = (HttpRequest) request;
        HttpRequestEntitySaveRequest saveRequest = modelMapper.map(httpRequest, HttpRequestEntitySaveRequest.class);
        requestService.resolveVelocityVariables(httpRequest, context, true, saveRequest);
        requestService.encodeRequestParametersExceptEnv(httpRequest.getRequestParams());
        MiaExportRequestEvent miaExportRequestEvent = new MiaExportRequestEvent();
        miaExportRequestEvent.setId(exportRequestId);
        miaExportRequestEvent.setProjectId(requestMiaExportRequest.getProjectId());
        miaExportRequestEvent.setMiaPath(requestMiaExportRequest.getMiaPath());
        // agreed to use request name as process name
        miaExportRequestEvent.setMiaProcessName(request.getName());
        HttpRequestExportEntity miaRequestEntity = new HttpRequestExportEntity(httpRequest, true);
        miaExportRequestEvent.setRequest(miaRequestEntity);
        kafkaExportEventSendingService.miaExportRequestEventSend(exportRequestId, miaExportRequestEvent);
    }
}
