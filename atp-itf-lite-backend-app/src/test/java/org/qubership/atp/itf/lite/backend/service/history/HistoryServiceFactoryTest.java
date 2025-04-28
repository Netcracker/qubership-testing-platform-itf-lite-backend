package org.qubership.atp.itf.lite.backend.service.history;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestRepository;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.service.history.iface.RestoreHistoryService;
import org.qubership.atp.itf.lite.backend.service.history.iface.RetrieveHistoryService;
import org.qubership.atp.itf.lite.backend.service.history.impl.FolderRestoreHistoryService;
import org.qubership.atp.itf.lite.backend.service.history.impl.FolderRetrieveHistoryService;
import org.qubership.atp.itf.lite.backend.service.history.impl.HistoryServiceFactory;
import org.qubership.atp.itf.lite.backend.service.history.impl.HttpRequestRestoreHistoryService;
import org.qubership.atp.itf.lite.backend.service.history.impl.HttpRequestRetrieveHistoryService;

public class HistoryServiceFactoryTest {
    private static ThreadLocal<HistoryServiceFactory> historyServiceFactory = new ThreadLocal<>();
    private static ThreadLocal<RequestRepository> requestRepository = new ThreadLocal<>();

    private static HttpRequestRestoreHistoryService httpRequestRestoreHistoryService;
    private static HttpRequestRetrieveHistoryService httpRequestRetrieveHistoryService;
    private static FolderRestoreHistoryService folderRestoreHistoryService;
    private static FolderRetrieveHistoryService folderRetrieveHistoryService;

    @BeforeAll
    static public void setUp() {
        httpRequestRestoreHistoryService = mock(HttpRequestRestoreHistoryService.class);
        when(httpRequestRestoreHistoryService.getItemType()).thenCallRealMethod();
        when(httpRequestRestoreHistoryService.getEntityClass()).thenCallRealMethod();

        httpRequestRetrieveHistoryService = mock(HttpRequestRetrieveHistoryService.class);
        when(httpRequestRetrieveHistoryService.getItemType()).thenCallRealMethod();
        when(httpRequestRetrieveHistoryService.getEntityClass()).thenCallRealMethod();

        folderRestoreHistoryService = mock(FolderRestoreHistoryService.class);
        when(folderRestoreHistoryService.getItemType()).thenCallRealMethod();
        when(folderRestoreHistoryService.getEntityClass()).thenCallRealMethod();

        folderRetrieveHistoryService = mock(FolderRetrieveHistoryService.class);
        when(folderRetrieveHistoryService.getItemType()).thenCallRealMethod();
        when(folderRetrieveHistoryService.getEntityClass()).thenCallRealMethod();
    }

    @BeforeEach
    public void beforeEach() {
        RequestRepository requestRepositoryMock = mock(RequestRepository.class);
        requestRepository.set(requestRepositoryMock);
        historyServiceFactory.set(new HistoryServiceFactory(
                Arrays.asList(httpRequestRestoreHistoryService, folderRestoreHistoryService),
                Arrays.asList(httpRequestRetrieveHistoryService, folderRetrieveHistoryService),
                requestRepositoryMock));
    }

    @Test
    public void getRestoreHistoryServiceTest_httpRequest_returnHttpRequestRestoreHistoryService() {
        when(requestRepository.get().findTransportType(any())).thenReturn(TransportType.REST);

        Optional<RestoreHistoryService> restoreHistoryService =
                historyServiceFactory.get().getRestoreHistoryService("request", UUID.randomUUID());

        Assertions.assertEquals(HttpRequestRestoreHistoryService.class, restoreHistoryService.get().getClass());
    }

    @Test
    public void getRestoreHistoryServiceTest_folder_returnFolderRestoreHistoryService() {
        Optional<RestoreHistoryService> restoreHistoryService =
                historyServiceFactory.get().getRestoreHistoryService("folder", UUID.randomUUID());

        Assertions.assertEquals(FolderRestoreHistoryService.class, restoreHistoryService.get().getClass());
    }

    @Test
    public void getRetrieveHistoryServiceTest_httpRequest_returnHttpRequestRetrieveHistoryService() {
        when(requestRepository.get().findTransportType(any())).thenReturn(TransportType.SOAP);

        Optional<RetrieveHistoryService> retrieveHistoryService =
                historyServiceFactory.get().getRetrieveHistoryService("request", UUID.randomUUID());

        Assertions.assertEquals(HttpRequestRetrieveHistoryService.class, retrieveHistoryService.get().getClass());
    }

    @Test
    public void getRetrieveHistoryServiceTest_folder_returnFolderRetrieveHistoryService() {
        Optional<RetrieveHistoryService> retrieveHistoryService =
                historyServiceFactory.get().getRetrieveHistoryService("folder", UUID.randomUUID());

        Assertions.assertEquals(FolderRetrieveHistoryService.class, retrieveHistoryService.get().getClass());
    }
}