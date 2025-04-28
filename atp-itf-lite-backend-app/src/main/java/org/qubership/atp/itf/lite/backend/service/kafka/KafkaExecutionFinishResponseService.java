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

import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.itf.lite.backend.configuration.KafkaConfiguration;
import org.qubership.atp.itf.lite.backend.mdc.ItfLiteMdcField;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfLiteExecutionFinishEvent;
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
public class KafkaExecutionFinishResponseService {

    private static final String KAFKA_ITF_LITE_EXECUTION_FINISH_EVENT_RESPONSE_LISTENER_ID =
            "kafkaItfLiteExecutionFinishEventResponseListenerId";

    private final SseEmitterService sseEmitterService;

    /**
     * Creates KafkaExecutionFinishResponseService.
     *
     * @param sseEmitterService sse emitter service
     */
    public KafkaExecutionFinishResponseService(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    /**
     * Listen start execution kafka topic.
     */
    @KafkaListener(groupId = KAFKA_ITF_LITE_EXECUTION_FINISH_EVENT_RESPONSE_LISTENER_ID
            + "_#{T(org.qubership.atp.itf.lite.backend.utils.PodNameUtils).getServicePodName()}",
            topics = "${kafka.itflite.execution.finish.topic}",
            containerFactory = KafkaConfiguration.ITF_LITE_EXECUTION_FINISH_CONTAINER_FACTORY_BEAN_NAME
    )
    @Transactional
    public void listenItfLiteExecutionFinishEvent(@Payload ItfLiteExecutionFinishEvent executionFinishEvent) {
        MDC.clear();
        MdcUtils.put(ItfLiteMdcField.REQUEST_ID.toString(), executionFinishEvent.getRequestId());
        log.debug("Start itf-lite execution processing by event from kafka [{}]", executionFinishEvent);
        // check if current itf-lite sseEmitters map has sseEmitter with key = sseId
        SseEmitter sseEmitter = sseEmitterService.getEmitter(executionFinishEvent.getSseId());
            if (sseEmitter == null) {
                log.debug(Constants.SSE_EMITTER_WITH_SSE_ID_NOT_FOUND, executionFinishEvent.getSseId());
                return;
            }
            sseEmitterService.generateResponseAndSendToEmitter(sseEmitter, executionFinishEvent);
    }
}
