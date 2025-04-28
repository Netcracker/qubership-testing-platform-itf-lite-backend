package org.qubership.atp.itf.lite.backend.service.history;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.javers.core.Javers;
import org.javers.shadow.Shadow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.exceptions.history.ItfLiteRevisionHistoryNotFoundException;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;
import org.qubership.atp.itf.lite.backend.service.FolderService;
import org.qubership.atp.itf.lite.backend.service.RequestService;
import org.qubership.atp.itf.lite.backend.service.history.impl.FolderRestoreHistoryService;
import org.qubership.atp.itf.lite.backend.service.history.impl.HttpRequestRestoreHistoryService;
import org.qubership.atp.itf.lite.backend.service.history.impl.ValidateReferenceExistsService;

public class RestoreHistoryServiceTest {

    private final ThreadLocal<Javers> javers = new ThreadLocal<>();
    private final ThreadLocal<RequestService> requestService = new ThreadLocal<>();
    private final ThreadLocal<ValidateReferenceExistsService> validateReferenceExistsService = new ThreadLocal<>();
    private final ThreadLocal<FolderService> folderService = new ThreadLocal<>();

    private final ThreadLocal<HttpRequestRestoreHistoryService> httpRequestRestoreHistoryService = new ThreadLocal<>();
    private final ThreadLocal<FolderRestoreHistoryService> folderRestoreHistoryService = new ThreadLocal<>();

    private final ThreadLocal<MockedStatic<Hibernate>> hibernate = new ThreadLocal<>();

    @BeforeEach
    public void before() {
        ModelMapper modelMapper = new ModelMapper();
        Javers javersMock = Mockito.mock(Javers.class);
        RequestService requestServiceMock = Mockito.mock(RequestService.class);
        FolderService folderServiceMock = Mockito.mock(FolderService.class);
        ValidateReferenceExistsService validateReferenceExistsServiceMock = Mockito.mock(ValidateReferenceExistsService.class);
        javers.set(javersMock);
        requestService.set(requestServiceMock);
        folderService.set(folderServiceMock);
        validateReferenceExistsService.set(validateReferenceExistsServiceMock);
        httpRequestRestoreHistoryService.set(new HttpRequestRestoreHistoryService(javersMock, requestServiceMock,
                validateReferenceExistsServiceMock, modelMapper));
        folderRestoreHistoryService.set(new FolderRestoreHistoryService(javersMock, folderServiceMock,
                validateReferenceExistsServiceMock, modelMapper));
        hibernate.set(mockStatic(Hibernate.class));
    }

    @Test
    public void restoreToRevisionTest_withHttpRequest_restoreObjectToShadow() {
        HttpRequest actualRequest = EntitiesGenerator.generateRandomHttpRequest();
        HttpRequest shadowRequest = EntitiesGenerator.generateRandomHttpRequest();
        shadowRequest.setName("name1");
        shadowRequest.getRequestHeaders().set(0, new RequestHeader("new header", "value", "", false));
        shadowRequest.getRequestParams().set(0, new RequestParam("new param", "value", "", false));
        shadowRequest.getBody().setContent("{\"newKey\":\"newValue\"}");
        Shadow shadow = mock(Shadow.class);

        when(javers.get().findShadows(any())).thenReturn(Collections.singletonList(shadow));
        hibernate.get().when(() -> Hibernate.unproxy(any(), any())).thenReturn(actualRequest);
        when(shadow.get()).thenReturn(shadowRequest);

        httpRequestRestoreHistoryService.get().restoreToRevision(UUID.randomUUID(), 1L);

        ArgumentCaptor<HttpRequest> captureHttpRequest = ArgumentCaptor.forClass(HttpRequest.class);
        verify(requestService.get(), times(1)).restore(captureHttpRequest.capture());
        HttpRequest result = captureHttpRequest.getValue();
        Assertions.assertEquals(shadowRequest.getName(), result.getName());
        Assertions.assertEquals(shadowRequest.getBody().getContent(), result.getBody().getContent());
        Assertions.assertEquals(shadowRequest.getRequestHeaders(), result.getRequestHeaders());
        Assertions.assertEquals(shadowRequest.getRequestParams(), result.getRequestParams());
    }

    @Test
    public void restoreToRevisionTest_notFoundShadows_returnException() {
        HttpRequest actualRequest = new HttpRequest();
        actualRequest.setName("name2");
        HttpRequest shadowRequest = new HttpRequest();
        shadowRequest.setName("name1");
        when(javers.get().findShadows(any())).thenReturn(Collections.emptyList());

        assertThrows(ItfLiteRevisionHistoryNotFoundException.class,
                () -> httpRequestRestoreHistoryService.get().restoreToRevision(UUID.randomUUID(), 1L));
    }

    @Test
    public void restoreToRevisionTest_withFolder_restoreObjectToShadow() {
        Folder actualFolder = EntitiesGenerator.generateFolder("name2", UUID.randomUUID());
        Folder shadowFolder = EntitiesGenerator.generateFolder("name1", UUID.randomUUID());
        Shadow shadow = mock(Shadow.class);

        when(javers.get().findShadows(any())).thenReturn(Collections.singletonList(shadow));
        hibernate.get().when(() -> Hibernate.unproxy(any(), any())).thenReturn(actualFolder);
        when(shadow.get()).thenReturn(shadowFolder);

        folderRestoreHistoryService.get().restoreToRevision(UUID.randomUUID(), 1L);

        ArgumentCaptor<Folder> captureFolder = ArgumentCaptor.forClass(Folder.class);
        verify(folderService.get(), times(1)).restore(captureFolder.capture());
        Folder result = captureFolder.getValue();
        Assertions.assertEquals(shadowFolder.getName(), result.getName());
    }

    @AfterEach
    public void close() {
        hibernate.get().close();
    }
}
