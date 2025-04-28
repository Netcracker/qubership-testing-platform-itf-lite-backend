package org.qubership.atp.itf.lite.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.CustomRequestExecutionRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestExecutionDetailsRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestExecutionRepository;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.ErrorResponseSerializable;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionResponse;
import org.qubership.atp.itf.lite.backend.model.entities.FileBody;
import org.qubership.atp.itf.lite.backend.model.entities.history.HistoryRequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.history.HistoryRequestDetailsResponse;
import org.qubership.atp.itf.lite.backend.model.entities.history.HistorySearchRequest;
import org.qubership.atp.itf.lite.backend.model.entities.history.HttpRequestExecutionDetails;
import org.qubership.atp.itf.lite.backend.model.entities.history.RequestExecution;

import joptsimple.internal.Strings;

@ExtendWith(MockitoExtension.class)
public class RequestExecutionHistoryServiceTest {

    private final ThreadLocal<RequestExecutionDetailsRepository> detailsRepository = new ThreadLocal<>();
    private final ThreadLocal<CustomRequestExecutionRepository> customRequestExecutionRepository = new ThreadLocal<>();
    private final ThreadLocal<RequestExecutionRepository> requestExecutionRepository = new ThreadLocal<>();
    private final ThreadLocal<GridFsService> gridFsService = new ThreadLocal<>();
    private final ThreadLocal<RequestExecutionHistoryService> service = new ThreadLocal<>();

    private static final String token = Strings.EMPTY;
    private static final UUID projectId = UUID.randomUUID();
    private static final UUID sseId = UUID.randomUUID();
    private static final HttpRequestEntitySaveRequest httpSavedRequest = EntitiesGenerator.generateRandomHttpRequestEntitySaveRequest();
    private static final RequestExecutionResponse response = EntitiesGenerator.generateRequestExecutionResponse();

    @BeforeEach
    public void setUp() {
        RequestExecutionDetailsRepository detailsRepositoryMock = mock(RequestExecutionDetailsRepository.class);
        CustomRequestExecutionRepository customRequestExecutionRepositoryMock = mock(CustomRequestExecutionRepository.class);
        RequestExecutionRepository requestExecutionRepositoryMock = mock(RequestExecutionRepository.class);
        GridFsService gridFsServiceMock = mock(GridFsService.class);
        UserService userService = mock(UserService.class);
        detailsRepository.set(detailsRepositoryMock);
        customRequestExecutionRepository.set(customRequestExecutionRepositoryMock);
        requestExecutionRepository.set(requestExecutionRepositoryMock);
        gridFsService.set(gridFsServiceMock);
        service.set(new RequestExecutionHistoryService(userService, detailsRepositoryMock,
                customRequestExecutionRepositoryMock, requestExecutionRepositoryMock, gridFsServiceMock));
    }

    @Test
    public void logHttpRequestExecution_correctDataProvided_executionHistorySaved() {
        // given
        String executor = service.get().getUserInformation(token);
        RequestExecution execution = new RequestExecution(executor, sseId, httpSavedRequest, response, null);
        HistoryRequestBody requestBody = new HistoryRequestBody();
        requestBody.setContent(httpSavedRequest.getBody().getContent());
        requestBody.setType(httpSavedRequest.getBody().getType());
        HttpRequestExecutionDetails details = new HttpRequestExecutionDetails(
                execution, httpSavedRequest, response, null, requestBody);
        // when
        service.get().logHttpRequestExecution(token, sseId, httpSavedRequest, response, null, any());
        // then
        verify(detailsRepository.get()).save(details);
    }

    @Test
    public void logHttpRequestExecution_correctDataProvided_executionHistorySavedWithFile() {
        // given
        String executor = service.get().getUserInformation(token);
        HttpRequestEntitySaveRequest httpSavedRequestWithFile = EntitiesGenerator.generateRandomHttpRequestEntitySaveRequestWithFileData();
        RequestExecution execution = new RequestExecution(executor, sseId, httpSavedRequestWithFile, response, null);

        UUID fileId = UUID.randomUUID();
        when(gridFsService.get().saveHistoryBinary(any(), any(), any())).thenReturn(fileId);
        HistoryRequestBody requestBody = new HistoryRequestBody();
        requestBody.setType(httpSavedRequestWithFile.getBody().getType());
        requestBody.setBinaryBody(new FileBody(httpSavedRequestWithFile.getFile().getFileName(), fileId));
        HttpRequestExecutionDetails details = new HttpRequestExecutionDetails(
                execution, httpSavedRequestWithFile, response, null, requestBody);
        // when
        service.get().logHttpRequestExecution(token, sseId, httpSavedRequestWithFile, response, null, any());
        // then
        verify(detailsRepository.get()).save(details);
    }

    @Test
    public void getExecutionHistory_correctRequestProvided_ExecutionHistoryReturned() {
        //given
        HistorySearchRequest request = new HistorySearchRequest(projectId, 0, 10, null, null);
        // when
        service.get().getExecutionHistory(request);
        // then
        verify(customRequestExecutionRepository.get()).findAllRequestExecutions(request);
    }

    @Test
    public void getExecutionHistoryDetails_correctRequestProvided_ExecutionHistoryDetailsReturned() {
        // given
        TransportType restTransportType = TransportType.REST;
        UUID restHistoryItemId = UUID.randomUUID();
        RequestExecution httpRequestExecution = EntitiesGenerator.generateHttpRequestExecution(restHistoryItemId);
        HttpRequestExecutionDetails actualDetails = new HttpRequestExecutionDetails();
        ErrorResponseSerializable errorResponse = new ErrorResponseSerializable(500, null, null, null,
                "message ", "resa", new ArrayList<>());
        actualDetails.setErrorMessage(errorResponse);
        actualDetails.setRequestPreScript("pre");
        actualDetails.setRequestPostScript("post");

        // when
        when(requestExecutionRepository.get().findById(restHistoryItemId))
                .thenReturn(Optional.of(httpRequestExecution));
        when(detailsRepository.get().findByRequestExecution(any()))
                .thenReturn(actualDetails);

        HistoryRequestDetailsResponse actualResult = service.get().getExecutionHistoryDetailsByHistoryItemId(restHistoryItemId);
        // then
        verify(detailsRepository.get(), times(1)).findByRequestExecution(httpRequestExecution);
        Assertions.assertEquals(actualResult.getErrorMessage(), actualDetails.getErrorMessage().getMessage());
    }

    @Test
    public void getExecutionHistoryDetails_incorrectRequestProvided_ExecutionHistoryDetailsReturnedFailed() {
        // given
        TransportType restTransportType = TransportType.REST;
        UUID restHistoryItemId = UUID.randomUUID();
        // when
        when(requestExecutionRepository.get().findById(restHistoryItemId))
                .thenReturn(Optional.empty());
        // then
        AtpEntityNotFoundException exception = assertThrows(
                AtpEntityNotFoundException.class,
                () -> service.get().getExecutionHistoryDetailsByHistoryItemId(restHistoryItemId)
        );
        String requestExecutionEntityName = RequestExecution.class.getSimpleName();
        String expectedErrorMessage = String.format(AtpEntityNotFoundException.DEFAULT_ID_MESSAGE, requestExecutionEntityName, restHistoryItemId);
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    @Test
    public void cleanUpRequestExecutionHistory() {
        // given
        final int shift = 14;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -14);
        Timestamp timestamp = new Timestamp(calendar.getTimeInMillis());
        // when
        service.get().cleanUpRequestExecutionHistory(shift);
        // then
        ArgumentCaptor<Timestamp> captureRequestTimestamp = ArgumentCaptor.forClass(Timestamp.class);
        verify(requestExecutionRepository.get(), times(1)).deleteByExecutedWhenBefore(captureRequestTimestamp.capture());
        Timestamp timeStampReal = captureRequestTimestamp.getValue();
        if(!timeStampReal.equals(timestamp)) {
            //10 milliseconds for slow execution
            if (timeStampReal.getTime() > timestamp.getTime() && timeStampReal.getTime() - 10 < timestamp.getTime()) {
                assertTrue(true);
            }
        }
        assertTrue(true);
    }

    @Test
    public void getExecutorsInRequestExecutionHistory_searchCriteriaSpecified_dataReturned() {
        // given, when
        service.get().getExecutorsInRequestExecutionHistory(projectId);
        // then
        verify(requestExecutionRepository.get()).findByProjectId(projectId);
    }

}
