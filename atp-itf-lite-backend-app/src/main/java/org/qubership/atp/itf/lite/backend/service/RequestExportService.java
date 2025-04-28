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

import static org.qubership.atp.itf.lite.backend.utils.Constants.ATP_EXPORT_FINISHED_TEMPLATE;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.integration.configuration.model.notification.Notification;
import org.qubership.atp.integration.configuration.service.NotificationService;
import org.qubership.atp.itf.lite.backend.components.export.RequestExportStrategiesRegistry;
import org.qubership.atp.itf.lite.backend.components.export.strategies.request.RequestExportStrategy;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestExportRepository;
import org.qubership.atp.itf.lite.backend.enums.ImportToolType;
import org.qubership.atp.itf.lite.backend.enums.RequestExportStatus;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteExportRequestException;
import org.qubership.atp.itf.lite.backend.mdc.ItfLiteMdcField;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestExportRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExportResultResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.RequestExportEntity;
import org.slf4j.MDC;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestExportService extends CrudService<RequestExportEntity> {

    private final RequestExportRepository repository;
    private final RequestExportStrategiesRegistry exportStrategiesRegistry;
    private final RequestService requestService;
    private final SseEmitterService sseEmitterService;
    private final NotificationService notificationService;
    private final RequestExportExceptionResponseService requestExportExceptionResponseService;

    /**
     * Export request.
     *
     * @param sseId sse id
     * @param userId user id
     * @param exportRequest  export request
     * @param importToolType import tool type
     * @param context itf context
     */
    public void exportRequests(UUID sseId, UUID userId, RequestExportRequest exportRequest,
                               ImportToolType importToolType, String context,
                               UUID environmentId) {
        UUID exportRequestId = UUID.randomUUID();
        try {
            List<Request> requests = requestService.getAllRequestsByProjectIdFolderIdsRequestIds(
                    exportRequest.getProjectId(), null, exportRequest.getRequestIds());
            saveExportRequest(exportRequestId, sseId, userId, exportRequest);
            for (Request request : requests) {
                MdcUtils.put(ItfLiteMdcField.REQUEST_ID.toString(), request.getId());
                TransportType requestTransportType = request.getTransportType();
                try {
                    RequestExportStrategy exportStrategy =
                            exportStrategiesRegistry.getStrategy(importToolType, requestTransportType);
                    exportStrategy.export(exportRequestId, exportRequest, request, context, environmentId);
                } catch (Exception err) {
                    processExceptionDuringExport(exportRequestId, sseId, request.getId(), importToolType, err);
                }
                MDC.remove(ItfLiteMdcField.REQUEST_ID.toString());
            }
        } catch (Exception e) {
            String errorMessage = "Failed to export requests.\n" + e.getMessage();
            log.error("Failed to export requests", e);
            removeFinishedExport(exportRequestId);
            SseEmitter emitter = sseEmitterService.getEmitter(sseId);
            if (emitter != null) {
                sseEmitterService.emitterCompleteWithError(emitter, new ItfLiteExportRequestException());
            } else {
                // send kafka events with exception
                log.debug("Send kafka event for each request, exportRequestId = {}", exportRequestId);
                exportRequest.getRequestIds().forEach(requestId -> {
                        requestExportExceptionResponseService.sendExceptionResponseEvent(
                                importToolType, exportRequestId, requestId, errorMessage);
                });
            }
            // send notification
            log.debug("Send notification about failed export with exportRequestId = {}", exportRequestId);
            Notification notification = new Notification(errorMessage, Notification.Type.ERROR, userId);
            notificationService.sendNotification(notification);
        }
    }

    private void processExceptionDuringExport(UUID exportRequestId, UUID sseId, UUID requestId,
                                              ImportToolType importToolType, Exception err) throws IOException {
        log.error("Failed to export request with id '{}'\n{}", requestId, err.getMessage(), err);
        SseEmitter emitter = sseEmitterService.getEmitter(sseId);
        if (emitter != null) {
            RequestExportEntity requestExportEntity = findByRequestExportId(exportRequestId);
            // notify about failed export
            RequestExportResultResponse exportResultWithError = RequestExportResultResponse.builder()
                    .requestId(requestId)
                    .errorDescription(err.getMessage())
                    .status(RequestExportStatus.ERROR)
                    .build();
            processExportResult(requestExportEntity, exportResultWithError, emitter, importToolType);
        } else {
            requestExportExceptionResponseService.sendExceptionResponseEvent(
                    importToolType, exportRequestId, requestId, err.getMessage());
        }
    }

    /**
     * Saves export request.
     * @param requestExportId request export id
     * @param sseId sse id
     * @param userId user id
     * @param requestExportRequest request export request
     */
    public void saveExportRequest(UUID requestExportId, UUID sseId, UUID userId,
                                  RequestExportRequest requestExportRequest) {
        RequestExportEntity requestExportEntity = new RequestExportEntity();
        requestExportEntity.setRequestExportId(requestExportId);
        requestExportEntity.setSseId(sseId);
        requestExportEntity.setUserId(userId);
        Map<UUID, RequestExportStatus> requestStatuses = new HashMap<>();
        requestExportRequest.getRequestIds()
                .forEach(requestId -> requestStatuses.put(requestId, RequestExportStatus.IN_PROGRESS));
        requestExportEntity.setRequestStatuses(requestStatuses);
        requestExportEntity.setDestination(requestExportRequest.getDestination());
        save(requestExportEntity);
    }

    /**
     * Update request id status.
     * @param requestExportEntity request export entity
     * @param requestId request id
     * @param status new status
     */
    public void updateRequestIdStatus(RequestExportEntity requestExportEntity, UUID requestId,
                                      RequestExportStatus status) {
        if (!CollectionUtils.isEmpty(requestExportEntity.getRequestStatuses())) {
            Map<UUID, RequestExportStatus> requestStatuses = requestExportEntity.getRequestStatuses();
            requestStatuses.put(requestId, status);
            save(requestExportEntity);
            return;
        }
        log.error("Request export entity with export id = {} has empty request statuses map",
                requestExportEntity.getRequestExportId());
    }

    /**
     * Check if all requests are processed.
     * @param requestExportEntity request export entity
     * @return true if export finished
     */
    public boolean isExportFinished(RequestExportEntity requestExportEntity) {
        Map<UUID, RequestExportStatus> requestStatuses = requestExportEntity.getRequestStatuses();
        return requestStatuses.entrySet().stream()
                .allMatch(entry -> entry.getValue() == RequestExportStatus.DONE
                        || entry.getValue() == RequestExportStatus.ERROR);
    }

    public void removeFinishedExport(UUID requestExportId) {
        repository.deleteByRequestExportId(requestExportId);
    }

    public RequestExportEntity findByRequestExportId(UUID requestExportId) {
        return repository.findByRequestExportId(requestExportId);
    }

    /**
     * Prepares and sends export result to sse emitter and notification service.
     * @param requestExportEntity request export entity
     * @param exportResult export result
     * @param sseEmitter sse emitter
     * @param importToolType import tool type
     * @throws IOException io exception in sse emitter send
     */
    @Transactional
    public void processExportResult(RequestExportEntity requestExportEntity,
                                    RequestExportResultResponse exportResult, SseEmitter sseEmitter,
                                    ImportToolType importToolType) throws IOException {
        log.debug("Send sse event with export result for sseId = {}", requestExportEntity.getSseId());
        updateRequestIdStatus(requestExportEntity, exportResult.getRequestId(), exportResult.getStatus());
        sseEmitterService.sendEventWithExportResult(requestExportEntity.getSseId(),
                sseEmitter, importToolType, exportResult);
        if (isExportFinished(requestExportEntity)) {
            String message = String.format(ATP_EXPORT_FINISHED_TEMPLATE, importToolType.name(),
                    requestExportEntity.getDestination());
            Notification notification = new Notification(
                    message, Notification.Type.INFO, requestExportEntity.getUserId());
            removeFinishedExport(requestExportEntity.getRequestExportId());
            // need to complete to remove from emitters map
            sseEmitter.complete();
            notificationService.sendNotification(notification);
        }
    }

    @Override
    protected JpaRepository<RequestExportEntity, UUID> repository() {
        return repository;
    }
}
