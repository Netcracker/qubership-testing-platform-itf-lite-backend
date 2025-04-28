package org.qubership.atp.itf.lite.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestSnapshotRepository;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.model.api.request.BulkDeleteSnapshotsRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.BearerAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestSnapshotResponse;
import org.qubership.atp.itf.lite.backend.model.entities.FileBody;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.RequestSnapshot;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileInfo;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.key.RequestSnapshotKey;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class RequestSnapshotServiceTest {

    @InjectMocks
    private RequestSnapshotService requestSnapshotService;

    @Mock
    private RequestService requestService;

    @Mock
    private GridFsService gridFsService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RequestAuthorizationService requestAuthorizationService;

    @Mock
    private RequestSnapshotRepository repository;



    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void test_saveSnapshot() throws JsonProcessingException {
        RequestSnapshot requestSnapshot = new RequestSnapshot();
        requestSnapshot.setRequestId(UUID.randomUUID());
        requestSnapshot.setSessionId(UUID.randomUUID());
        requestSnapshot.setRequest("{\"transportType\": \"REST\"\"authorization\": {}}");
        List<MultipartFile> files = new ArrayList<>();
        Optional<FileBody> fileInfo = Optional.empty();

        Request request = new HttpRequest();
        RequestAuthorization requestAuthorization = new RequestAuthorization();
        requestAuthorization.setType(RequestAuthorizationType.BEARER);
        request.setAuthorization(requestAuthorization);
        RequestEntitySaveRequest requestEntitySaveRequest = new HttpRequestEntitySaveRequest();
        BearerAuthorizationSaveRequest requestAuthorizationSaveRequest = new BearerAuthorizationSaveRequest();
        requestAuthorizationSaveRequest.setType(RequestAuthorizationType.BEARER);
        requestEntitySaveRequest.setAuthorization(requestAuthorizationSaveRequest);
        AuthorizationUtils.setModelMapper(new ModelMapper());
        when(objectMapper.readValue(anyString(), eq(Request.class))).thenReturn(request);
        when(objectMapper.readValue(anyString(), eq(RequestEntitySaveRequest.class))).thenReturn(requestEntitySaveRequest);
        when(repository.findById(any(RequestSnapshotKey.class))).thenReturn(Optional.empty());

        requestSnapshotService.saveSnapshot(requestSnapshot, files, fileInfo);

        verify(requestService).checkFilesSize(files);
        verify(requestAuthorizationService).encryptAuthorizationParameters(any());
        verify(repository).save(requestSnapshot);
    }

    @Test
    void test_SaveFileToFileSystemAndGridFs_successfully() throws IOException {
        UUID sessionId = UUID.randomUUID();
        UUID sessionRequestId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(mock(java.io.InputStream.class));
        when(file.getOriginalFilename()).thenReturn("testFile");

        FileBody fileBody = new FileBody("testFile", UUID.randomUUID());

        when(gridFsService.saveBinaryBySessionId(anyString(), eq(sessionId), eq(sessionRequestId), any(), any(), any()))
                .thenReturn(fileBody);

        Optional<FileBody> result = requestSnapshotService.saveFileToFileSystemAndGridFs(sessionId,
                sessionRequestId, file, TransportType.REST);

        assertTrue(result.isPresent());
        assertEquals(fileBody, result.get());
    }

    @Test
    void test_GetSnapshot_successfully() {
        UUID sessionId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        RequestSnapshot requestSnapshot = new RequestSnapshot();
        requestSnapshot.setBinaryFileId(UUID.randomUUID());
        requestSnapshot.setRequest("request");

        when(repository.findById(any(RequestSnapshotKey.class))).thenReturn(Optional.of(requestSnapshot));

        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName("testFile");
        fileInfo.setFileType("type");
        fileInfo.setContentType("content");
        fileInfo.setSize(123L);

        when(gridFsService.getFileInfoByFileId(any(UUID.class))).thenReturn(fileInfo);

        RequestSnapshotResponse result = requestSnapshotService.getSnapshot(sessionId, requestId);

        assertNotNull(result);
        assertEquals("request", result.getRequest());
        assertNotNull(result.getBinaryFile());
        assertEquals("testFile", result.getBinaryFile().getFileName());
    }

    @Test
    void test_DeleteSnapshotByRequestSnapshotKey_successfully() {
        UUID sessionId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        requestSnapshotService.deleteSnapshotByRequestSnapshotKey(sessionId, requestId);

        verify(repository).deleteBySessionIdAndRequestId(sessionId, requestId);
        verify(gridFsService).removeAllFilesBySessionIdAndSessionRequestId(sessionId, requestId);
    }

    @Test
    void test_BulkDeleteSnapshots_successfully() {
        BulkDeleteSnapshotsRequest bulkDeleteSnapshotsRequest = new BulkDeleteSnapshotsRequest();
        bulkDeleteSnapshotsRequest.setRequestIds(Arrays.asList(UUID.randomUUID(), UUID.randomUUID()));

        UUID sessionId = UUID.randomUUID();

        requestSnapshotService.bulkDeleteSnapshots(bulkDeleteSnapshotsRequest, sessionId);

        verify(repository).deleteAllBySessionIdAndRequestIdIn(any(), anyList());
        verify(gridFsService).bulkRemoveFilesBySnapshotKeys(any(), anyList());
    }

    @Test
    void test_PreSaveRequestProcessing_successfully() {
        UUID sessionId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        requestSnapshotService.preSaveRequestProcessing(sessionId, requestId);

        verify(gridFsService).moveFileFromSnapshotToRequest(sessionId, requestId);
        verify(repository).deleteBySessionIdAndRequestId(sessionId, requestId);
    }

    @Test
    void test_GetByCreatedWhenDifferenceGreaterThanReferenceDate_successfully() {
        Date referenceDate = new Date();
        Long expirationPeriod = 1000L;
        List<RequestSnapshotKey> keys = new ArrayList<>();

        when(repository.findAllByCreatedWhenDifferenceGreaterThanReferenceDate(referenceDate, expirationPeriod))
                .thenReturn(keys);

        List<RequestSnapshotKey> result = requestSnapshotService.getByCreatedWhenDifferenceGreaterThanReferenceDate(referenceDate, expirationPeriod);

        assertEquals(keys, result);
    }
}

