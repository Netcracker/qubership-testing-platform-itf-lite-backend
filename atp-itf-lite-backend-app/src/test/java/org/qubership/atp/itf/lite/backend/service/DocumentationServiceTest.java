package org.qubership.atp.itf.lite.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.configuration.MapperConfiguration;
import org.qubership.atp.itf.lite.backend.enums.EntityType;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderTreeSearchRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.GroupResponse;
import org.qubership.atp.itf.lite.backend.model.documentation.FolderDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.RequestDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.RequestEntityEditDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.TreeFolderDocumentationResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;

@ExtendWith(MockitoExtension.class)
class DocumentationServiceTest {

    private final ThreadLocal<DocumentationService> documentationService = new ThreadLocal<>();
    private final ThreadLocal<FolderService> folderService = new ThreadLocal<>();
    private final ThreadLocal<RequestService> requestService = new ThreadLocal<>();
    private final ThreadLocal<WritePermissionsService> writePermissionsService = new ThreadLocal<>();
    private static final ModelMapper modelMapper = new MapperConfiguration().modelMapper();


    @BeforeEach
    public void setUp() {
        FolderService folderServiceMock = Mockito.mock(FolderService.class);
        RequestService requestServiceMock = Mockito.mock(RequestService.class);
        WritePermissionsService writePermissionsServiceMock = Mockito.mock(WritePermissionsService.class);
        folderService.set(folderServiceMock);
        requestService.set(requestServiceMock);
        writePermissionsService.set(writePermissionsServiceMock);
        documentationService.set(new DocumentationService(folderServiceMock, requestServiceMock, modelMapper,
                writePermissionsServiceMock));
    }

    @Test
    public void getDocumentation_whenGetZeroPageWithPageSizeTwoAndHaveRepoThreeObject_successResult() {
        //given
        final UUID projectId = UUID.randomUUID();
        final UUID parentId = UUID.randomUUID();
        Folder rootFolder = EntitiesGenerator.generateFolder("folder_1", projectId, null);
        Request rootRequest = EntitiesGenerator.generateHttpRequest(UUID.randomUUID(), "request_1", projectId,
                UUID.randomUUID(), TransportType.REST, 100,  "IsRequest");
        rootRequest.setHasWritePermissions(true);
        FolderTreeSearchRequest folderTreeSearchRequest = new FolderTreeSearchRequest(projectId, parentId, "");

        GroupResponse folder = new GroupResponse(rootFolder, null);
        GroupResponse request = new GroupResponse(rootRequest, null);
        folder.setChildren(Arrays.asList(request, request));


        //when
        when(folderService.get().getRequestTreeByParentFolderId(any())).thenReturn(folder);
        when(folderService.get().getFolder(any())).thenReturn(rootFolder);
        when(requestService.get().getRequest(any())).thenReturn(rootRequest);
        when(writePermissionsService.get().hasWritePermissions(any(), any())).thenReturn(true);

        TreeFolderDocumentationResponse response = documentationService.get().getDescription(folderTreeSearchRequest, 0, 2);

        //then
        FolderDocumentation actualFolder = (FolderDocumentation) response.getAbstractDocumentationList().get(0);
        RequestDocumentation actualRequest = (RequestDocumentation) response.getAbstractDocumentationList().get(1);
        assertEquals(response.getTotalNumber(), 3);
        assertEquals(actualFolder.getName(), "folder_1");
        assertEquals(actualFolder.getDescription(), "");
        assertEquals(actualRequest.getDescription(), "IsRequest");
        assertTrue(actualFolder.isHasWritePermissions());
        assertTrue(actualRequest.isHasWritePermissions());
    }

    @Test
    public void editDocumentationRequest_haveCorrectlyRequest_documentationIsEdit() {
        //given
        final UUID projectId = UUID.randomUUID();
        final UUID requestId = UUID.randomUUID();
        Request rootRequest = EntitiesGenerator.generateHttpRequest(UUID.randomUUID(), "request_1", projectId,
                UUID.randomUUID(), TransportType.REST, 100,  "IsRequest");
        rootRequest.setHasWritePermissions(true);
        RequestEntityEditDocumentation documentationRequest =
                new RequestEntityEditDocumentation(projectId, EntityType.REQUEST, "docRequest");

        //when
        ArgumentCaptor<Request> captureRequest = ArgumentCaptor.forClass(Request.class);
        when(requestService.get().getRequest(any())).thenReturn(rootRequest);
        when(requestService.get().save(captureRequest.capture())).thenReturn(rootRequest);

        documentationService.get().editDocumentationRequest(requestId, documentationRequest);

        //then
        Request savedRequest = captureRequest.getValue();
        assertEquals(savedRequest.getDescription(), "docRequest");
    }

    @Test
    public void editDocumentationFolder_haveCorrectlyRequest_documentationIsEdit() {
        //given
        final UUID projectId = UUID.randomUUID();
        final UUID requestId = UUID.randomUUID();
        Folder rootFolder = EntitiesGenerator.generateFolder("folder_1", projectId, null);
        RequestEntityEditDocumentation documentationRequest =
                new RequestEntityEditDocumentation(projectId, EntityType.FOLDER, "docFolder");

        //when
        ArgumentCaptor<Folder> captureFolder = ArgumentCaptor.forClass(Folder.class);
        when(folderService.get().getFolder(any())).thenReturn(rootFolder);
        when(folderService.get().save(captureFolder.capture())).thenReturn(rootFolder);

        documentationService.get().editDocumentationFolder(requestId, documentationRequest);

        //then
        Folder savedRequest = captureFolder.getValue();
        assertEquals(savedRequest.getDescription(), "docFolder");
    }
}
