package org.qubership.atp.itf.lite.backend.service.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateItfExportFailedResponseEvent;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateItfExportSuccessResponseEvent;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateMiaExportFailedResponseEvent;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateMiaExportSuccessResponseEvent;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestExportEntity;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestExportEntityBySseId;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestItfExportRequest;
import static org.qubership.atp.itf.lite.backend.utils.CompareEntitiesUtils.compareEvents;
import static org.qubership.atp.itf.lite.backend.utils.Constants.ATP_EXPORT_FINISHED_TEMPLATE;
import static org.qubership.atp.itf.lite.backend.utils.Constants.ITF_DESTINATION_TEMPLATE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.integration.configuration.model.notification.Notification;
import org.qubership.atp.integration.configuration.service.NotificationService;
import org.qubership.atp.itf.lite.backend.components.export.RequestExportStrategiesRegistry;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestExportRepository;
import org.qubership.atp.itf.lite.backend.enums.ImportToolType;
import org.qubership.atp.itf.lite.backend.enums.RequestExportStatus;
import org.qubership.atp.itf.lite.backend.enums.SseEventType;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfExportResponseEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.MiaExportResponseEvent;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestItfExportRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExportResultResponse;
import org.qubership.atp.itf.lite.backend.model.entities.RequestExportEntity;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.service.RequestExportExceptionResponseService;
import org.qubership.atp.itf.lite.backend.service.RequestExportService;
import org.qubership.atp.itf.lite.backend.service.RequestService;
import org.qubership.atp.itf.lite.backend.service.SseEmitterService;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
public class KafkaExportEventResponseServiceTest {

    private final ThreadLocal<SseEmitterService> sseEmitterService = new ThreadLocal<>();
    private final ThreadLocal<RequestExportRepository> requestExportRepository = new ThreadLocal<>();
    private final ThreadLocal<RequestExportStrategiesRegistry> exportStrategiesRegistry = new ThreadLocal<>();
    private final ThreadLocal<RequestService> requestService = new ThreadLocal<>();
    private final ThreadLocal<NotificationService> notificationService = new ThreadLocal<>();
    private final ThreadLocal<RequestExportExceptionResponseService> requestExportExceptionResponseService = new ThreadLocal<>();
    private final ThreadLocal<KafkaExportEventResponseService> kafkaExportEventResponseService = new ThreadLocal<>();
    private final ThreadLocal<RequestExportService> requestExportService = new ThreadLocal<>();


    @BeforeEach
    public void setUp() {
        SseEmitterService sseEmitterServiceMock = mock(SseEmitterService.class);
        RequestExportRepository requestExportRepositoryMock = mock(RequestExportRepository.class);
        RequestExportStrategiesRegistry exportStrategiesRegistryMock = mock(RequestExportStrategiesRegistry.class);
        RequestService requestServiceMock = mock(RequestService.class);
        NotificationService notificationServiceMock = mock(NotificationService.class);
        RequestExportExceptionResponseService requestExportExceptionResponseServiceMock = mock(RequestExportExceptionResponseService.class);
        RequestExportService requestExportServiceMock = new RequestExportService(requestExportRepositoryMock, exportStrategiesRegistryMock,
                requestServiceMock, sseEmitterServiceMock, notificationServiceMock, requestExportExceptionResponseServiceMock);
        KafkaExportEventResponseService kafkaExportEventResponseServiceMock = new KafkaExportEventResponseService(
                sseEmitterServiceMock, requestExportServiceMock);
        sseEmitterService.set(sseEmitterServiceMock);
        requestExportRepository.set(requestExportRepositoryMock);
        exportStrategiesRegistry.set(exportStrategiesRegistryMock);
        requestService.set(requestServiceMock);
        notificationService.set(notificationServiceMock);
        requestExportExceptionResponseService.set(requestExportExceptionResponseServiceMock);
        kafkaExportEventResponseService.set(kafkaExportEventResponseServiceMock);
        requestExportService.set(requestExportServiceMock);
    }

    @Test
    public void listenMiaExportSuccessAndFailedResponseTest_shouldSuccessfullySendEvent() throws IOException {
        // given
        final UUID requestExportId = UUID.randomUUID();
        final UUID sseId = UUID.randomUUID();
        HttpRequest httpRequest = generateRandomHttpRequest();
        RequestExportEntity requestExportEntity = generateRequestExportEntityBySseId(sseId, httpRequest.getId());
        MiaExportResponseEvent expectedMiaExportSuccessResponse = generateMiaExportSuccessResponseEvent(requestExportId,
                httpRequest);
        SseEmitter sseEmitter = mock(SseEmitter.class);
        ArgumentCaptor<SseEmitter.SseEventBuilder> sseEventBuilderArgumentCaptor =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        RequestExportResultResponse expectedExportSuccessResult = RequestExportResultResponse.builder()
                .requestId(expectedMiaExportSuccessResponse.getRequestId())
                .requestUrl(expectedMiaExportSuccessResponse.getMiaUrl())
                .errorDescription(expectedMiaExportSuccessResponse.getErrorMessage())
                .status(RequestExportStatus.valueOf(expectedMiaExportSuccessResponse.getStatus()))
                .build();

        MiaExportResponseEvent expectedMiaExportFailedResponse = generateMiaExportFailedResponseEvent(requestExportId,
                httpRequest);
        RequestExportResultResponse expectedExportFailedResult = RequestExportResultResponse.builder()
                .requestId(expectedMiaExportFailedResponse.getRequestId())
                .requestUrl(expectedMiaExportFailedResponse.getMiaUrl())
                .errorDescription(expectedMiaExportFailedResponse.getErrorMessage())
                .status(RequestExportStatus.valueOf(expectedMiaExportFailedResponse.getStatus()))
                .build();
        SseEmitter.SseEventBuilder expectedEventWithSuccess = SseEmitter.event()
                .name(SseEventType.EXPORT_FINISHED.name())
                .data(expectedExportSuccessResult, MediaType.APPLICATION_JSON);
        SseEmitter.SseEventBuilder expectedEventWithFail = SseEmitter.event()
                .name(SseEventType.EXPORT_FINISHED.name())
                .data(expectedExportFailedResult, MediaType.APPLICATION_JSON);
        // when
        when(sseEmitterService.get().getEmitter(any())).thenReturn(sseEmitter);
        doCallRealMethod().when(sseEmitterService.get()).sendEventWithExportResult(any(), any(), any(), any());
        when(requestExportService.get().findByRequestExportId(any())).thenReturn(requestExportEntity);

        kafkaExportEventResponseService.get().listenMiaExportResponse(expectedMiaExportSuccessResponse);
        kafkaExportEventResponseService.get().listenMiaExportResponse(expectedMiaExportFailedResponse);
        // then
        verify(sseEmitter, times(2)).send(sseEventBuilderArgumentCaptor.capture());
        assertEquals(2, sseEventBuilderArgumentCaptor.getAllValues().size());
        compareEvents(expectedEventWithSuccess, sseEventBuilderArgumentCaptor.getAllValues().get(0));
        compareEvents(expectedEventWithFail, sseEventBuilderArgumentCaptor.getAllValues().get(1));
    }

    @Test
    public void listenMiaExportTest_twoRequestsWithSuccessAndFailedExport_exportFinished_shouldSuccessfullySendEvent() throws IOException {
        // given
        final UUID requestExportId = UUID.randomUUID();
        final UUID sseId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        Map<UUID, RequestExportStatus> requestStatuses = new HashMap<>();
        HttpRequest httpRequest1 = generateRandomHttpRequest();
        requestStatuses.put(httpRequest1.getId(), RequestExportStatus.IN_PROGRESS);
        HttpRequest httpRequest2 = generateRandomHttpRequest();
        requestStatuses.put(httpRequest2.getId(), RequestExportStatus.IN_PROGRESS);
        RequestExportEntity requestExportEntity = generateRequestExportEntity(requestExportId, sseId, userId,
                requestStatuses);
        String miaPath = "path/to/mia";
        requestExportEntity.setDestination(miaPath);
        MiaExportResponseEvent expectedMiaExportSuccessResponse = generateMiaExportSuccessResponseEvent(sseId,
                httpRequest1);
        SseEmitter sseEmitter = mock(SseEmitter.class);
        ArgumentCaptor<SseEmitter.SseEventBuilder> sseEventBuilderArgumentCaptor =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        RequestExportResultResponse expectedExportSuccessResult = RequestExportResultResponse.builder()
                .requestId(expectedMiaExportSuccessResponse.getRequestId())
                .requestUrl(expectedMiaExportSuccessResponse.getMiaUrl())
                .errorDescription(expectedMiaExportSuccessResponse.getErrorMessage())
                .status(RequestExportStatus.valueOf(expectedMiaExportSuccessResponse.getStatus()))
                .build();

        MiaExportResponseEvent expectedMiaExportFailedResponse = generateMiaExportFailedResponseEvent(sseId, httpRequest2);
        RequestExportResultResponse expectedExportFailedResult = RequestExportResultResponse.builder()
                .requestId(expectedMiaExportFailedResponse.getRequestId())
                .requestUrl(expectedMiaExportFailedResponse.getMiaUrl())
                .errorDescription(expectedMiaExportFailedResponse.getErrorMessage())
                .status(RequestExportStatus.valueOf(expectedMiaExportFailedResponse.getStatus()))
                .build();
        SseEmitter.SseEventBuilder expectedEventWithSuccess = SseEmitter.event()
                .name(SseEventType.EXPORT_FINISHED.name())
                .data(expectedExportSuccessResult, MediaType.APPLICATION_JSON);
        SseEmitter.SseEventBuilder expectedEventWithFail = SseEmitter.event()
                .name(SseEventType.EXPORT_FINISHED.name())
                .data(expectedExportFailedResult, MediaType.APPLICATION_JSON);

        String message = String.format(ATP_EXPORT_FINISHED_TEMPLATE,
                ImportToolType.MIA.name(), requestExportEntity.getDestination());
        Notification expectedNotification = new Notification(
                message, Notification.Type.INFO, requestExportEntity.getUserId());
        // when
        when(sseEmitterService.get().getEmitter(any())).thenReturn(sseEmitter);
        doCallRealMethod().when(sseEmitterService.get()).sendEventWithExportResult(any(), any(), any(), any());
        when(requestExportRepository.get().findByRequestExportId(any())).thenReturn(requestExportEntity);

        kafkaExportEventResponseService.get().listenMiaExportResponse(expectedMiaExportSuccessResponse);
        kafkaExportEventResponseService.get().listenMiaExportResponse(expectedMiaExportFailedResponse);
        // then
        verify(sseEmitter, times(2)).send(sseEventBuilderArgumentCaptor.capture());
        assertEquals(2, sseEventBuilderArgumentCaptor.getAllValues().size());
        compareEvents(expectedEventWithSuccess, sseEventBuilderArgumentCaptor.getAllValues().get(0));
        compareEvents(expectedEventWithFail, sseEventBuilderArgumentCaptor.getAllValues().get(1));

        verify(requestExportRepository.get()).deleteByRequestExportId(any());

        ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService.get()).sendNotification(notificationArgumentCaptor.capture());
        assertEquals(expectedNotification, notificationArgumentCaptor.getValue());
    }

    @Test
    public void listenItfExportSuccessAndFailedResponseTest_shouldSuccessfullySendEvent() throws IOException {
        // given
        final UUID requestExportId = UUID.randomUUID();
        final UUID sseId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        HttpRequest httpRequest = generateRandomHttpRequest();
        RequestExportEntity requestExportEntity = generateRequestExportEntityBySseId(sseId, httpRequest.getId());
        ItfExportResponseEvent expectedItfExportSuccessResponseEvent = generateItfExportSuccessResponseEvent(
                requestExportId, httpRequest);
        SseEmitter sseEmitter = mock(SseEmitter.class);
        ArgumentCaptor<SseEmitter.SseEventBuilder> sseEventBuilderArgumentCaptor =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        RequestExportResultResponse expectedExportSuccessResult = RequestExportResultResponse.builder()
                .requestId(expectedItfExportSuccessResponseEvent.getRequestId())
                .requestUrl(expectedItfExportSuccessResponseEvent.getItfRequestUrl())
                .errorDescription(expectedItfExportSuccessResponseEvent.getErrorMessage())
                .status(RequestExportStatus.valueOf(expectedItfExportSuccessResponseEvent.getStatus()))
                .build();

        ItfExportResponseEvent expectedItfExportFailedResponseEvent = generateItfExportFailedResponseEvent(
                requestExportId, httpRequest);
        RequestExportResultResponse expectedExportFailedResult = RequestExportResultResponse.builder()
                .requestId(expectedItfExportFailedResponseEvent.getRequestId())
                .requestUrl(expectedItfExportFailedResponseEvent.getItfRequestUrl())
                .errorDescription(expectedItfExportFailedResponseEvent.getErrorMessage())
                .status(RequestExportStatus.valueOf(expectedItfExportFailedResponseEvent.getStatus()))
                .build();
        SseEmitter.SseEventBuilder expectedEventWithSuccess = SseEmitter.event()
                .name(SseEventType.EXPORT_FINISHED.name())
                .data(expectedExportSuccessResult, MediaType.APPLICATION_JSON);
        SseEmitter.SseEventBuilder expectedEventWithFail = SseEmitter.event()
                .name(SseEventType.EXPORT_FINISHED.name())
                .data(expectedExportFailedResult, MediaType.APPLICATION_JSON);
        // when
        when(sseEmitterService.get().getEmitter(any())).thenReturn(sseEmitter);
        doCallRealMethod().when(sseEmitterService.get()).sendEventWithExportResult(any(), any(), any(), any());
        when(requestExportService.get().findByRequestExportId(any())).thenReturn(requestExportEntity);

        kafkaExportEventResponseService.get().listenItfExportResponse(expectedItfExportSuccessResponseEvent);
        kafkaExportEventResponseService.get().listenItfExportResponse(expectedItfExportFailedResponseEvent);
        // then
        verify(sseEmitter, times(2)).send(sseEventBuilderArgumentCaptor.capture());
        assertEquals(2, sseEventBuilderArgumentCaptor.getAllValues().size());
        compareEvents(expectedEventWithSuccess, sseEventBuilderArgumentCaptor.getAllValues().get(0));
        compareEvents(expectedEventWithFail, sseEventBuilderArgumentCaptor.getAllValues().get(1));
    }

    @Test
    public void listenItfExportTest_twoRequestsWithSuccessAndFailedExport_exportFinished_shouldSuccessfullySendEvent() throws IOException {
        // given
        final UUID requestExportId = UUID.randomUUID();
        final UUID sseId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        Map<UUID, RequestExportStatus> requestStatuses = new HashMap<>();
        HttpRequest httpRequest1 = generateRandomHttpRequest();
        requestStatuses.put(httpRequest1.getId(), RequestExportStatus.IN_PROGRESS);
        RequestExportEntity requestExportEntity = generateRequestExportEntity(requestExportId, sseId, userId,
                requestStatuses);
        RequestItfExportRequest requestItfExportRequest = generateRequestItfExportRequest();
        String itfDestination = String.format(ITF_DESTINATION_TEMPLATE, requestItfExportRequest.getItfUrl(),
                requestItfExportRequest.getSystemId(), requestItfExportRequest.getOperationId());
        requestExportEntity.setDestination(itfDestination);
        ItfExportResponseEvent expectedItfExportSuccessResponseEvent = generateItfExportSuccessResponseEvent(sseId,
                httpRequest1);
        SseEmitter sseEmitter = mock(SseEmitter.class);
        ArgumentCaptor<SseEmitter.SseEventBuilder> sseEventBuilderArgumentCaptor =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        RequestExportResultResponse expectedExportSuccessResult = RequestExportResultResponse.builder()
                .requestId(expectedItfExportSuccessResponseEvent.getRequestId())
                .requestUrl(expectedItfExportSuccessResponseEvent.getItfRequestUrl())
                .errorDescription(expectedItfExportSuccessResponseEvent.getErrorMessage())
                .status(RequestExportStatus.valueOf(expectedItfExportSuccessResponseEvent.getStatus()))
                .build();

        SseEmitter.SseEventBuilder expectedEventWithSuccess = SseEmitter.event()
                .name(SseEventType.EXPORT_FINISHED.name())
                .data(expectedExportSuccessResult, MediaType.APPLICATION_JSON);

        String message = String.format(ATP_EXPORT_FINISHED_TEMPLATE,
                ImportToolType.ITF.name(), requestExportEntity.getDestination());
        Notification expectedNotification = new Notification(
                message, Notification.Type.INFO, requestExportEntity.getUserId());
        // when
        when(sseEmitterService.get().getEmitter(any())).thenReturn(sseEmitter);
        doCallRealMethod().when(sseEmitterService.get()).sendEventWithExportResult(any(), any(), any(), any());
        when(requestExportRepository.get().findByRequestExportId(any())).thenReturn(requestExportEntity);

        kafkaExportEventResponseService.get().listenItfExportResponse(expectedItfExportSuccessResponseEvent);
        // then
        verify(sseEmitter, times(1)).send(sseEventBuilderArgumentCaptor.capture());
        assertEquals(1, sseEventBuilderArgumentCaptor.getAllValues().size());
        compareEvents(expectedEventWithSuccess, sseEventBuilderArgumentCaptor.getAllValues().get(0));

        verify(requestExportRepository.get()).deleteByRequestExportId(any());

        ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService.get()).sendNotification(notificationArgumentCaptor.capture());
        assertEquals(expectedNotification, notificationArgumentCaptor.getValue());
    }
}
