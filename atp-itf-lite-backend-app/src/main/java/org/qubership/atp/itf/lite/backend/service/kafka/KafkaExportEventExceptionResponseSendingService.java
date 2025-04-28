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

import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfExportResponseEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.MiaExportResponseEvent;
import org.springframework.kafka.core.KafkaTemplate;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Slf4j
public class KafkaExportEventExceptionResponseSendingService {

    private String miaFinishTopicName;
    private KafkaTemplate<UUID, MiaExportResponseEvent> miaFinishExportKafkaTemplate;
    private String itfFinishTopicName;
    private KafkaTemplate<UUID, ItfExportResponseEvent> itfFinishExportKafkaTemplate;

    /**
     * Creates KafkaExportEventSendingService.
     *
     * @param miaFinishTopicName           mia finish topic name
     * @param miaFinishExportKafkaTemplate mia finish kafka template
     * @param itfFinishTopicName           itf finish topic name
     * @param itfFinishExportKafkaTemplate itf finish kafka template
     */
    public KafkaExportEventExceptionResponseSendingService(
            String miaFinishTopicName,
            KafkaTemplate<UUID, MiaExportResponseEvent> miaFinishExportKafkaTemplate,
            String itfFinishTopicName,
            KafkaTemplate<UUID, ItfExportResponseEvent> itfFinishExportKafkaTemplate) {
        this.miaFinishTopicName = miaFinishTopicName;
        this.miaFinishExportKafkaTemplate = miaFinishExportKafkaTemplate;
        this.itfFinishTopicName = itfFinishTopicName;
        this.itfFinishExportKafkaTemplate = itfFinishExportKafkaTemplate;
    }

    /**
     * Sends mia export exception response event to kafka.
     *
     * @param miaExportResponseEvent mia export response event
     */
    public void miaFinishExportResponseEventSend(MiaExportResponseEvent miaExportResponseEvent) {
        log.debug("Send mia export finish response to kafka for requestExportId = {}, requestId = {}",
                miaExportResponseEvent.getId(), miaExportResponseEvent.getRequestId());
        miaFinishExportKafkaTemplate.send(miaFinishTopicName, miaExportResponseEvent.getId(), miaExportResponseEvent);
    }

    /**
     * Sends itf export exception response event to kafka.
     *
     * @param itfExportResponseEvent itf export request event
     */
    public void itfFinishExportResponseEventSend(ItfExportResponseEvent itfExportResponseEvent) {
        log.debug("Send itf export finish response to kafka for requestExportId = {}, requestId = {}",
                itfExportResponseEvent.getId(), itfExportResponseEvent.getRequestId());
        itfFinishExportKafkaTemplate.send(itfFinishTopicName, itfExportResponseEvent.getId(), itfExportResponseEvent);
    }
}
