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

import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfExportRequestEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.MiaExportRequestEvent;
import org.springframework.kafka.core.KafkaTemplate;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Slf4j
public class KafkaExportEventSendingService {

    private String miaTopicName;
    private KafkaTemplate<UUID, MiaExportRequestEvent> miaExportKafkaTemplate;
    private String itfTopicName;
    private KafkaTemplate<UUID, ItfExportRequestEvent> itfExportKafkaTemplate;

    /**
     * Creates KafkaExportEventSendingService.
     * @param miaTopicName mia topic name
     * @param miaExportKafkaTemplate mia kafka template
     * @param itfTopicName itf topic name
     * @param itfExportKafkaTemplate itf kafka template
     */
    public KafkaExportEventSendingService(String miaTopicName,
                                          KafkaTemplate<UUID, MiaExportRequestEvent> miaExportKafkaTemplate,
                                          String itfTopicName,
                                          KafkaTemplate<UUID, ItfExportRequestEvent> itfExportKafkaTemplate) {
        this.miaTopicName = miaTopicName;
        this.miaExportKafkaTemplate = miaExportKafkaTemplate;
        this.itfTopicName = itfTopicName;
        this.itfExportKafkaTemplate = itfExportKafkaTemplate;
    }

    /**
     * Sends mia export request event to kafka.
     * @param requestExportId request export id
     * @param miaExportRequestEvent mia export request event
     */
    public void miaExportRequestEventSend(UUID requestExportId, MiaExportRequestEvent miaExportRequestEvent) {
        log.debug("Send mia export request to kafka for requestExportId = {}, requestId = {}",
                requestExportId, miaExportRequestEvent.getRequest().getId());
        miaExportKafkaTemplate.send(miaTopicName, requestExportId, miaExportRequestEvent);
    }

    /**
     * Sends itf export request event to kafka.
     * @param requestExportId request export id
     * @param itfExportRequestEvent itf export request event
     */
    public void itfExportRequestEventSend(UUID requestExportId, ItfExportRequestEvent itfExportRequestEvent) {
        log.debug("Send itf export request to kafka for requestExportId = {}, requestId = {}",
                requestExportId, itfExportRequestEvent.getRequest().getId());
        itfExportKafkaTemplate.send(itfTopicName, requestExportId, itfExportRequestEvent);
    }
}
