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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.modelmapper.ModelMapper;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.integration.configuration.model.notification.Notification;
import org.qubership.atp.integration.configuration.service.NotificationService;
import org.qubership.atp.itf.lite.backend.configuration.SseProperties;
import org.qubership.atp.itf.lite.backend.enums.ImportToolType;
import org.qubership.atp.itf.lite.backend.enums.SseEventType;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.exceptions.ExceptionConstants;
import org.qubership.atp.itf.lite.backend.exceptions.ItfLiteException;
import org.qubership.atp.itf.lite.backend.model.RequestRuntimeOptions;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfLiteExecutionFinishEvent;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.ErrorResponseSerializable;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionHeaderResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExportResultResponse;
import org.qubership.atp.itf.lite.backend.model.api.sse.GetAccessTokenData;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.qubership.atp.itf.lite.backend.model.entities.history.HttpRequestExecutionDetails;
import org.qubership.atp.itf.lite.backend.model.entities.history.RequestExecutionDetails;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaExecutionFinishSendingService;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseEmitterService {

    private final RequestService requestService;
    private final NotificationService notificationService;
    private final KafkaExecutionFinishSendingService kafkaExecutionFinishSendingService;
    private final RequestExecutionHistoryService requestExecutionHistoryService;
    private final ModelMapper modelMapper;
    private final ExecutorService ssePingsExecutorService = Executors.newCachedThreadPool();
    private final SseProperties sseProperties;
    private final Map<UUID, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    private final GridFsService gridFsService;


    /**
     * Generates and configures emitter for sseId.
     *
     * @param sseId  sse id
     * @param userId user id
     * @return configured sse emitter with connection event
     */
    public SseEmitter generateAndConfigureEmitter(UUID sseId, UUID userId) {
        SseEmitter emitter = new SseEmitter(sseProperties.getSseEmitterTimeout());
        sseEmitters.put(sseId, emitter);
        emitter.onCompletion(() -> sseEmitters.remove(sseId));
        emitter.onTimeout(() -> {
            sseEmitters.remove(sseId);
            prepareAndSendSseEmitterExpiredNotification(userId);
        });
        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        ssePingsExecutorService.execute(() -> {
            try {
                MdcUtils.setContextMap(mdcMap);
                SseEmitter.SseEventBuilder pingEventWithZeroRetryTimeout = SseEmitter.event()
                        .name(SseEventType.PING.name())
                        .reconnectTime(0);
                // ping UI eventSource to prolong connection
                while (true) {
                    TimeUnit.MILLISECONDS.sleep(sseProperties.getSseEmitterPingTimeout());
                    emitter.send(pingEventWithZeroRetryTimeout);
                }
            } catch (Exception e) {
                log.debug("Emitter with sseId = {} was removed from sseEmitters map.", sseId);
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /**
     * Prepares and sends notification message about sse emitter is expired.
     *
     * @param userId user id
     */
    private void prepareAndSendSseEmitterExpiredNotification(UUID userId) {
        Notification notification = new Notification(Constants.SSE_EMITTER_EXPIRED, Notification.Type.INFO, userId);
        notificationService.sendNotification(notification);
    }

    /**
     * Checks that emitter exists and returns emitter.
     *
     * @param sseId sse id
     */
    public SseEmitter getEmitter(UUID sseId) {
        return sseEmitters.getOrDefault(sseId, null);
    }

    /**
     * Completes emitter with error.
     *
     * @param emitter emitter
     */
    public void emitterCompleteWithError(SseEmitter emitter, Exception e) {
        String message = e.getMessage();
        log.error("Exception occurred while sending a response throw emitter: {}", message, e);
        // sse lib on UI has own exception filters, that's why we can't see our custom exception message
        String errorMessage = String.format(ExceptionConstants.EXECUTE_REQUEST_MESSAGE_TEMPLATE, message);
        if (e instanceof ItfLiteException) {
            emitter.completeWithError(e);
            // need to throw exception in /execute
            throw (ItfLiteException) e;
        } else {
            emitter.completeWithError(new ItfLiteException(errorMessage));
            // need to throw exception in /execute
            throw new ItfLiteException(errorMessage);
        }
    }

    /**
     * Sends event about export finish.
     *
     * @param sseId              sse id
     * @param emitter            emitter
     * @param getAccessTokenData getAccessTokenData
     */
    public void sendGetAccessTokenResult(UUID sseId, SseEmitter emitter, GetAccessTokenData getAccessTokenData) {
        try {
            log.debug("Sending sse event of 'GetAccessToken' for sseId = {}", sseId);
            SseEmitter.SseEventBuilder getAccessTokenEvent = SseEmitter.event()
                    .name(SseEventType.GET_ACCESS_TOKEN_FINISHED.name())
                    .data(getAccessTokenData, MediaType.APPLICATION_JSON);
            emitter.send(getAccessTokenEvent);
        } catch (IOException e) {
            log.error("Can't send to emitter result '{}' due to exception", getAccessTokenData, e);
        } finally {
            emitter.complete();
        }
    }

    /**
     * Sends event about export finish.
     *
     * @param sseId          sse id
     * @param emitter        emitter
     * @param importToolType import tool type (MIA or ITF)
     * @param exportResult   export result
     */
    public void sendEventWithExportResult(UUID sseId, SseEmitter emitter,
                                          ImportToolType importToolType,
                                          RequestExportResultResponse exportResult) throws IOException {
        log.debug("Sending sse event for sseId = {}, importToolType = {}", sseId, importToolType);
        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name(SseEventType.EXPORT_FINISHED.name())
                .data(exportResult, MediaType.APPLICATION_JSON);
        emitter.send(event);
    }

    /**
     * Sends event about execution finish.
     *
     * @param sseId                    sse id
     * @param requestExecutionResponse request execution response
     */
    public void sendEventWithExecutionResult(UUID sseId, SseEmitter emitter,
                                             RequestExecutionResponse requestExecutionResponse) {
        log.debug("Sending sse event for sseId = {}", sseId);
        SseEmitter.SseEventBuilder executionEvent = SseEmitter.event()
                .name(SseEventType.EXECUTION_FINISHED.name())
                .data(requestExecutionResponse, MediaType.APPLICATION_JSON);
        try {
            emitter.send(executionEvent);
        } catch (IOException e) {
            log.error("ERROR during sending sse event for sseId {}", sseId, e);
        }
        // connection will be closed right after receiving event
        // need to complete to remove from emitters map
        emitter.complete();
    }

    /**
     * Executes request. After that sends sse event if emitter exists on current itf-lite instance or sends kafka
     * event about execution finish.
     *
     * @param requestEntity request entity
     * @param context       itf context
     * @param token         auth token
     * @param sseId         sse id
     * @param file          diameter dictionary or binary file
     * @param files         files from form-data
     */
    public void processRequestExecution(RequestEntitySaveRequest requestEntity, String context, String token,
                                        UUID sseId, Optional<MultipartFile> file, List<MultipartFile> files,
                                        UUID environmentId) {
        processRequestExecution(requestEntity, context, token, sseId, file, files, environmentId,
                new RequestRuntimeOptions(), null);
    }

    /**
     * Executes request. After that sends sse event if emitter exists on current itf-lite instance or sends kafka
     * event about execution finish.
     *
     * @param requestEntity  request entity
     * @param context        itf context
     * @param token          auth token
     * @param sseId          sse id
     * @param file           diameter dictionary or binary file
     * @param files          files from form-data
     * @param runtimeOptions runtime request options
     */
    public void processRequestExecution(RequestEntitySaveRequest requestEntity, String context, String token,
                                        UUID sseId, Optional<MultipartFile> file, List<MultipartFile> files,
                                        UUID environmentId, RequestRuntimeOptions runtimeOptions,
                                        UUID sessionId) {
        if (!file.isPresent() && sessionId != null) {
            Optional<FileData> existingFileDataOpt = gridFsService.downloadFileBySessionIdAndRequestId(sessionId,
                    requestEntity.getId());
            if (existingFileDataOpt.isPresent()) {
                file = Optional.of(existingFileDataOpt.get());
            }
        }
        ItfLiteExecutionFinishEvent finishEvent =
                new ItfLiteExecutionFinishEvent(sseId, requestEntity.getId(), requestEntity.getTransportType());
        RequestExecutionResponse response = new RequestExecutionResponse();
        try {
            List<FileData> fileDataList = convertListMultipartToFileData(files);
            response = requestService
                    .executeRequest(requestEntity, context, token, sseId, file, environmentId,
                            fileDataList, runtimeOptions);
        } catch (ItfLiteException itfLiteException) {
            setErrorMessageInResponseAndFinishEvent(response, finishEvent, itfLiteException);
            throw itfLiteException;
        }  catch (Exception e) {
            log.error("Error happen during request execution with ID {} in scope of SSE ID {}",
                    requestEntity.getId(), sseId, e);
            setErrorMessageInResponseAndFinishEvent(response, finishEvent, e);
        } finally {
            finishExecutionRequest(response, finishEvent);
        }
    }

    private void setErrorMessageInResponseAndFinishEvent(RequestExecutionResponse response,
                                          ItfLiteExecutionFinishEvent finishEvent,
                                          Exception e) {
        finishEvent.setErrorMessage(e.getMessage());
        ErrorResponseSerializable error = new ErrorResponseSerializable();
        error.setMessage(e.getMessage());
        response.setError(error);
    }

    void finishExecutionRequest(RequestExecutionResponse response, ItfLiteExecutionFinishEvent finishEvent) {
        SseEmitter sseEmitter = getEmitter(finishEvent.getSseId());
        if (sseEmitter != null) {
            if (response == null) {
                generateResponseAndSendToEmitter(sseEmitter, finishEvent);
            } else {
                sendEventWithExecutionResult(finishEvent.getSseId(), sseEmitter, response);
            }
        } else {
            kafkaExecutionFinishSendingService.executionFinishEventSend(finishEvent);
        }
    }

    /**
     * Generate response for SSE and send to emitter.
     *
     * @param sseEmitter           emitter
     * @param executionFinishEvent ItfLiteExecutionFinishEvent
     */
    public void generateResponseAndSendToEmitter(SseEmitter sseEmitter,
                                                 ItfLiteExecutionFinishEvent executionFinishEvent) {
        log.info("Generate response (from kafka or due to exception)");
        RequestExecutionResponse response = new RequestExecutionResponse();
        RequestExecutionDetails executionDetails = requestExecutionHistoryService
                .getExecutionHistoryDetailsBySseId(executionFinishEvent.getSseId());
        HttpRequestExecutionDetails httpExecutionDetails = (HttpRequestExecutionDetails) executionDetails;
        fillHttpExecutionResponse(httpExecutionDetails, executionFinishEvent.getRequestId(), response);

        sendEventWithExecutionResult(executionFinishEvent.getSseId(), sseEmitter, response);
    }

    void fillHttpExecutionResponse(HttpRequestExecutionDetails httpExecutionDetails,
                                   UUID requestId, RequestExecutionResponse response) {
        log.debug("Fetched http details from database: [{}]", httpExecutionDetails);
        modelMapper.map(httpExecutionDetails.getRequestExecution(), response);
        response.setId(requestId);
        response.setBody(httpExecutionDetails.getResponseBody());
        response.setError(httpExecutionDetails.getErrorMessage());
        response.setExecutionId(httpExecutionDetails.getRequestExecution().getId());
        response.setContextVariables(httpExecutionDetails.getContextVariables());
        response.setCookies(httpExecutionDetails.getCookies());
        response.setCookieHeader(httpExecutionDetails.getCookieHeader());
        fillResponseFromResponseBodyByte(httpExecutionDetails, response);

        List<RequestExecutionHeaderResponse> responseHeaders = new ArrayList<>();
        Map<String, List<String>> executionDetailsResponseHeaders = httpExecutionDetails.getResponseHeaders();

        if (executionDetailsResponseHeaders != null) {
            for (Map.Entry<String, List<String>> entry : executionDetailsResponseHeaders.entrySet()) {
                String headerKey = entry.getKey();
                List<String> headerValues = entry.getValue();
                if (headerValues != null) {
                    headerValues.forEach(headerValue ->
                            responseHeaders.add(new RequestExecutionHeaderResponse(headerKey, headerValue)));
                }
            }
            response.setResponseHeaders(responseHeaders);

            RequestBodyType responseBodyType = requestService.getResponseBodyType(executionDetailsResponseHeaders);
            response.setBodyType(responseBodyType);
        }

        log.debug("Configured http response from database: [{}]", response);
    }

    protected List<FileData> convertListMultipartToFileData(List<MultipartFile> files) {
        List<FileData> res = new ArrayList<>();
        if (files != null) {
            files.forEach(multipartFile -> {
                String fileName = multipartFile.getOriginalFilename();
                try {
                    res.add(new FileData(multipartFile.getBytes(), fileName));
                } catch (IOException e) {
                    log.error("Unable put to list for file = {}", fileName, e);
                }
            });
        }
        return res;
    }

    private void fillResponseFromResponseBodyByte(RequestExecutionDetails executionDetails,
                                                  RequestExecutionResponse response) {
        if (Objects.isNull(executionDetails.getResponseBody())
                && Objects.nonNull(executionDetails.getResponseBodyByte())) {
            response.setBody(new String(executionDetails.getResponseBodyByte(), StandardCharsets.UTF_8));
        }
    }
}
