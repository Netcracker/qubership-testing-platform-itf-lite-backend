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

import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.enums.ImportToolType;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.model.api.kafka.entities.HttpRequestExportEntity;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestExportRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestItfExportRequest;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.service.RequestService;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaExportEventSendingService;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ItfRestRequestExportStrategy extends ItfAbstractRequestExportStrategy {

    private final RequestService requestService;

    public ItfRestRequestExportStrategy(KafkaExportEventSendingService kafkaExportEventSendingService,
                                        RequestService requestService) {
        super(kafkaExportEventSendingService);
        this.requestService = requestService;
    }

    @Override
    public void export(UUID exportRequestId, RequestExportRequest exportRequest, Request request, String context,
                       UUID environmentId)
            throws URISyntaxException, AtpDecryptException {
        HttpRequest httpRequestWithResolvedVariables =
                requestService.resolveAllVariables((HttpRequest) request, context, false, environmentId);
        requestService.encodeRequestParametersExceptEnv(httpRequestWithResolvedVariables.getRequestParams());
        HttpRequestExportEntity httpRequestExportEntity =
                new HttpRequestExportEntity(httpRequestWithResolvedVariables, true);
        sendExportRequestEvent(exportRequestId, (RequestItfExportRequest) exportRequest, httpRequestExportEntity);
    }

    @Override
    public TransportType getRequestTransportType() {
        return TransportType.REST;
    }

    @Override
    public ImportToolType getImportToolType() {
        return ImportToolType.ITF;
    }
}
