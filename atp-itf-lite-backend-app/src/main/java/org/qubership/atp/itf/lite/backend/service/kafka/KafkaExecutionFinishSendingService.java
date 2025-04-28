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

import java.util.UUID;

import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfLiteExecutionFinishEvent;
import org.springframework.kafka.core.KafkaTemplate;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Slf4j
public class KafkaExecutionFinishSendingService {

    private String itfLiteExecutionFinishTopic;
    private KafkaTemplate<UUID, ItfLiteExecutionFinishEvent> itfLiteExecutionFinishKafkaTemplate;

    /**
     * Creates KafkaExecutionFinishSendingService.
     *
     * @param itfLiteExecutionFinishTopic         itf lite execution finish topic
     * @param itfLiteExecutionFinishKafkaTemplate itf lit execution finish kafka template
     */
    public KafkaExecutionFinishSendingService(
            String itfLiteExecutionFinishTopic,
            KafkaTemplate<UUID, ItfLiteExecutionFinishEvent> itfLiteExecutionFinishKafkaTemplate) {
        this.itfLiteExecutionFinishTopic = itfLiteExecutionFinishTopic;
        this.itfLiteExecutionFinishKafkaTemplate = itfLiteExecutionFinishKafkaTemplate;
    }

    /**
     * Sends execution finish event to kafka.
     *
     * @param executionFinishEvent execution finish event
     */
    public void executionFinishEventSend(ItfLiteExecutionFinishEvent executionFinishEvent) {
        log.debug("Send execution finish event for sseId = {}", executionFinishEvent.getSseId());
        itfLiteExecutionFinishKafkaTemplate.send(itfLiteExecutionFinishTopic, executionFinishEvent.getSseId(),
                executionFinishEvent);
    }
}
