package org.qubership.atp.itf.lite.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.GridFsRepository;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;

@ExtendWith(MockitoExtension.class)
public class GridFsServiceTest {

    private final ThreadLocal<GridFsRepository> repository = new ThreadLocal<>();
    private final ThreadLocal<GridFsService> gridFsService = new ThreadLocal<>();


    @BeforeEach
    public void setUp() {
        GridFsRepository repositoryMock = mock(GridFsRepository.class);
        repository.set(repositoryMock);
        gridFsService.set(new GridFsService(repositoryMock));
    }

    @Test
    public void downloadDictionaryTest_whenRequestIdIsSpecified_shouldReturnFileData() {
        // given
        final UUID requestId = UUID.randomUUID();
        FileData dictionaryFileData = new FileData("test".getBytes(), "test.txt");
        // when
        when(repository.get().getRequestFileData(any())).thenReturn(Optional.of(dictionaryFileData));
        Optional<FileData> actualFileData = gridFsService.get().downloadFile(requestId);
        // then
        assertTrue(actualFileData.isPresent());
        assertEquals(dictionaryFileData, actualFileData.get());
    }

    @Test
    public void removeDictionaryByRequestIdTest_whenRequestIdIsSpecified_shouldReturnNothing() {
        // given
        final UUID requestId = UUID.randomUUID();
        // when
        doNothing().when(repository.get()).removeFileByRequestId(any());
        gridFsService.get().removeFileByRequestId(requestId);
        // then
        verify(repository.get()).removeFileByRequestId(any());
    }

    @Test
    public void removeDictionaryByDateTest_whenNumberOfDaysIsSpecified_shouldReturnNothing() {
        // given
        final UUID requestId = UUID.randomUUID();
        // when
        doNothing().when(repository.get()).removeFilesByDate(any());
        gridFsService.get().removeFilesByDate(1);
        // then
        verify(repository.get()).removeFilesByDate(any());
    }

    @Test
    public void saveDictionaryTest_shouldReturnNothing() throws IOException {
        // given
        final UUID requestId = UUID.randomUUID();
        InputStream dictionary = IOUtils.toInputStream("test", "UTF-8");
        // when
        gridFsService.get().saveDictionaryByRequestId(LocalDateTime.now().toString(), requestId, dictionary, "test.zip");
        // then
        verify(repository.get()).saveDictionaryByRequestId(any(), any(), any(), any());
    }

    @Test
    public void saveDictionaryBySessionIdAndSessionRequestIdTest_shouldReturnNothing() throws IOException {
        // given
        final UUID requestId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        InputStream dictionary = IOUtils.toInputStream("test", "UTF-8");
        // when
        gridFsService.get().saveDictionaryBySessionIdAndSessionRequestId(LocalDateTime.now().toString(),
                requestId, sessionId, dictionary, "test.zip");
        // then
        verify(repository.get()).saveDictionaryBySessionIdAndSessionRequestId(any(), any(), any(), any(),
                any());
    }

    @Test
    public void saveBinaryBySessionIdTest_shouldReturnNothing() throws IOException {
        // given
        final UUID requestId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        InputStream dictionary = IOUtils.toInputStream("test", "UTF-8");
        // when
        gridFsService.get().saveBinaryBySessionId(LocalDateTime.now().toString(),
                sessionId, requestId, dictionary, "test.zip", "zip");
        // then
        verify(repository.get()).saveBinaryBySessionIdAndSessionRequestId(any(), any(), any(), any(),
                any(), any());
    }
    @Test
    public void saveFileBySessionIdTest_shouldReturnNothing() throws IOException {
        // given
        final UUID fileId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        InputStream dictionary = IOUtils.toInputStream("test", "UTF-8");
        // when
        gridFsService.get().saveFileBySessionId(LocalDateTime.now().toString(),
                sessionId, dictionary, "test.zip", fileId);
        // then
        verify(repository.get()).saveFileBySessionId(any(), any(), any(), any(),
                any());
    }

    @Test
    public void removeFileBySessionIdTest_shouldReturnNothing() {
        // given
        final UUID sessionId = UUID.randomUUID();
        // when
        gridFsService.get().removeFileBySessionId(sessionId);
        // then
        verify(repository.get()).removeFileBySessionId(any());
    }
}
