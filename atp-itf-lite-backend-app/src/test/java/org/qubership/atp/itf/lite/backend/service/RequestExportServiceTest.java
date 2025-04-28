package org.qubership.atp.itf.lite.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestExportEntity;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestItfExportRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestMiaExportRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.integration.configuration.service.NotificationService;
import org.qubership.atp.itf.lite.backend.components.export.RequestExportStrategiesRegistry;
import org.qubership.atp.itf.lite.backend.components.export.strategies.request.ItfRestRequestExportStrategy;
import org.qubership.atp.itf.lite.backend.components.export.strategies.request.MiaRestRequestExportStrategy;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestExportRepository;
import org.qubership.atp.itf.lite.backend.enums.ImportToolType;
import org.qubership.atp.itf.lite.backend.enums.RequestExportStatus;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestItfExportRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestMiaExportRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExportResultResponse;
import org.qubership.atp.itf.lite.backend.model.entities.RequestExportEntity;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
public class RequestExportServiceTest {

    private final ThreadLocal<RequestExportRepository> requestExportRepository = new ThreadLocal<>();
    private final ThreadLocal<RequestExportStrategiesRegistry> exportStrategiesRegistry = new ThreadLocal<>();
    private final ThreadLocal<RequestService> requestService = new ThreadLocal<>();
    private final ThreadLocal<SseEmitterService> sseEmitterService = new ThreadLocal<>();
    private final ThreadLocal<NotificationService> notificationService = new ThreadLocal<>();
    private final ThreadLocal<RequestExportExceptionResponseService> requestExportExceptionResponseService = new ThreadLocal<>();
    private final ThreadLocal<RequestExportService> requestExportService = new ThreadLocal<>();

    private static final UUID sseId = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        RequestExportRepository requestExportRepositoryMock = mock(RequestExportRepository.class);
        RequestExportStrategiesRegistry exportStrategiesRegistryMock = mock(RequestExportStrategiesRegistry.class);
        RequestService requestServiceMock = mock(RequestService.class);
        SseEmitterService sseEmitterServiceMock = mock(SseEmitterService.class);
        NotificationService notificationServiceMock = mock(NotificationService.class);
        RequestExportExceptionResponseService requestExportExceptionResponseServiceMock = mock(RequestExportExceptionResponseService.class);
        requestExportRepository.set(requestExportRepositoryMock);
        exportStrategiesRegistry.set(exportStrategiesRegistryMock);
        requestService.set(requestServiceMock);
        sseEmitterService.set(sseEmitterServiceMock);
        notificationService.set(notificationServiceMock);
        requestExportExceptionResponseService.set(requestExportExceptionResponseServiceMock);
        requestExportService.set(new RequestExportService(requestExportRepositoryMock, exportStrategiesRegistryMock,
                requestServiceMock, sseEmitterServiceMock, notificationServiceMock, requestExportExceptionResponseServiceMock));

    }

    @Test
    public void exportRequestsToMiaTest_exceptionDuringExport_shouldSendEventWithErrorMessage() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        RequestMiaExportRequest miaExportRequestEvent = generateRequestMiaExportRequest();
        ImportToolType importToolType = ImportToolType.MIA;
        HttpRequest httpRequest = generateRandomHttpRequest();
        MiaRestRequestExportStrategy miaRestRequestExportStrategy = mock(MiaRestRequestExportStrategy.class);
        Map<UUID, RequestExportStatus> statuses = new HashMap<>();
        statuses.put(httpRequest.getId(), RequestExportStatus.ERROR);
        RequestExportEntity requestExportEntity = generateRequestExportEntity(UUID.randomUUID(), sseId, userId,
                statuses);
        SseEmitter sseEmitter = mock(SseEmitter.class);

        // when
        when(requestService.get().getAllRequestsByProjectIdFolderIdsRequestIds(any(), any(), any()))
                .thenReturn(Collections.singletonList(httpRequest));
        when(requestExportRepository.get().findByRequestExportId(any())).thenReturn(requestExportEntity);

        when(exportStrategiesRegistry.get().getStrategy(any(), any())).thenReturn(miaRestRequestExportStrategy);

        when(sseEmitterService.get().getEmitter(any())).thenReturn(sseEmitter);

        String runtimeExceptionMessage = "Exception during export";
        doThrow(new RuntimeException(runtimeExceptionMessage)).when(miaRestRequestExportStrategy)
                .export(any(), any(), any(), any(), any());

        requestExportService.get().exportRequests(sseId, userId, miaExportRequestEvent, importToolType, null, null);
        // then
        ArgumentCaptor<RequestExportResultResponse> requestExportResultResponseArgumentCaptor = ArgumentCaptor.forClass(
                RequestExportResultResponse.class);
        verify(sseEmitterService.get(), times(1)).sendEventWithExportResult(any(), any(), any(),
                requestExportResultResponseArgumentCaptor.capture());

        RequestExportResultResponse expectedExportResultWithError = RequestExportResultResponse.builder()
                .requestId(httpRequest.getId())
                .errorDescription(runtimeExceptionMessage)
                .status(RequestExportStatus.ERROR)
                .build();
        assertThat(requestExportResultResponseArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expectedExportResultWithError);
    }

    @Test
    public void saveExportRequestTest_miaExport_shouldSuccessfullyExecuted() {
        // given
        UUID requestExportId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RequestMiaExportRequest requestMiaExportRequest = new RequestMiaExportRequest();
        UUID requestId = UUID.randomUUID();
        Set<UUID> requestIds = Collections.singleton(requestId);
        requestMiaExportRequest.setRequestIds(requestIds);
        String miaPath = "path/to/mia";
        requestMiaExportRequest.setMiaPath(miaPath);
        // when
        requestExportService.get().saveExportRequest(requestExportId, sseId, userId,
                requestMiaExportRequest);
        // then
        ArgumentCaptor<RequestExportEntity> requestExportEntityArgumentCaptor =
                ArgumentCaptor.forClass(RequestExportEntity.class);
        verify(requestExportRepository.get()).save(requestExportEntityArgumentCaptor.capture());
        RequestExportEntity actualEntity = requestExportEntityArgumentCaptor.getValue();
        assertEquals(requestExportId, actualEntity.getRequestExportId());
        assertEquals(sseId, actualEntity.getSseId());
        assertEquals(userId, actualEntity.getUserId());
        assertEquals(miaPath, actualEntity.getDestination());
        assertNotNull(actualEntity.getRequestStatuses());
        assertEquals(1, actualEntity.getRequestStatuses().size());
        assertEquals(RequestExportStatus.IN_PROGRESS, actualEntity.getRequestStatuses().get(requestId));
    }

    @Test
    public void updateRequestIdStatusTest_shouldSuccessfullyExecuted() {
        // given
        UUID requestExportId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        Map<UUID, RequestExportStatus> requestIdsStatuses = new HashMap<>();
        requestIdsStatuses.put(requestId, RequestExportStatus.IN_PROGRESS);
        RequestExportEntity requestExportEntity = generateRequestExportEntity(requestExportId, sseId, userId,
                requestIdsStatuses);
        // when
        requestExportService.get().updateRequestIdStatus(requestExportEntity, requestId, RequestExportStatus.DONE);
        // then
        ArgumentCaptor<RequestExportEntity> requestExportEntityArgumentCaptor =
                ArgumentCaptor.forClass(RequestExportEntity.class);
        verify(requestExportRepository.get()).save(requestExportEntityArgumentCaptor.capture());
        RequestExportEntity actualEntity = requestExportEntityArgumentCaptor.getValue();
        assertNotNull(actualEntity.getRequestStatuses());
        assertEquals(1, actualEntity.getRequestStatuses().size());
        assertEquals(RequestExportStatus.DONE, actualEntity.getRequestStatuses().get(requestId));
    }

    @Test
    public void isExportFinishedTest_shouldSuccessfullyExecuted() {
        // given
        UUID requestId = UUID.randomUUID();
        Map<UUID, RequestExportStatus> finishedRequestIdsStatuses = new HashMap<>();
        finishedRequestIdsStatuses.put(requestId, RequestExportStatus.DONE);
        RequestExportEntity requestExportFinishedEntity = generateRequestExportEntity(null,null, null,
                finishedRequestIdsStatuses);
        Map<UUID, RequestExportStatus> unfinishedRequestIdsStatuses = new HashMap<>();
        unfinishedRequestIdsStatuses.put(requestId, RequestExportStatus.IN_PROGRESS);
        RequestExportEntity requestExportUnfinishedEntity = generateRequestExportEntity(null,null, null,
                unfinishedRequestIdsStatuses);
        // when
        // then
        assertTrue(requestExportService.get().isExportFinished(requestExportFinishedEntity));
        assertFalse(requestExportService.get().isExportFinished(requestExportUnfinishedEntity));
    }

    @Test
    public void removeFinishedExportTest_shouldSuccessfullyExecuted() {
        // given
        UUID exportRequestId = UUID.randomUUID();
        // when
        requestExportService.get().removeFinishedExport(exportRequestId);
        // then
        verify(requestExportRepository.get()).deleteByRequestExportId(any());
    }

    @Test
    public void findByRequestExportIdTest_shouldSuccessfullyExecuted() {
        // given
        UUID exportRequestId = UUID.randomUUID();
        // when
        requestExportService.get().findByRequestExportId(exportRequestId);
        // then
        verify(requestExportRepository.get()).findByRequestExportId(any());
    }

    @Test
    public void exportRequestsToItfTest_exceptionDuringExport_shouldSendEventWithErrorMessage() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        RequestItfExportRequest itfExportRequest = generateRequestItfExportRequest();
        ImportToolType importToolType = ImportToolType.ITF;
        HttpRequest httpRequest = generateRandomHttpRequest();
        ItfRestRequestExportStrategy itfRestRequestExportStrategy = mock(ItfRestRequestExportStrategy.class);
        Map<UUID, RequestExportStatus> statuses = new HashMap<>();
        RequestExportEntity requestExportEntity = generateRequestExportEntity(UUID.randomUUID(), sseId, userId,
                statuses);
        SseEmitter sseEmitter = mock(SseEmitter.class);

        // when
        when(requestService.get().getAllRequestsByProjectIdFolderIdsRequestIds(any(), any(), any()))
                .thenReturn(Arrays.asList(httpRequest));
        when(exportStrategiesRegistry.get().getStrategy(any(), any()))
                .thenReturn(itfRestRequestExportStrategy);
        when(requestExportRepository.get().findByRequestExportId(any())).thenReturn(requestExportEntity);
        when(sseEmitterService.get().getEmitter(any())).thenReturn(sseEmitter);

        String runtimeRestRequestExceptionMessage = "Exception during rest export";
        doThrow(new RuntimeException(runtimeRestRequestExceptionMessage)).when(itfRestRequestExportStrategy)
                .export(any(), any(), any(), any(), any());
        String runtimeDiameterRequestExceptionMessage = "Exception during diameter export";
        requestExportService.get().exportRequests(sseId, userId, itfExportRequest, importToolType, null, null);
        // then
        ArgumentCaptor<RequestExportResultResponse> requestExportResultResponseArgumentCaptor = ArgumentCaptor.forClass(
                RequestExportResultResponse.class);
        verify(sseEmitterService.get(), times(1)).sendEventWithExportResult(any(), any(), any(),
                requestExportResultResponseArgumentCaptor.capture());
        RequestExportResultResponse expectedRestExportResultWithError = RequestExportResultResponse.builder()
                .requestId(httpRequest.getId())
                .errorDescription(runtimeRestRequestExceptionMessage)
                .status(RequestExportStatus.ERROR)
                .build();
        assertEquals(1, requestExportResultResponseArgumentCaptor.getAllValues().size());
        assertThat(requestExportResultResponseArgumentCaptor.getAllValues().get(0))
                .usingRecursiveComparison()
                .isEqualTo(expectedRestExportResultWithError);
    }

    @Test
    public void exportRequestsTest_exceptionBeforeActualSendingRequest_shouldSendEventWithErrorMessageAndNotificationMessage() {
        // given
        UUID userId = UUID.randomUUID();
        RequestMiaExportRequest miaExportRequest = generateRequestMiaExportRequest();
        RequestItfExportRequest itfExportRequest = generateRequestItfExportRequest();
        SseEmitter sseEmitter = mock(SseEmitter.class);

        // when
        String runtimeRestRequestExceptionMessage = "Exception during rest export";
        doThrow(new RuntimeException(runtimeRestRequestExceptionMessage)).
                when(requestService.get()).getAllRequestsByProjectIdFolderIdsRequestIds(any(), any(), any());

        when(sseEmitterService.get().getEmitter(any()))
                .thenReturn(sseEmitter)
                .thenReturn(null);

        requestExportService.get().exportRequests(sseId, userId, itfExportRequest, ImportToolType.ITF, null, null);
        requestExportService.get().exportRequests(sseId, userId, miaExportRequest, ImportToolType.MIA, null, null);
        // then
        verify(sseEmitterService.get(), times(1)).emitterCompleteWithError(any(), any());
        verify(requestExportRepository.get(), times(2)).deleteByRequestExportId(any());
        verify(notificationService.get(), times(2)).sendNotification(any());
    }
}
