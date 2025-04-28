package org.qubership.atp.itf.lite.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.qubership.atp.integration.configuration.service.NotificationService;
import org.qubership.atp.itf.lite.backend.configuration.MapperConfiguration;
import org.qubership.atp.itf.lite.backend.configuration.SseProperties;
import org.qubership.atp.itf.lite.backend.enums.ContextVariableType;
import org.qubership.atp.itf.lite.backend.exceptions.ExceptionConstants;
import org.qubership.atp.itf.lite.backend.exceptions.ItfLiteException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteHttpRequestExecuteException;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.api.dto.ResponseCookie;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfLiteExecutionFinishEvent;
import org.qubership.atp.itf.lite.backend.model.api.request.ContextVariable;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionHeaderResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionResponse;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.qubership.atp.itf.lite.backend.model.entities.history.HistoryRequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.history.HttpRequestExecutionDetails;
import org.qubership.atp.itf.lite.backend.model.entities.history.RequestExecution;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaExecutionFinishSendingService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
public class SseEmitterServiceTest {

    private final ThreadLocal<RequestService> requestService = new ThreadLocal<>();
    private final ThreadLocal<KafkaExecutionFinishSendingService> kafkaExecutionFinishSendingService = new ThreadLocal<>();
    private final ThreadLocal<SseProperties> sseProperties = new ThreadLocal<>();
    private final ThreadLocal<SseEmitterService> sseEmitterService = new ThreadLocal<>();
    private final ThreadLocal<SseEmitterService> spySseEmitterService = new ThreadLocal<>();

    private static final ModelMapper modelMapper = new MapperConfiguration().modelMapper();
    private static final String executor = "user";

    @BeforeEach
    public void setUp() {
        RequestService requestServiceMock = mock(RequestService.class);
        NotificationService notificationServiceMock = mock(NotificationService.class);
        KafkaExecutionFinishSendingService kafkaExecutionFinishSendingServiceMock = mock(KafkaExecutionFinishSendingService.class);
        RequestExecutionHistoryService requestExecutionHistoryServiceMock = mock(RequestExecutionHistoryService.class);
        SseProperties ssePropertiesMock = mock(SseProperties.class);
        SseEmitterService sseEmitterServiceMock = new SseEmitterService(requestServiceMock, notificationServiceMock,
                kafkaExecutionFinishSendingServiceMock, requestExecutionHistoryServiceMock, modelMapper,
                ssePropertiesMock, mock(GridFsService.class));

        requestService.set(requestServiceMock);
        kafkaExecutionFinishSendingService.set(kafkaExecutionFinishSendingServiceMock);
        sseProperties.set(ssePropertiesMock);
        sseEmitterService.set(sseEmitterServiceMock);
        spySseEmitterService.set(spy(sseEmitterServiceMock));
    }

    @Test
    public void fillHttpExecutionResponse_successfullySetContextVariables() {
        // given
        RequestExecutionResponse response = new RequestExecutionResponse();
        HttpRequestExecutionDetails httpExecutionDetails = new HttpRequestExecutionDetails();

        RequestExecution requestExecution = new RequestExecution();

        ContextVariable contextVariable = new ContextVariable("anyKey", "anyObj", ContextVariableType.GLOBAL);
        List<ContextVariable> list = new ArrayList<>();
        list.add(contextVariable);
        httpExecutionDetails.setContextVariables(list);
        UUID requestId = UUID.randomUUID();

        ResponseCookie responseCookie = new ResponseCookie("name", "value");
        HttpHeaderSaveRequest cookieHeader = new HttpHeaderSaveRequest("key", "value", "descr");

        httpExecutionDetails.setRequestExecution(requestExecution);
        httpExecutionDetails.setCookieHeader(cookieHeader);
        httpExecutionDetails.setCookies(Collections.singletonList(responseCookie));
        httpExecutionDetails.setResponseBodyByte("test".getBytes());
        // when
        sseEmitterService.get().fillHttpExecutionResponse(httpExecutionDetails, requestId, response);

        // then
        assertEquals(contextVariable, response.getContextVariables().get(0));
        assertEquals(cookieHeader, response.getCookieHeader());
        assertEquals(responseCookie, response.getCookies().get(0));
        assertEquals("test", response.getBody());
    }

    @Test
    public void fillHttpExecutionResponse_successfullySetResponseHeaders() {
        // given
        RequestExecutionResponse response = new RequestExecutionResponse();
        HttpRequestExecutionDetails httpExecutionDetails = new HttpRequestExecutionDetails();

        String headerKey = "key";
        List<String> headerValues = Arrays.asList("value1", "value2");
        httpExecutionDetails.setResponseHeaders(Collections.singletonMap(headerKey, headerValues));
        UUID requestId = UUID.randomUUID();

        RequestExecution requestExecution = new RequestExecution();
        httpExecutionDetails.setRequestExecution(requestExecution);
        // when
        sseEmitterService.get().fillHttpExecutionResponse(httpExecutionDetails, requestId, response);

        // then
        assertEquals(2, response.getResponseHeaders().size());
        assertEquals(headerKey, response.getResponseHeaders().get(0).getKey());
        assertEquals(headerValues.get(0), response.getResponseHeaders().get(0).getValue());
        assertEquals(headerKey, response.getResponseHeaders().get(1).getKey());
        assertEquals(headerValues.get(1), response.getResponseHeaders().get(1).getValue());
    }

    @Test
    public void generateAndConfigureEmitterTest_successfullyCreated() {
        // given
        final UUID sseId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();

        // when
        when(sseProperties.get().getSseEmitterTimeout()).thenReturn(1L);
        SseEmitter actualEmitter = sseEmitterService.get().generateAndConfigureEmitter(sseId, userId);
        // then
        assertNotNull(actualEmitter);
    }

    @Test
    public void getEmitterTest_emitterAlreadyExists_successfullyReturned() {
        // given
        final UUID sseId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        when(sseProperties.get().getSseEmitterTimeout()).thenReturn(10L);
        SseEmitter expectedEmitter = sseEmitterService.get().generateAndConfigureEmitter(sseId, userId);
        // when
        SseEmitter actualEmitter = sseEmitterService.get().getEmitter(sseId);
        // then
        assertEquals(expectedEmitter, actualEmitter);
    }

    @Test
    public void getEmitterTest_emitterDoesNotExistAndRequiredEmitterTrue_nullReturned() {
        // given
        final UUID sseId = UUID.randomUUID();

        // when
        SseEmitter actualEmitter = sseEmitterService.get().getEmitter(sseId);
        // then
        assertNull(actualEmitter);
    }

    @Test
    public void emitterCompleteWithErrorTest_sendNotification() {
        // given
        SseEmitter expectedEmitter = mock(SseEmitter.class);
        String errorMessage = "Exception";
        String expectedExceptionMessage = String.format(ExceptionConstants.EXECUTE_REQUEST_MESSAGE_TEMPLATE,
                errorMessage);
        RuntimeException requestExecutionFailedException = new RuntimeException(errorMessage);
        // when
        ItfLiteException exception = assertThrows(
                ItfLiteException.class,
                () -> sseEmitterService.get().emitterCompleteWithError(expectedEmitter, requestExecutionFailedException)
        );
        // then
        verify(expectedEmitter).completeWithError(any());
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    public void processRequestExecutionTest_http_successfullyExecuted() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        SseEmitter sseEmitter = mock(SseEmitter.class);
        RequestExecutionResponse response = EntitiesGenerator.generateRequestExecutionResponse();
        HttpRequestEntitySaveRequest httpSavedRequest = EntitiesGenerator.generateRandomHttpRequestEntitySaveRequest();
        RequestExecution requestExecution = new RequestExecution(executor, sseId, httpSavedRequest, response, null);
        HistoryRequestBody requestBody = new HistoryRequestBody();
        requestBody.setContent(httpSavedRequest.getBody().getContent());
        requestBody.setType(httpSavedRequest.getBody().getType());
        HttpRequestExecutionDetails httpExecutionDetails = new HttpRequestExecutionDetails(requestExecution, httpSavedRequest, response, null, requestBody);
        response.setId(httpSavedRequest.getId());

        // when
        when(requestService.get().executeRequest(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        doReturn(sseEmitter, null).when(spySseEmitterService.get()).getEmitter(any());
        spySseEmitterService.get().processRequestExecution(httpSavedRequest, "", "", sseId, Optional.empty(), null, null);
        spySseEmitterService.get().processRequestExecution(httpSavedRequest, "", "", sseId, Optional.empty(), null, null);

        // then
        ArgumentCaptor<RequestExecutionResponse> requestExecutionResponseCaptor = ArgumentCaptor.forClass(RequestExecutionResponse.class);
        verify(spySseEmitterService.get(), times(1)).sendEventWithExecutionResult(eq(sseId), eq(sseEmitter), requestExecutionResponseCaptor.capture());
        response.setId(httpSavedRequest.getId());
        response.setResponseHeaders(Arrays.asList(new RequestExecutionHeaderResponse("key1", "[value1, value2]"), new RequestExecutionHeaderResponse("key2", "[value]")));
        assertEquals(response, requestExecutionResponseCaptor.getValue());

        ArgumentCaptor<ItfLiteExecutionFinishEvent> itfLiteExecutionFinishEventCaptor = ArgumentCaptor.forClass(ItfLiteExecutionFinishEvent.class);
        verify(kafkaExecutionFinishSendingService.get()).executionFinishEventSend(itfLiteExecutionFinishEventCaptor.capture());
        assertEquals(sseId, itfLiteExecutionFinishEventCaptor.getValue().getSseId());
        assertEquals(httpSavedRequest.getId(), itfLiteExecutionFinishEventCaptor.getValue().getRequestId());
        assertEquals(httpSavedRequest.getTransportType(), itfLiteExecutionFinishEventCaptor.getValue().getTransportType());
    }

    @Test
    public void processRequestExecutionTest_sendHttp_tryItfLiteHttpRequestExecuteException() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        SseEmitter sseEmitter = mock(SseEmitter.class);
        HttpRequestEntitySaveRequest httpSavedRequest = EntitiesGenerator.generateRandomHttpRequestEntitySaveRequest();

        // when
        when(requestService.get().executeRequest(any(), anyString(), anyString(), any(), any(),
                any(), any(), any()))
                .thenThrow(new ItfLiteHttpRequestExecuteException());

        doReturn(sseEmitter, null).when(spySseEmitterService.get()).getEmitter(any());
        when(spySseEmitterService.get().getEmitter(any())).thenReturn(sseEmitter);
        Exception actualException = assertThrows(ItfLiteHttpRequestExecuteException.class, () -> {
            spySseEmitterService.get().processRequestExecution(httpSavedRequest, "", "", sseId, Optional.empty(), null, null);
            spySseEmitterService.get().processRequestExecution(httpSavedRequest, "", "", sseId, Optional.empty(), null, null);
        });

        // then
        ArgumentCaptor<RequestExecutionResponse> requestExecutionResponseCaptor = ArgumentCaptor.forClass(RequestExecutionResponse.class);
        verify(spySseEmitterService.get(), times(1))
                .sendEventWithExecutionResult(eq(sseId), eq(sseEmitter), requestExecutionResponseCaptor.capture());
        assertEquals(ItfLiteHttpRequestExecuteException.DEFAULT_MESSAGE, actualException.getMessage(),
                "Not Correctly itf lite exception or try tehnical exception");

        RequestExecutionResponse actualRequestExecutionResponse = requestExecutionResponseCaptor.getValue();
        assertEquals(false, actualRequestExecutionResponse.isTestsPassed(), "Wait error and tests not passed");
    }

    @Test
    public void convertListToMapInputStream_WhenMultipartNotNull_ShouldConvertToMap() throws IOException {
        MultipartFile multipartFile = new MockMultipartFile("test", "test.json",
                "application/json", new byte[11534336]);
        MultipartFile multipartFile1 = new MockMultipartFile("test1", "test.pdf",
                "application/pdf", new byte[11534336]);
        List<MultipartFile> files = new ArrayList<>();
        files.add(multipartFile);
        files.add(multipartFile1);
        List<FileData> expRes = new ArrayList<>();
        expRes.add(new FileData(multipartFile.getBytes(), multipartFile.getOriginalFilename()));
        expRes.add(new FileData(multipartFile1.getBytes(), multipartFile1.getOriginalFilename()));
        List<FileData> actualRes = sseEmitterService.get().convertListMultipartToFileData(files);
        assertEquals(expRes, actualRes);
    }
}
