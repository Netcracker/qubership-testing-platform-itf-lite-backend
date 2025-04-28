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

package org.qubership.atp.itf.lite.backend.service.kafka;

import java.io.IOException;
import java.util.UUID;

import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.itf.lite.backend.configuration.KafkaConfiguration;
import org.qubership.atp.itf.lite.backend.enums.ImportToolType;
import org.qubership.atp.itf.lite.backend.enums.RequestExportStatus;
import org.qubership.atp.itf.lite.backend.mdc.ItfLiteMdcField;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ExportResponseEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfExportResponseEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.MiaExportResponseEvent;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExportResultResponse;
import org.qubership.atp.itf.lite.backend.model.entities.RequestExportEntity;
import org.qubership.atp.itf.lite.backend.service.RequestExportService;
import org.qubership.atp.itf.lite.backend.service.SseEmitterService;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KafkaExportEventResponseService {

    private static final String KAFKA_MIA_EXPORT_EVENT_RESPONSE_LISTENER_ID = "kafkaMiaExportEventResponseListenerId";
    private static final String KAFKA_ITF_EXPORT_EVENT_RESPONSE_LISTENER_ID = "kafkaItfExportEventResponseListenerId";

    private final SseEmitterService sseEmitterService;
    private final RequestExportService requestExportService;

    /**
     * Creates KafkaExportEventResponseService.
     * @param sseEmitterService sse emitter service
     * @param requestExportService request export service
     */
    public KafkaExportEventResponseService(SseEmitterService sseEmitterService,
                                               RequestExportService requestExportService) {
        this.sseEmitterService = sseEmitterService;
        this.requestExportService = requestExportService;
    }

    /**
     * Listens kafka event for mia export response.
     * @param miaExportResponse kafka event with mia export response
     * @throws IOException during sse emitter send response
     */
    @KafkaListener(groupId = KAFKA_MIA_EXPORT_EVENT_RESPONSE_LISTENER_ID
                + "_#{T(org.qubership.atp.itf.lite.backend.utils.PodNameUtils).getServicePodName()}",
            topics = {"${kafka.itflite.export.mia.finish.topic}"},
            containerFactory = KafkaConfiguration.MIA_EXPORT_KAFKA_CONTAINER_FACTORY_BEAN_NAME)
    @Transactional
    public void listenMiaExportResponse(@Payload MiaExportResponseEvent miaExportResponse) throws IOException {
        MDC.clear();
        MdcUtils.put(ItfLiteMdcField.REQUEST_ID.toString(), miaExportResponse.getRequestId());
        processExportResponse(miaExportResponse, ImportToolType.MIA);
    }

    /**
     * Listens kafka event for itf export response.
     * @param itfExportResponse kafka event with itf export response
     * @throws IOException during sse emitter send response
     */
    @KafkaListener(groupId = KAFKA_ITF_EXPORT_EVENT_RESPONSE_LISTENER_ID
                + "_#{T(org.qubership.atp.itf.lite.backend.utils.PodNameUtils).getServicePodName()}",
            topics = {"${kafka.itflite.export.itf.finish.topic}"},
            containerFactory = KafkaConfiguration.ITF_EXPORT_KAFKA_CONTAINER_FACTORY_BEAN_NAME)
    @Transactional
    public void listenItfExportResponse(@Payload ItfExportResponseEvent itfExportResponse) throws IOException {
        MDC.clear();
        MdcUtils.put(ItfLiteMdcField.REQUEST_ID.toString(), itfExportResponse.getRequestId());
        processExportResponse(itfExportResponse, ImportToolType.ITF);
    }

    /**
     * Prepares and sends sse events about export results.
     * @param exportResponse export response
     * @param importToolType import tool type
     * @throws IOException io exception in sse emitter send
     */
    @Transactional
    public void processExportResponse(ExportResponseEvent exportResponse, ImportToolType importToolType)
            throws IOException {
        log.debug("Start itf-lite export response from {} processing by event from kafka [{}]",
                importToolType, exportResponse);
        log.info("Read {} export response from kafka.", importToolType);
        UUID requestExportId = exportResponse.getId();
        log.debug("Search sseId by requestExportId = {}", requestExportId);
        RequestExportEntity requestExportEntity = requestExportService.findByRequestExportId(requestExportId);
        UUID sseId = requestExportEntity.getSseId();
        log.debug("Search for sseEmitter with sseId = {}", sseId);
        // check if current itf-lite sseEmitters map has sseEmitter with key = sseId
        SseEmitter sseEmitter = sseEmitterService.getEmitter(sseId);
        if (sseEmitter == null) {
            log.debug(Constants.SSE_EMITTER_WITH_SSE_ID_NOT_FOUND, sseId);
            return;
        }

        RequestExportStatus status = RequestExportStatus.DONE;
        if (!RequestExportStatus.DONE.name().equalsIgnoreCase(exportResponse.getStatus())) {
            status = RequestExportStatus.ERROR;
        }
        RequestExportResultResponse exportResult = buildExportResult(exportResponse, status);
        requestExportService.processExportResult(requestExportEntity, exportResult, sseEmitter, importToolType);
    }

    private RequestExportResultResponse buildExportResult(ExportResponseEvent exportResponse,
                                                          RequestExportStatus requestExportStatus) {
        return RequestExportResultResponse.builder()
                .requestId(exportResponse.getRequestId())
                .requestUrl(exportResponse.getRequestUrl())
                .errorDescription(exportResponse.getErrorMessage())
                .status(requestExportStatus)
                .build();
    }
}
