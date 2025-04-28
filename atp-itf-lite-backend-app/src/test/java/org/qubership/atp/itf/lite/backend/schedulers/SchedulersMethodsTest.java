package org.qubership.atp.itf.lite.backend.schedulers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.qubership.atp.itf.lite.backend.utils.Constants.DEFAULT_DICTIONARIES_FOLDER;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.GetAuthorizationCodeRepository;
import org.qubership.atp.itf.lite.backend.service.CollectionRunService;
import org.qubership.atp.itf.lite.backend.service.GridFsService;
import org.qubership.atp.itf.lite.backend.service.RequestExecutionHistoryService;

@ExtendWith(MockitoExtension.class)
public class SchedulersMethodsTest {

    private final ThreadLocal<LockManager> lockManager = new ThreadLocal<>();
    private final ThreadLocal<GridFsService> gridFsService = new ThreadLocal<>();
    private final ThreadLocal<RequestExecutionHistoryService> historyService = new ThreadLocal<>();
    private final ThreadLocal<DictionariesCleaner> dictionariesCleaner = new ThreadLocal<>();
    private final ThreadLocal<RequestExecutionHistoryCleanup> historyCleanup = new ThreadLocal<>();
    private final ThreadLocal<GetAuthorizationCodeRepository> getAuthorizationCodeRepository = new ThreadLocal<>();
    private final ThreadLocal<GetAccessTokenCleanup> getAccessTokenCleanup = new ThreadLocal<>();
    private final ThreadLocal<CollectionRunService> collectionRunService = new ThreadLocal<>();
    private final ThreadLocal<CollectionRunsCleaner> collectionRunsCleaner = new ThreadLocal<>();

    @BeforeEach
    public void setUp() {
        LockManager lockManagerMock = mock(LockManager.class);
        GridFsService gridFsServiceMock = mock(GridFsService.class);
        RequestExecutionHistoryService historyServiceMock = mock(RequestExecutionHistoryService.class);
        GetAuthorizationCodeRepository getAuthorizationCodeRepositoryMock = mock(GetAuthorizationCodeRepository.class);
        CollectionRunService collectionRunServiceMock = mock(CollectionRunService.class);
        lockManager.set(lockManagerMock);
        gridFsService.set(gridFsServiceMock);
        historyService.set(historyServiceMock);
        collectionRunService.set(collectionRunServiceMock);
        getAuthorizationCodeRepository.set(getAuthorizationCodeRepositoryMock);
        dictionariesCleaner.set(new DictionariesCleaner(lockManagerMock, gridFsServiceMock));
        historyCleanup.set(new RequestExecutionHistoryCleanup(lockManagerMock, historyServiceMock));
        getAccessTokenCleanup.set(new GetAccessTokenCleanup(lockManagerMock, getAuthorizationCodeRepositoryMock));
        collectionRunsCleaner.set(new CollectionRunsCleaner(lockManagerMock, collectionRunServiceMock));
    }

    @Test
    public void dictionariesCleanup_whenDirectoryWithFilesExists_shouldDeleteWholeDirectory()
            throws IOException {
        // given
        Files.createDirectories(DEFAULT_DICTIONARIES_FOLDER);
        // when
        dictionariesCleaner.get().dictionariesCleanup();
        // then
        assertFalse(Files.exists(DEFAULT_DICTIONARIES_FOLDER));
        // cleanup
        Files.deleteIfExists(DEFAULT_DICTIONARIES_FOLDER);
    }

    @Test
    public void gridFsDictionariesCleanup_whenFileInGridFsExists_shouldDeleteFile() {
        // given, when
        dictionariesCleaner.get().gridFsFilesCleanup();
        // then
        verify(gridFsService.get(), times(1)).removeFilesByDate(any());
    }

    @Test
    public void dictionariesCleanupWithLockManagerTest_shouldCallDictionariesCleanupMethod() {
        // given, when
        doNothing().when(lockManager.get()).executeWithLock(any(), any());
        dictionariesCleaner.get().dictionariesCleanupWithLockManager();
        // then
        verify(lockManager.get(), times(1)).executeWithLock(any(), any());
    }

    @Test
    public void gridFsDictionariesCleanupWithLockManagerTest_shouldCallgridFsDictionariesCleanupMethod() {
        // given, when
        doNothing().when(lockManager.get()).executeWithLock(any(), any());
        dictionariesCleaner.get().gridFsDictionariesCleanupWithLockManager();
        // then
        verify(lockManager.get(), times(1)).executeWithLock(any(), any());
    }

    @Test
    public void requestExecutionHistoryCleanupWithLockManagerTest_shouldCallRequestExecutionHistoryCleanupMethod() {
        // given, when
        doNothing().when(lockManager.get()).executeWithLock(any(), any());
        historyCleanup.get().cleanUpRequestExecutionHistoryWithLockManager();
        // then
        verify(lockManager.get(), times(1)).executeWithLock(any(), any());
    }

    @Test
    public void cleanUpRequestExecutionHistory_whenHistoryRecordsExists_shouldCleanUpRecords() {
        // given, when
        historyCleanup.get().cleanUpRequestExecutionHistory();
        // then
        verify(historyService.get(), times(1)).cleanUpRequestExecutionHistory(anyInt());
    }

    @Test
    public void getAccessTokenCleanup_whenHistoryRecordsExists_shouldCleanUpRecords() {
        // mock
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        // action
        getAccessTokenCleanup.get().getAccessTokenCleanup();
        // check
        verify(lockManager.get()).executeWithLock(any(), runnable.capture());
        runnable.getValue().run();
        verify(getAuthorizationCodeRepository.get(), times(1)).deleteByStartedAtBefore(any());
    }

    @Test
    public void collectionRunsCleanupWithLockManagerTest_shouldCallCollectionRunsCleanUpMethod() {
        // given, when
        doNothing().when(lockManager.get()).executeWithLock(any(), any());
        collectionRunsCleaner.get().collectionRunsCleanupWithLockManager();
        // then
        verify(lockManager.get(), times(1)).executeWithLock(any(), any());
    }

    @Test
    public void cleanUpCollectionRunsTest_shouldCallCollectionRunServiceCleanUpMethod() {
        // given, when
        collectionRunsCleaner.get().cleanUpCollectionRuns();
        // then
        verify(collectionRunService.get(), times(1)).cleanUpRequestExecutionHistory(anyInt());
    }
}
