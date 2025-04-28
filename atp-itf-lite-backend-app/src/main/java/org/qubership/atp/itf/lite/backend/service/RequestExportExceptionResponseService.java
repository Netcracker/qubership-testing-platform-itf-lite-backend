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

package org.qubership.atp.itf.lite.backend.service;

import java.util.UUID;

import org.qubership.atp.itf.lite.backend.enums.ImportToolType;
import org.qubership.atp.itf.lite.backend.enums.RequestExportStatus;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfExportResponseEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.MiaExportResponseEvent;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaExportEventExceptionResponseSendingService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestExportExceptionResponseService {

    private final KafkaExportEventExceptionResponseSendingService kafkaExportEventExceptionResponseSendingService;

    /**
     * Sends exception response event to kafka export finish topics.
     *
     * @param exportRequestId export request id
     * @param requestId       request id
     * @param errorMessage    error message
     */
    public void sendExceptionResponseEvent(ImportToolType importToolType, UUID exportRequestId, UUID requestId,
                                           String errorMessage) {
        if (ImportToolType.MIA.equals(importToolType)) {
            sendMiaExportExceptionResponseEvent(exportRequestId, requestId, errorMessage);
        } else {
            sendItfExportExceptionResponseEvent(exportRequestId, requestId, errorMessage);
        }
    }

    /**
     * Sends exception response event to kafka mia export finish topics.
     *
     * @param exportRequestId export request id
     * @param requestId       request id
     * @param errorMessage    error message
     */
    private void sendMiaExportExceptionResponseEvent(UUID exportRequestId, UUID requestId, String errorMessage) {
        log.debug("Send export finish response for exportRequestId = {}, requestId = {}", exportRequestId, requestId);
        MiaExportResponseEvent miaExportResponseEvent = new MiaExportResponseEvent();
        miaExportResponseEvent.setId(exportRequestId);
        miaExportResponseEvent.setRequestId(requestId);
        miaExportResponseEvent.setErrorMessage(errorMessage);
        miaExportResponseEvent.setStatus(RequestExportStatus.ERROR.name());
        kafkaExportEventExceptionResponseSendingService.miaFinishExportResponseEventSend(miaExportResponseEvent);
    }

    /**
     * Sends exception response event to kafka itf export finish topics.
     *
     * @param exportRequestId export request id
     * @param requestId       request id
     * @param errorMessage    error message
     */
    private void sendItfExportExceptionResponseEvent(UUID exportRequestId, UUID requestId, String errorMessage) {
        log.debug("Send export finish response for exportRequestId = {}, requestId = {}", exportRequestId, requestId);
        ItfExportResponseEvent itfExportResponseEvent = new ItfExportResponseEvent();
        itfExportResponseEvent.setId(exportRequestId);
        itfExportResponseEvent.setRequestId(requestId);
        itfExportResponseEvent.setErrorMessage(errorMessage);
        itfExportResponseEvent.setStatus(RequestExportStatus.ERROR.name());
        kafkaExportEventExceptionResponseSendingService.itfFinishExportResponseEventSend(itfExportResponseEvent);
    }
}
