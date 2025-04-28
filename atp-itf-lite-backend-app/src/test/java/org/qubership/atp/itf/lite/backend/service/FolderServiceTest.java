package org.qubership.atp.itf.lite.backend.service;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.entities.Operation;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.auth.springbootstarter.security.permissions.PolicyEnforcement;
import org.qubership.atp.auth.springbootstarter.services.UsersService;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.itf.lite.backend.configuration.MapperConfiguration;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.FolderRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestRepository;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.exceptions.access.ItfLiteAccessDeniedException;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderCopyRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderDeleteRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderEditRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderMoveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderOrderChangeRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderUpsetRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.IdWithModifiedWhen;
import org.qubership.atp.itf.lite.backend.model.api.request.Permissions;
import org.qubership.atp.itf.lite.backend.model.api.request.Settings;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.InheritFromParentAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.ParentRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BearerRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.InheritFromParentRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.converters.ListConverter;
import org.qubership.atp.itf.lite.backend.model.entities.converters.PermissionEntityConverter;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.service.history.iface.DeleteHistoryService;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.qubership.atp.itf.lite.backend.utils.StreamUtils;
import org.qubership.atp.itf.lite.backend.utils.UserManagementEntities;

@ExtendWith(MockitoExtension.class)
public class FolderServiceTest {

    private final ThreadLocal<FolderRepository> folderRepository = new ThreadLocal<>();
    private final ThreadLocal<RequestRepository> requestRepository = new ThreadLocal<>();
    private final ThreadLocal<UsersService> usersService = new ThreadLocal<>();
    private final ThreadLocal<PolicyEnforcement> policyEnforcement = new ThreadLocal<>();
    private final ThreadLocal<Provider<UserInfo>> userInfoProvider = new ThreadLocal<>();
    private final ThreadLocal<RequestAuthorizationService> requestAuthorizationService = new ThreadLocal<>();
    private final ThreadLocal<FolderSpecificationService> folderSpecificationService = new ThreadLocal<>();
    private final ThreadLocal<DeleteHistoryService> deleteHistoryService = new ThreadLocal<>();
    private final ThreadLocal<FolderService> folderService = new ThreadLocal<>();

    private static final ModelMapper modelMapper = new MapperConfiguration().modelMapper();

    @BeforeEach
    public void setUp() {
        FolderRepository folderRepositoryMock = mock(FolderRepository.class);
        RequestRepository requestRepositoryMock = mock(RequestRepository.class);
        UsersService usersServiceMock = mock(UsersService.class);
        PolicyEnforcement policyEnforcementMock = mock(PolicyEnforcement.class);
        Provider userInfoProviderMock = mock(Provider.class);
        RequestAuthorizationService requestAuthorizationServiceMock = mock(RequestAuthorizationService.class);
        FolderSpecificationService folderSpecificationServiceMock = mock(FolderSpecificationService.class);
        DeleteHistoryService deleteHistoryServiceMock = mock(DeleteHistoryService.class);
        folderRepository.set(folderRepositoryMock);
        requestRepository.set(requestRepositoryMock);
        usersService.set(usersServiceMock);
        policyEnforcement.set(policyEnforcementMock);
        userInfoProvider.set(userInfoProviderMock);
        requestAuthorizationService.set(requestAuthorizationServiceMock);
        folderSpecificationService.set(folderSpecificationServiceMock);
        deleteHistoryService.set(deleteHistoryServiceMock);
        folderService.set(new FolderService(folderRepositoryMock, requestRepositoryMock, modelMapper, usersServiceMock,
                policyEnforcementMock, userInfoProviderMock, requestAuthorizationServiceMock, folderSpecificationServiceMock,
                deleteHistoryServiceMock, new PermissionEntityConverter(), new ListConverter()));
    }

    @Test
    public void getFolder_whenFolderIdIsSpecified_shouldReturnFolderByGetMethod() {
        // given
        final UUID folderId = UUID.randomUUID();

        when(folderRepository.get().findById(folderId)).thenReturn(Optional.of(new Folder()));

        // when
        folderService.get().getFolder(folderId);

        // then
        verify(folderRepository.get()).findById(folderId);
    }

    @Test
    public void getFolder_whenFolderIdIsNotFound_expectedEntityNotFoundException() {
        // given
        final UUID folderId = UUID.randomUUID();

        when(folderRepository.get().findById(folderId)).thenReturn(Optional.empty());

        // when
        AtpEntityNotFoundException exception = assertThrows(
                AtpEntityNotFoundException.class,
                () -> folderService.get().getFolder(folderId)
        );

        // then
        String folderEntityName = Folder.class.getSimpleName();
        String expectedErrorMessage = String.format(AtpEntityNotFoundException.DEFAULT_ID_MESSAGE, folderEntityName, folderId);
        assertEquals(expectedErrorMessage, exception.getMessage());

        verify(folderRepository.get()).findById(folderId);
    }

    @Test
    public void createFolders_whenFolderListSpecified_shouldSaveFoldersBySaveAllMethod() {
        // given
        final Folder folder1 = new Folder();
        final Folder folder2 = new Folder();
        final List<Folder> folders = asList(folder1, folder2);

        // when
        folderService.get().createFolders(folders);

        // then
        verify(folderRepository.get()).saveAll(folders);
    }

    @Test
    public void getAllFolders_whenProjectIdIsNotSpecified_shouldReturnProjectDataByFindAllMethod() {
        // given
        final UUID projectId = null;

        // when
        folderService.get().getAllFolders(projectId);

        // then
        verify(folderRepository.get()).findAll();
    }

    @Test
    public void getAllFolders_whenProjectIdIsSpecified_shouldReturnProjectDataByFindAllByProjectIdMethod() {
        // given
        final UUID projectId = UUID.randomUUID();

        // when
        folderService.get().getAllFolders(projectId);

        // then
        verify(folderRepository.get()).findAllByProjectIdOrderByOrder(projectId);
    }

    @Test
    public void getSettingTest() {
        // given
        Folder folder = EntitiesGenerator.generateFolder("test", UUID.randomUUID());
        folder.setAutoCookieDisabled(true);
        // when
        when(folderRepository.get().findById(folder.getId())).thenReturn(Optional.of(folder));
        Settings settings = folderService.get().getSettings(folder.getId());

        // then
        assertEquals(folder.getId(), settings.getId());
        assertEquals(folder.getName(), settings.getName());
        assertEquals(true, settings.isAutoCookieDisabled());
    }

    @Test
    public void createFolder_whenAllCreationRequestSpecified_shouldCreateFolderBySaveMethod() throws Exception {
        // given
        FolderUpsetRequest request = new FolderUpsetRequest(
                "Some Folder", UUID.randomUUID(), UUID.randomUUID(), null, false, false, false, false, false, "", null,
                new Date());

        // when
        folderService.get().createFolder(request);

        // then
        verify(folderRepository.get()).save(any());
    }

    @Test
    public void createFolder_withInheritAuth_authFolderIdShouldBeSet() throws Exception {
        // given
        UUID authFolderId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID parentFolderId = UUID.randomUUID();
        InheritFromParentAuthorizationSaveRequest authRequest =
                EntitiesGenerator.generateInheritFromParentAuthSaveRequest(UUID.randomUUID());
        FolderUpsetRequest request = new FolderUpsetRequest(
                "Some Folder", projectId, parentFolderId, null, false,
                false, false, false, false, "", authRequest, new Date());
        Folder folderWithAuth = EntitiesGenerator.generateFolder("folder with auth", projectId);
        RequestAuthorization auth = EntitiesGenerator.generateBearerRequestAuthorization("token");
        folderWithAuth.setAuthorization(auth);

        // when
        folderService.get().createFolder(request);

        // then
        ArgumentCaptor<Folder> savedFolderCapture = ArgumentCaptor.forClass(Folder.class);
        verify(folderRepository.get()).save(savedFolderCapture.capture());
        Folder savedFolder = savedFolderCapture.getValue();
        Assertions.assertNotNull(savedFolder.getAuthorization());
        Assertions.assertEquals(parentFolderId,
                ((InheritFromParentRequestAuthorization) savedFolder.getAuthorization()).getAuthorizationFolderId());
    }

    @Test
    public void editFolder_changeParameters_changeParametersInFolderAndRequest() throws Exception {
        //given
        final UUID projectId = UUID.randomUUID();
        Folder folderParent = EntitiesGenerator.generateFolder("FolderParent", projectId);
        Folder folderChild = EntitiesGenerator.generateFolder("FolderChild", projectId, folderParent.getId());
        Request request1 = EntitiesGenerator.generateHttpRequest("request1", projectId, null);
        Request request2 = EntitiesGenerator.generateHttpRequest("request2", projectId, null);
        Request request3 = EntitiesGenerator.generateHttpRequest("request3", projectId, folderChild.getId());
        List<Request> projectRequests = Arrays.asList(request1, request2, request3);
        FolderEditRequest upsetRequest = new FolderEditRequest("name", projectId, null,
                null, true, true, true, true, false, null, new Date());
        Folder folder = new Folder();
        folder.setId(projectId);
        List<Folder> folderList = new ArrayList<>();
        folderList.add(folder);

        //when
        when(folderRepository.get().findById(folderParent.getId())).thenReturn(Optional.of(folderParent));
        when(folderRepository.get().findHeirsIdsByIdIn(eq(Collections.singleton(folderParent.getId()))))
                .thenReturn(Collections.singleton(folder.getId()));
        when(folderRepository.get().findAllByIdIn(any())).thenReturn(folderList);
        when(requestRepository.get().findAllByFolderIdIn(any())).thenReturn(projectRequests);
        folderService.get().editFolder(folderParent.getId(), upsetRequest);

        //then
        ArgumentCaptor<List<Folder>> captureFolders = ArgumentCaptor.forClass(ArrayList.class);
        verify(folderRepository.get(), times(2)).saveAll(captureFolders.capture());
        List<Folder> copyFolders = new ArrayList<>(captureFolders.getAllValues().get(0));
        Assertions.assertEquals(2, copyFolders.size());
        Assertions.assertEquals(copyFolders.get(0).isDisableSslCertificateVerification(),
                upsetRequest.isDisableSslCertificateVerification());
        Assertions.assertEquals(copyFolders.get(0).isDisableSslClientCertificate(),
                upsetRequest.isDisableSslClientCertificate());
        Assertions.assertEquals(copyFolders.get(0).isDisableFollowingRedirect(),
                upsetRequest.isDisableFollowingRedirect());
        Assertions.assertEquals(copyFolders.get(0).isAutoCookieDisabled(),
                upsetRequest.isAutoCookieDisabled());

        ArgumentCaptor<List<Request>> captureFoldersRequests = ArgumentCaptor.forClass(ArrayList.class);
        verify(requestRepository.get(), times(1)).saveAll(captureFoldersRequests.capture());
        List<Request> copyFoldersRequests = new ArrayList<>(captureFoldersRequests.getValue());
        Assertions.assertEquals(3, copyFoldersRequests.size());
        Assertions.assertEquals(copyFoldersRequests.get(0).isDisableFollowingRedirect(),
                upsetRequest.isDisableFollowingRedirect());
        Assertions.assertEquals(copyFoldersRequests.get(0).isDisableSslCertificateVerification(),
                upsetRequest.isDisableSslCertificateVerification());
        Assertions.assertEquals(copyFoldersRequests.get(0).isDisableSslClientCertificate(),
                upsetRequest.isDisableSslClientCertificate());
        Assertions.assertEquals(copyFoldersRequests.get(0).isAutoCookieDisabled(),
                upsetRequest.isAutoCookieDisabled());
    }

    @Test
    public void editFolder_whenAllEditRequestSpecified_shouldEditFoldersBySaveMethod() throws Exception {
        final UUID projectId = UUID.randomUUID();
        Folder folderParent = EntitiesGenerator.generateFolder("FolderParent", projectId);
        //edit parameters
        final String givenName = "NewNameFolder";
        FolderEditRequest request = new FolderEditRequest();
        request.setName(givenName);
        request.setProjectId(projectId);

        //when
        when(folderRepository.get().findById(folderParent.getId())).thenReturn(Optional.of(folderParent));
        folderService.get().editFolder(folderParent.getId(), request);

        //then
        verify(folderRepository.get()).save(any());
        assertEquals(givenName, folderParent.getName());
    }

    @Test
    public void editFolder_whenFolderIsNotFoundBySpecifiedId_shouldReturnErrorMessage() {
        //given
        final UUID folderId = UUID.randomUUID();
        final String givenName = "NewNameFolder";
        FolderEditRequest request = new FolderEditRequest();
        request.setName(givenName);

        //when
        when(folderRepository.get().findById(folderId)).thenReturn(Optional.empty());

        // then
        AtpEntityNotFoundException exception = assertThrows(
                AtpEntityNotFoundException.class,
                () -> folderService.get().editFolder(folderId, request)
        );
        String folderEntityName = Folder.class.getSimpleName();
        String expectedErrorMessage = String.format(AtpEntityNotFoundException.DEFAULT_ID_MESSAGE, folderEntityName, folderId);
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    @Test
    public void editFolder_InheritFromParentAuthSpecified_authShouldBeUpdated() throws Exception {
        //given
        final String givenName = "NewNameFolder";
        final UUID parentFolderId = UUID.randomUUID();
        InheritFromParentAuthorizationSaveRequest authSaveRequest =
                EntitiesGenerator.generateInheritFromParentAuthSaveRequest(null);
        FolderEditRequest request = new FolderEditRequest();
        request.setName(givenName);
        request.setAuthorization(authSaveRequest);
        request.setParentId(parentFolderId);
        final UUID projectId = UUID.randomUUID();
        final UUID folderId = UUID.randomUUID();
        Folder folder = EntitiesGenerator.generateFolder(givenName, projectId, parentFolderId);
        BearerRequestAuthorization bearerAuth = EntitiesGenerator.generateBearerRequestAuthorization("token");
        Folder parentFolder = EntitiesGenerator.generateFolder("parentFolder", projectId);
        parentFolder.setId(parentFolderId);
        parentFolder.setAuthorization(bearerAuth);

        //when
        when(folderRepository.get().findById(folderId)).thenReturn(Optional.of(folder));
        folderService.get().editFolder(folderId, request);

        ArgumentCaptor<Folder> savedFolderCapture = ArgumentCaptor.forClass(Folder.class);
        verify(folderRepository.get()).save(savedFolderCapture.capture());
        Folder savedFolder = savedFolderCapture.getValue();
        Assertions.assertNotNull(savedFolder);
        Assertions.assertEquals(parentFolderId,
                ((InheritFromParentRequestAuthorization) savedFolder.getAuthorization()).getAuthorizationFolderId());
    }

    @Test
    public void copyFolder_whenAllCopyRequestSpecified_shouldCopyFoldersBySaveMethod() {
        final UUID projectId = UUID.randomUUID();
        Folder folderParent = EntitiesGenerator.generateFolder("FolderParent", projectId);
        Folder folderChild = EntitiesGenerator.generateFolder("FolderChild", projectId, folderParent.getId());
        Request request3 = EntitiesGenerator.generateHttpRequest("request3", projectId, folderChild.getId());
        //copy parameters
        FolderCopyRequest request = new FolderCopyRequest();
        Set<UUID> ids = new HashSet<>(Collections.singletonList(folderChild.getId()));
        request.setIds(ids);
        request.setToFolderId(folderChild.getParentId());
        request.setProjectId(projectId);

        //when
        when(folderRepository.get().findHeirsIdsByIdIn(eq(ids)))
                .thenReturn(Collections.singleton(folderChild.getId()));
        when(folderRepository.get().findAllByIdIn(ids)).thenReturn(Collections.singletonList(folderChild));
        when(requestRepository.get().findAllByFolderIdIn(ids)).thenReturn(Collections.singletonList(request3));
        when(folderRepository.get().findById(eq(folderChild.getId()))).thenReturn(Optional.of(folderChild));
        when(folderRepository.get().findById(eq(folderChild.getParentId()))).thenReturn(Optional.of(folderParent));
        folderService.get().copyFolders(request);

        //then
        ArgumentCaptor<List<Folder>> captureFolders = ArgumentCaptor.forClass(ArrayList.class);
        verify(folderRepository.get(), times(2)).saveAll(captureFolders.capture());
        List<Folder> copyFolders = new ArrayList<>(captureFolders.getAllValues().get(0));
        assertEquals(copyFolders.size(), 1, "Copy folders objects");

        ArgumentCaptor<List<Request>> captureFoldersRequests = ArgumentCaptor.forClass(ArrayList.class);
        verify(requestRepository.get(), times(1)).saveAll(captureFoldersRequests.capture());
        List<Request> copyFoldersRequests = new ArrayList<>(captureFoldersRequests.getValue());
        assertEquals(copyFoldersRequests.size(), 1, "Copy folders requests");
    }

    @Test
    public void copyFolder_whenDestinationFolderIsNull_shouldCopyFoldersBySaveMethod() {
        final UUID projectId = UUID.randomUUID();
        Folder folderParent = EntitiesGenerator.generateFolder("FolderParent", projectId);
        Folder folderChild = EntitiesGenerator.generateFolder("FolderChild", projectId, folderParent.getId());
        Request request3 = EntitiesGenerator.generateHttpRequest("request3", projectId, folderChild.getId());
        //copy parameters
        FolderCopyRequest request = new FolderCopyRequest();
        Set<UUID> ids = new HashSet<>(Collections.singletonList(folderChild.getId()));
        request.setIds(ids);
        request.setToFolderId(null);
        request.setProjectId(projectId);

        //when
        when(folderRepository.get().findHeirsIdsByIdIn(eq(ids)))
                .thenReturn(Collections.singleton(folderChild.getId()));
        when(folderRepository.get().findAllByIdIn(ids)).thenReturn(Collections.singletonList(folderChild));
        when(requestRepository.get().findAllByFolderIdIn(ids)).thenReturn(Collections.singletonList(request3));
        folderService.get().copyFolders(request);

        //then
        ArgumentCaptor<List<Folder>> captureFolders = ArgumentCaptor.forClass(ArrayList.class);
        verify(folderRepository.get(), times(2)).saveAll(captureFolders.capture());
        List<Folder> copyFolders = new ArrayList<>(captureFolders.getAllValues().get(0));
        assertEquals(copyFolders.size(), 1, "Copy folders objects");
        assertNull(copyFolders.get(0).getParentId(), "Copy folders requests");

        ArgumentCaptor<List<Request>> captureFoldersRequests = ArgumentCaptor.forClass(ArrayList.class);
        verify(requestRepository.get(), times(1)).saveAll(captureFoldersRequests.capture());
        List<Request> copyFoldersRequests = new ArrayList<>(captureFoldersRequests.getValue());
        assertEquals(copyFoldersRequests.size(), 1, "Copy folders requests");
    }

    @Test
    public void copyFolder_whenAllCopyRequestSpecifiedAndFolderFoundInParentFolder_shouldCopyFoldersBySaveMethod() {
        final UUID projectId = UUID.randomUUID();
        Folder folderParent = EntitiesGenerator.generateFolder("FolderParent", projectId);
        Folder folderChild = EntitiesGenerator.generateFolder("FolderChild", projectId, folderParent.getId());
        Request request3 = EntitiesGenerator.generateHttpRequest("request3", projectId, folderChild.getId());
        //copy parameters
        FolderCopyRequest request = new FolderCopyRequest();
        Set<UUID> ids = new HashSet<>(Collections.singletonList(folderChild.getId()));
        request.setIds(ids);
        request.setToFolderId(folderChild.getParentId());
        request.setProjectId(projectId);

        Folder folderWithSameName = EntitiesGenerator.generateFolder(folderChild.getName(),
                folderChild.getProjectId(), folderChild.getParentId());
        Folder folderWithSameNameWithCopy = EntitiesGenerator.generateFolder(folderChild.getName() + Constants.COPY_POSTFIX,
                folderChild.getProjectId(), folderChild.getParentId());

        //when
        when(folderRepository.get().findAllByProjectIdAndParentId(any(), any()))
                .thenReturn(Arrays.asList(folderWithSameName, folderWithSameNameWithCopy));
        when(folderRepository.get().findHeirsIdsByIdIn(eq(ids))).thenReturn(Collections.singleton(folderChild.getId()));
        when(folderRepository.get().findAllByIdIn(eq(ids))).thenReturn(Collections.singletonList(folderChild));
        when(requestRepository.get().findAllByFolderIdIn(ids)).thenReturn(Collections.singletonList(request3));
        when(folderRepository.get().findById(eq(folderChild.getId()))).thenReturn(Optional.of(folderChild));
        when(folderRepository.get().findById(eq(folderChild.getParentId()))).thenReturn(Optional.of(folderParent));
        folderService.get().copyFolders(request);

        //then
        ArgumentCaptor<List<Folder>> captureFolders = ArgumentCaptor.forClass(ArrayList.class);
        verify(folderRepository.get(), times(2)).saveAll(captureFolders.capture());
        List<Folder> copyFolders = new ArrayList<>(captureFolders.getAllValues().get(0));
        assertEquals(1, copyFolders.size(), "Copy folders objects");
        assertEquals(folderChild.getName() + Constants.COPY_POSTFIX + Constants.COPY_POSTFIX,
                copyFolders.get(0).getName());

        ArgumentCaptor<List<Request>> captureFoldersRequests = ArgumentCaptor.forClass(ArrayList.class);
        verify(requestRepository.get(), times(1)).saveAll(captureFoldersRequests.capture());
        List<Request> copyFoldersRequests = new ArrayList<>(captureFoldersRequests.getValue());
        assertEquals(1, copyFoldersRequests.size(), "Copy folders requests");
    }

    @Test
    public void copyFolder_whenInheritFromParentAuthSpecified_authFolderIdShouldBeChangedForFolderAndNestedRequests() {
        final UUID projectId = UUID.randomUUID();
        //copy parameters
        UUID folderToCopyId = UUID.randomUUID();
        UUID targetFolderId = UUID.randomUUID();
        FolderCopyRequest request = new FolderCopyRequest();
        Set<UUID> ids = new HashSet<>(Collections.singletonList(folderToCopyId));
        request.setIds(ids);
        request.setToFolderId(targetFolderId);
        request.setProjectId(projectId);

        UUID parentFolderId = UUID.randomUUID();
        BearerRequestAuthorization parentAuth = EntitiesGenerator.generateBearerRequestAuthorization("token");
        Folder parentFolder = EntitiesGenerator.generateFolder("parent", projectId);
        parentFolder.setId(parentFolderId);
        parentFolder.setAuthorization(parentAuth);

        InheritFromParentRequestAuthorization copyAuth =
                EntitiesGenerator.generateInheritFromParentRequestAuth(parentFolderId);
        Folder folderToCopy = EntitiesGenerator.generateFolder("toCopy", projectId, parentFolderId);
        folderToCopy.setId(folderToCopyId);
        folderToCopy.setAuthorization(copyAuth);

        BearerRequestAuthorization targetAuth = EntitiesGenerator.generateBearerRequestAuthorization("token2");
        Folder targetFolder = EntitiesGenerator.generateFolder("target", projectId, parentFolderId);
        targetFolder.setId(targetFolderId);
        targetFolder.setAuthorization(targetAuth);

        InheritFromParentRequestAuthorization copyRequestAuth =
                EntitiesGenerator.generateInheritFromParentRequestAuth(parentFolderId);
        Request requestToCopy = EntitiesGenerator.generateHttpRequest("toCopy", projectId);
        requestToCopy.setFolderId(folderToCopyId);
        requestToCopy.setAuthorization(copyRequestAuth);

        //when
        when(folderRepository.get().findHeirsIdsByIdIn(eq(ids))).thenReturn(Collections.singleton(folderToCopyId));
        when(folderRepository.get().findAllByIdIn(ids)).thenReturn(Collections.singletonList(folderToCopy));
        when(requestRepository.get().findAllByFolderIdIn(any())).thenReturn(Collections.singletonList(requestToCopy));
        when(folderRepository.get().findById(any())).thenReturn(Optional.of(targetFolder));
        folderService.get().copyFolders(request);

        //then
        ArgumentCaptor<List<Folder>> copiedFoldersCapture = ArgumentCaptor.forClass(List.class);
        verify(folderRepository.get(), times(2)).saveAll(copiedFoldersCapture.capture());
        List<Folder> copiedFolders = copiedFoldersCapture.getAllValues().get(0);
        Assertions.assertNotNull(copiedFolders);
        Assertions.assertEquals(1, copiedFolders.size());
        RequestAuthorization folderAuth = copiedFolders.get(0).getAuthorization();
        Assertions.assertEquals(targetFolderId,
                ((InheritFromParentRequestAuthorization) folderAuth).getAuthorizationFolderId());
    }

    @Test
    public void moveFolder_whenAllMoveRequestSpecified_shouldMoveFoldersBySaveMethod() {
        final UUID projectId = UUID.randomUUID();
        Folder folderParent = EntitiesGenerator.generateFolder("FolderParent", projectId);
        Folder folderChild = EntitiesGenerator.generateFolder("FolderChild", projectId, folderParent.getId());
        //move parameters
        FolderMoveRequest request = new FolderMoveRequest();
        Set<UUID> ids = new HashSet<>(Collections.singletonList(folderChild.getId()));
        request.setIds(Collections.singleton(new IdWithModifiedWhen(folderChild.getId(), null)));
        request.setToFolderId(folderParent.getParentId());
        request.setProjectId(projectId);

        //when
        when(folderRepository.get().findAllByIdIn(ids)).thenReturn(Collections.singletonList(folderChild));

        folderService.get().moveFolders(request);

        //then
        ArgumentCaptor<List<Folder>> captureFolders = ArgumentCaptor.forClass(ArrayList.class);
        verify(folderRepository.get(), times(2)).saveAll(captureFolders.capture());
        Folder folder = captureFolders.getAllValues().get(0).get(0);
        assertEquals(folder.getId(), folderChild.getId());
        assertEquals(folder.getParentId(), folderParent.getParentId());
    }

    @Test
    public void deleteFolder_whenAllDeleteRequestSpecified_shouldDeleteFoldersByDeleteMethod() {
        final UUID projectId = UUID.randomUUID();
        Folder folderParent = EntitiesGenerator.generateFolder("FolderParent", projectId);
        Folder folderChild = EntitiesGenerator.generateFolder("FolderChild", projectId, folderParent.getId());
        //delete parameters
        FolderDeleteRequest request = new FolderDeleteRequest();
        Set<UUID> folderIds = new HashSet<>(Collections.singletonList(folderParent.getId()));
        request.setProjectId(projectId);
        request.setIds(folderIds);

        //when
        when(folderRepository.get().findHeirsIdsByIdIn(eq(folderIds)))
                .thenReturn(new HashSet<UUID>() {{
                    add(folderParent.getId());
                    add(folderChild.getId());
                }});
        when(folderRepository.get().findAllByProjectId(projectId)).thenReturn(Collections.singletonList(folderParent));
        when(policyEnforcement.get().checkAccess(any(String.class), any(UUID.class), anySet(),
                eq(Operation.DELETE))).thenReturn(true);
        folderService.get().deleteFolders(request);

        //then
        ArgumentCaptor<Set<UUID>> captureFolderIds = ArgumentCaptor.forClass(HashSet.class);
        verify(folderRepository.get(), times(1)).deleteByIdIn(captureFolderIds.capture());
        Set<UUID> deletedFolderIds = new HashSet<>(captureFolderIds.getValue());
        assertEquals(2, deletedFolderIds.size(), "Deleted folders ids");
        assertTrue(deletedFolderIds.contains(folderParent.getId()));
    }

    @Test
    public void deleteFolder_whenAccessToAnySubfolderDenied_thenAccessDeniedException() {
        final UUID projectId = UUID.randomUUID();
        Folder folderParent = EntitiesGenerator.generateFolder("FolderParent", projectId);
        //delete parameters
        FolderDeleteRequest request = new FolderDeleteRequest();
        Set<UUID> folderIds = new HashSet<>(Collections.singletonList(folderParent.getId()));
        request.setProjectId(projectId);
        request.setIds(folderIds);

        //when
        when(folderRepository.get().findAllByProjectId(projectId)).thenReturn(Collections.singletonList(folderParent));
        when(policyEnforcement.get().checkAccess(any(String.class), any(UUID.class), anySet(),
                eq(Operation.DELETE))).thenReturn(false);
        assertThrows(ItfLiteAccessDeniedException.class, () -> folderService.get().deleteFolders(request));
    }

    @Test
    public void getFolderHeirs_whenAllRequestSpecified_shouldReturnFolderHeirsCount() {
        final UUID projectId = UUID.randomUUID();
        Folder folderParent = EntitiesGenerator.generateFolder("FolderParent", projectId);
        Folder folderChild = EntitiesGenerator.generateFolder("FolderChild", projectId, folderParent.getId());
        Request request1 = EntitiesGenerator.generateHttpRequest("request1", projectId, null);
        Request request2 = EntitiesGenerator.generateHttpRequest("request2", projectId, null);
        Request request3 = EntitiesGenerator.generateHttpRequest("request3", projectId, folderChild.getId());
        List<Folder> projectFolders = Arrays.asList(folderParent, folderChild);
        List<Request> projectRequests = Arrays.asList(request1, request2, request3);
        //delete parameters
        FolderDeleteRequest request = new FolderDeleteRequest();
        Set<UUID> folderIds = new HashSet<>(Collections.singletonList(folderParent.getId()));
        request.setProjectId(projectId);
        request.setIds(folderIds);

        //when
        when(folderRepository.get().findHeirsIdsByIdIn(eq(folderIds)))
                .thenReturn(new HashSet<UUID>() {{
                    add(folderParent.getId());
                    add(folderChild.getId());
                }});
        when(requestRepository.get().findAllByFolderIdIn(StreamUtils.extractIds(projectFolders))).thenReturn(projectRequests);
        long folderCount = folderService.get().countFolderHeirs(request);

        //then
        assertEquals(folderCount, projectRequests.size(), "Folder count");
    }

    @Test
    void fewFolderAreExisted_testOrder_expectedSuccessfullyChangedOrder() {
        // given
        final UUID projectId = UUID.randomUUID();
        final int order = 0;
        final FolderOrderChangeRequest request = new FolderOrderChangeRequest(projectId, null, order);
        final Folder e = EntitiesGenerator.generateFolder("e", projectId, null);
        final List<Folder> folders = new ArrayList<>(asList(
                EntitiesGenerator.generateFolder("a", projectId, null),
                EntitiesGenerator.generateFolder("a", projectId, null),
                EntitiesGenerator.generateFolder("b", projectId, null),
                EntitiesGenerator.generateFolder("c", projectId, null),
                EntitiesGenerator.generateFolder("d", projectId, null),
                e
        ));
        final UUID changedFolderId = e.getId();

        when(folderRepository.get().findAllByProjectIdAndParentId(projectId, null)).thenReturn(folders);

        // when
        folderService.get().order(changedFolderId, request);

        // then
        ArgumentCaptor<List<Folder>> foldersSaveCaptor = ArgumentCaptor.forClass(List.class);
        verify(folderRepository.get(), times(2)).saveAll(foldersSaveCaptor.capture());
        final List<Folder> savedFolders = foldersSaveCaptor.getAllValues().get(0);
        assertNotNull(savedFolders, "Saved folders list shouldn't be null");
        assertFalse(savedFolders.isEmpty(), "Saved folders list shouldn't be empty");
        assertEquals(folders.size(), savedFolders.size(), "Saved folders list size should be equal to fetched from the db");
        final Folder changedFolder = folders.get(order);
        assertEquals(changedFolderId, changedFolder.getId(), "Changed folder id should be equal to the requested one");
        assertEquals(order, changedFolder.getOrder(), "Changed folder order should be equal to the requested value");
    }

    @Test
    public void createFolderWithPermissions_isPermissionsSet() throws Exception {
        // given
        final FolderUpsetRequest createRequest = new FolderUpsetRequest();
        createRequest.setParentId(UUID.randomUUID());
        createRequest.setProjectId(UUID.randomUUID());
        Set<UUID> assignedUsers = new HashSet<UUID>() {{
            add(UUID.randomUUID());
            add(UUID.randomUUID());
        }};
        createRequest.setPermissions(new Permissions(true, assignedUsers));
        // when
        when(folderRepository.get().save(any(Folder.class))).thenAnswer(args -> args.getArguments()[0]);
        Folder savedFolder = folderService.get().createFolder(createRequest);

        // then
        verify(usersService.get(), times(1)).grantAllPermissions(
                eq(UserManagementEntities.FOLDER.getName()), any(UUID.class), any(UUID.class), any());
        assertEquals(savedFolder.getId(), savedFolder.getPermissionFolderId());
    }

    @Test
    public void createFolderWithPermissions_isPermissionsNotSet() throws Exception {
        // given
        final FolderUpsetRequest createRequest = new FolderUpsetRequest();
        createRequest.setParentId(UUID.randomUUID());

        // when
        when(folderRepository.get().save(any(Folder.class))).thenAnswer(args -> args.getArguments()[0]);
        Folder savedFolder = folderService.get().createFolder(createRequest);

        // then
        verify(usersService.get(), never()).grantAllPermissions(
                eq(UserManagementEntities.FOLDER.getName()), any(UUID.class), any(UUID.class), any());
        assertNull(savedFolder.getPermissionFolderId());
    }

    @Test
    public void editFolderWithoutPermissions_setPermissions() throws Exception {
        // given
        // root
        //    - folder_1: {id: 1, permission_id: null}
        //            - folder_2: {id: 2, permission_id: null}
        //                    - folder_3: {id: 3, permission_id: null}
        //                    - request_2_1: {id: 2_1, permissions_id: null}
        //
        //  after updating folder_1 all folder must have permission_id: 1

        UUID projectId = UUID.randomUUID();

        Folder folder1 = new Folder();
        UUID folder1Id = UUID.randomUUID();
        folder1.setId(folder1Id);
        folder1.setProjectId(projectId);

        Folder folder2 = new Folder();
        UUID folder2Id = UUID.randomUUID();
        folder2.setId(folder2Id);
        folder2.setParentId(folder1Id);
        folder2.setProjectId(projectId);

        Folder folder3 = new Folder();
        UUID folder3Id = UUID.randomUUID();
        folder3.setId(folder3Id);
        folder3.setParentId(folder2Id);
        folder3.setProjectId(projectId);

        Request request21 = new HttpRequest();
        UUID request21Id = UUID.randomUUID();
        request21.setId(request21Id);
        request21.setFolderId(folder2Id);
        request21.setProjectId(projectId);


        final FolderEditRequest createRequest = new FolderEditRequest();
        createRequest.setProjectId(projectId);
        createRequest.setParentId(UUID.randomUUID());
        List<UUID> assignedUsers = new ArrayList<UUID>() {{
            add(UUID.randomUUID());
            add(UUID.randomUUID());
        }};
        createRequest.setPermissions(new Permissions(true, new HashSet<>(assignedUsers)));
        // when
        when(folderRepository.get().findById(folder1Id)).thenReturn(Optional.of(folder1));
        when(folderRepository.get().findById(folder2Id)).thenReturn(Optional.of(folder2));
        when(folderRepository.get().findById(folder3Id)).thenReturn(Optional.of(folder3));
        when(folderRepository.get().findAllByProjectId(any(UUID.class))).thenReturn(Arrays.asList(folder1, folder2, folder3));
        when(requestRepository.get().findAllByProjectId(any(UUID.class))).thenReturn(Arrays.asList(request21));
        when(folderRepository.get().save(any(Folder.class))).thenAnswer(args -> args.getArguments()[0]);
        when(requestRepository.get().findById(request21Id)).thenReturn(Optional.of(request21));
        folderService.get().editFolder(folder1Id, createRequest);

        // then
        ArgumentCaptor<Folder> savedFolderCapture = ArgumentCaptor.forClass(Folder.class);
        ArgumentCaptor<Request> savedRequestCapture = ArgumentCaptor.forClass(Request.class);
        verify(folderRepository.get(), times(5)).save(savedFolderCapture.capture());
        verify(requestRepository.get(), times(1)).save(savedRequestCapture.capture());
        verify(usersService.get(), times(1)).grantAllPermissions(
                eq(UserManagementEntities.FOLDER.getName()), any(UUID.class), any(UUID.class), any());

        List<Folder> savedFolder = savedFolderCapture.getAllValues();
        assertEquals(folder1Id, savedFolder.get(0).getPermissionFolderId());
        assertEquals(folder1Id, savedFolder.get(1).getPermissionFolderId());
        assertEquals(folder1Id, savedFolder.get(2).getPermissionFolderId());

        Request savedRequest = savedRequestCapture.getValue();
        assertEquals(folder1Id, savedRequest.getPermissionFolderId());
    }

    @Test
    public void editFolderWithPermissions_unsetPermissions() throws Exception {
        // given
        // root
        //    - folder_1: {id: 1, permission_id: 1}
        //            - folder_2: {id: 2, permission_id: 1}
        //                    - folder_3: {id: 3, permission_id: 1}
        //                    - request_2_1: {id: 2_1, permissions_id: 1}
        //
        // after editing folder_1 all folder must have not permission_id

        UUID projectId = UUID.randomUUID();

        Folder folder1 = new Folder();
        UUID folder1Id = UUID.randomUUID();
        folder1.setId(folder1Id);
        folder1.setProjectId(projectId);
        folder1.setPermissionFolderId(folder1Id);

        Folder folder2 = new Folder();
        UUID folder2Id = UUID.randomUUID();
        folder2.setId(folder2Id);
        folder2.setParentId(folder1Id);
        folder2.setProjectId(projectId);
        folder2.setPermissionFolderId(folder1Id);

        Folder folder3 = new Folder();
        UUID folder3Id = UUID.randomUUID();
        folder3.setId(folder3Id);
        folder3.setParentId(folder2Id);
        folder3.setProjectId(projectId);
        folder3.setPermissionFolderId(folder1Id);

        Request request21 = new HttpRequest();
        UUID request21Id = UUID.randomUUID();
        request21.setId(request21Id);
        request21.setFolderId(folder2Id);
        request21.setProjectId(projectId);
        request21.setPermissionFolderId(folder1Id);


        final FolderEditRequest createRequest = new FolderEditRequest();
        createRequest.setProjectId(projectId);
        createRequest.setPermissions(new Permissions(false, null));
        // when
        when(folderRepository.get().findById(folder1Id)).thenReturn(Optional.of(folder1));
        when(folderRepository.get().findById(folder2Id)).thenReturn(Optional.of(folder2));
        when(folderRepository.get().findById(folder3Id)).thenReturn(Optional.of(folder3));
        when(folderRepository.get().findAllByProjectId(any(UUID.class))).thenReturn(Arrays.asList(folder1, folder2, folder3));
        when(requestRepository.get().findAllByProjectId(any(UUID.class))).thenReturn(Arrays.asList(request21));
        when(folderRepository.get().save(any(Folder.class))).thenAnswer(args -> args.getArguments()[0]);
        when(requestRepository.get().findById(request21Id)).thenReturn(Optional.of(request21));
        folderService.get().editFolder(folder1Id, createRequest);

        // then
        ArgumentCaptor<Folder> savedFolderCapture = ArgumentCaptor.forClass(Folder.class);
        ArgumentCaptor<Request> savedRequestCapture = ArgumentCaptor.forClass(Request.class);
        verify(folderRepository.get(), times(5)).save(savedFolderCapture.capture());
        verify(requestRepository.get(), times(1)).save(savedRequestCapture.capture());
        verify(usersService.get(), never()).grantAllPermissions(
                eq(UserManagementEntities.FOLDER.getName()), any(UUID.class), any(UUID.class), any());

        List<Folder> savedFolder = savedFolderCapture.getAllValues();
        assertNull(savedFolder.get(0).getPermissionFolderId());
        assertNull(savedFolder.get(1).getPermissionFolderId());
        assertNull(savedFolder.get(2).getPermissionFolderId());

        Request savedRequest = savedRequestCapture.getValue();
        assertNull(savedRequest.getPermissionFolderId());
    }

    @Test
    public void editFolderWithPermissions_unsetPermissions_nestedFolderHasOwnPermissionId() throws Exception {
        // given
        // root
        //    - folder_1: {id: 1, permission_id: 1}
        //            - folder_2: {id: 2, permission_id: 1}
        //                    - folder_3: {id: 3, permission_id: 3}
        //                            - request_3_1: {id: 3_1, permission_id: 3}
        //                    - request_2_1: {id: 2_1, permissions_id: 1}
        //
        // remove permissions for folder_1 => fodler_2 and request_2_1 must have not permission_id
        // folder_3 and request_3_1 still have permission_id

        UUID projectId = UUID.randomUUID();

        Folder folder1 = new Folder();
        folder1.setName("folder_1");
        UUID folder1Id = UUID.randomUUID();
        folder1.setId(folder1Id);
        folder1.setProjectId(projectId);
        folder1.setPermissionFolderId(folder1Id);

        Folder folder2 = new Folder();
        folder2.setName("folder_2");
        UUID folder2Id = UUID.randomUUID();
        folder2.setId(folder2Id);
        folder2.setParentId(folder1Id);
        folder2.setProjectId(projectId);
        folder2.setPermissionFolderId(folder1Id);

        Folder folder3 = new Folder();
        folder3.setName("folder_3");
        UUID folder3Id = UUID.randomUUID();
        folder3.setId(folder3Id);
        folder3.setParentId(folder2Id);
        folder3.setProjectId(projectId);
        folder3.setPermissionFolderId(folder3Id);

        Request request21 = new HttpRequest();
        UUID request21Id = UUID.randomUUID();
        request21.setId(request21Id);
        request21.setFolderId(folder2Id);
        request21.setProjectId(projectId);
        request21.setPermissionFolderId(folder1Id);

        Request request31 = new HttpRequest();
        UUID request31Id = UUID.randomUUID();
        request31.setId(request31Id);
        request31.setFolderId(folder3Id);
        request31.setProjectId(projectId);
        request31.setPermissionFolderId(folder3Id);


        final FolderEditRequest createRequest = new FolderEditRequest();
        createRequest.setProjectId(projectId);
        createRequest.setPermissions(new Permissions(false, null));
        // when
        when(folderRepository.get().findById(folder1Id)).thenReturn(Optional.of(folder1));
        when(folderRepository.get().findById(folder2Id)).thenReturn(Optional.of(folder2));
        when(folderRepository.get().findById(folder3Id)).thenReturn(Optional.of(folder3));
        when(folderRepository.get().findAllByProjectId(any(UUID.class))).thenReturn(Arrays.asList(folder1, folder2, folder3));
        when(requestRepository.get().findAllByProjectId(any(UUID.class))).thenReturn(Arrays.asList(request21, request31));
        when(folderRepository.get().save(any(Folder.class))).thenAnswer(args -> args.getArguments()[0]);
        when(requestRepository.get().findById(request21Id)).thenReturn(Optional.of(request21));
        folderService.get().editFolder(folder1Id, createRequest);

        // then
        ArgumentCaptor<Folder> savedFolderCapture = ArgumentCaptor.forClass(Folder.class);
        ArgumentCaptor<Request> savedRequestCapture = ArgumentCaptor.forClass(Request.class);
        verify(folderRepository.get(), times(3)).save(savedFolderCapture.capture());
        verify(requestRepository.get(), times(1)).save(savedRequestCapture.capture());
        verify(usersService.get(), never()).grantAllPermissions(
                eq(UserManagementEntities.FOLDER.getName()), any(UUID.class), any(UUID.class), any());

        List<Folder> savedFolder = savedFolderCapture.getAllValues();
        assertNull(savedFolder.get(0).getPermissionFolderId());
        assertNull(savedFolder.get(1).getPermissionFolderId());

        Request savedRequest = savedRequestCapture.getValue();
        assertNull(savedRequest.getPermissionFolderId());
    }

    @Test
    public void copyFolderWithPermissions() {
        // given
        // root
        //    - folder_1: {id: 1, permission_id: 1}
        //            - folder_2: {id: 2, permission_id: 2}
        //                    - folder_3: {id: 3, permission_id: 2}
        //                            - request_3_1: {id: 3_1, permission_id: 2}
        //                    - request_2_1: {id: 2_1, permissions_id: 1}
        //
        // after copying folder_3 to folder_1 copied folder and request must have permission_id: 1

        UUID projectId = UUID.randomUUID();

        Folder folder1 = new Folder();
        folder1.setName("folder_1");
        UUID folder1Id = UUID.randomUUID();
        folder1.setId(folder1Id);
        folder1.setProjectId(projectId);
        folder1.setPermissionFolderId(folder1Id);

        Folder folder2 = new Folder();
        folder2.setName("folder_2");
        UUID folder2Id = UUID.randomUUID();
        folder2.setId(folder2Id);
        folder2.setParentId(folder1Id);
        folder2.setProjectId(projectId);
        folder2.setPermissionFolderId(folder2Id);

        Folder folder3 = new Folder();
        folder3.setName("folder_3");
        UUID folder3Id = UUID.randomUUID();
        folder3.setId(folder3Id);
        folder3.setParentId(folder2Id);
        folder3.setProjectId(projectId);
        folder3.setPermissionFolderId(folder2Id);

        Folder folder3Copied = new Folder();
        folder3Copied.setName("folder_3");
        UUID folder3CopiedId = UUID.randomUUID();
        folder3Copied.setId(folder3CopiedId);
        folder3Copied.setParentId(folder1Id);
        folder3Copied.setProjectId(projectId);

        Request request21 = new HttpRequest();
        UUID request21Id = UUID.randomUUID();
        request21.setId(request21Id);
        request21.setFolderId(folder2Id);
        request21.setProjectId(projectId);
        request21.setPermissionFolderId(folder1Id);

        HttpRequest request31 = new HttpRequest();
        UUID request31Id = UUID.randomUUID();
        request31.setId(request31Id);
        request31.setFolderId(folder3Id);
        request31.setProjectId(projectId);
        request31.setPermissionFolderId(folder2Id);
        request31.setRequestParams(new ArrayList<>());
        request31.setRequestHeaders(new ArrayList<>());

        HttpRequest request31Copied = new HttpRequest();
        UUID request31CopiedId = UUID.randomUUID();
        request31Copied.setId(request31CopiedId);
        request31Copied.setFolderId(folder3CopiedId);
        request31Copied.setProjectId(projectId);
        request31Copied.setPermissionFolderId(null);
        request31Copied.setRequestParams(new ArrayList<>());
        request31Copied.setRequestHeaders(new ArrayList<>());


        final FolderCopyRequest copyRequest = new FolderCopyRequest();
        copyRequest.setProjectId(projectId);
        copyRequest.setToFolderId(folder1Id);
        Set<UUID> foldersToCopy = new HashSet<UUID>() {{
            add(folder3Id);
        }};
        copyRequest.setIds(foldersToCopy);

        // when
        when(folderRepository.get().findHeirsIdsByIdIn(eq(foldersToCopy)))
                .thenReturn(foldersToCopy);
        when(folderRepository.get().findAllByProjectId(any(UUID.class)))
                .thenReturn(Arrays.asList(folder1, folder2, folder3Copied));
        when(folderRepository.get().findAllByIdIn(eq(foldersToCopy))).thenReturn(Arrays.asList(folder3));
        when(requestRepository.get().findAllByFolderIdIn(eq(foldersToCopy))).thenReturn(Arrays.asList(request31));
        when(requestRepository.get().findAllByProjectId(any(UUID.class))).thenReturn(Arrays.asList(request21, request31Copied));
        when(folderRepository.get().findById(eq(folder1Id))).thenReturn(Optional.of(folder1));
        when(folderRepository.get().findById(eq(folder2Id))).thenReturn(Optional.of(folder2));
        when(folderRepository.get().findById(eq(folder3Id))).thenReturn(Optional.of(folder3));
        when(folderRepository.get().findById(eq(folder3CopiedId))).thenReturn(Optional.of(folder3Copied));
        when(folderRepository.get().save(any(Folder.class))).thenAnswer(args -> args.getArguments()[0]);
        when(requestRepository.get().findById(eq(request31CopiedId))).thenReturn(Optional.of(request31Copied));
        folderService.get().copyFolders(copyRequest);

        // then
        ArgumentCaptor<Folder> savedFolderCapture = ArgumentCaptor.forClass(Folder.class);
        verify(folderRepository.get(), times(4)).save(savedFolderCapture.capture());
        ArgumentCaptor<Request> savedRequestCapture = ArgumentCaptor.forClass(Request.class);
        verify(requestRepository.get(), times(1)).save(savedRequestCapture.capture());

        List<Folder> savedFolders = savedFolderCapture.getAllValues();
        assertEquals(folder1Id, savedFolders.get(0).getPermissionFolderId());
        List<Request> savedRequests = savedRequestCapture.getAllValues();
        assertEquals(folder1Id, savedRequests.get(0).getPermissionFolderId());
    }

    @Test
    public void getParentAuth_parentFolderHasNonInheritAuth_authReturned() {
        // given
        UUID projectId = UUID.randomUUID();
        Folder f1 = EntitiesGenerator.generateFolder("f1", projectId, null);
        BearerRequestAuthorization f1Auth = EntitiesGenerator.generateBearerRequestAuthorization("token");
        f1.setAuthorization(f1Auth);

        Folder f2 = EntitiesGenerator.generateFolder("f2", projectId, f1.getId());
        InheritFromParentRequestAuthorization f2Auth = EntitiesGenerator.generateInheritFromParentRequestAuth(f1.getId());
        f2.setAuthorization(f2Auth);

        // when
        when(folderRepository.get().findById(eq(f1.getId()))).thenReturn(Optional.of(f1));
        when(folderRepository.get().findById(eq(f2.getId()))).thenReturn(Optional.of(f2));

        // then
        ParentRequestAuthorization parentAuth = folderService.get().getParentAuth(f2.getId());
        Assertions.assertEquals(f1.getId(), parentAuth.getId());
        Assertions.assertEquals("f1", parentAuth.getName());
        Assertions.assertEquals(RequestAuthorizationType.BEARER, parentAuth.getType());
    }

    @Test
    public void getParentAuth_allParentFolderHasInheritAuth_nullReturned() {
        // given
        UUID projectId = UUID.randomUUID();
        Folder f1 = EntitiesGenerator.generateFolder("f1", projectId, null);
        InheritFromParentRequestAuthorization f1Auth = EntitiesGenerator.generateInheritFromParentRequestAuth(null);
        f1.setAuthorization(f1Auth);

        Folder f2 = EntitiesGenerator.generateFolder("f2", projectId, f1.getId());
        InheritFromParentRequestAuthorization f2Auth = EntitiesGenerator.generateInheritFromParentRequestAuth(f1.getId());
        f2.setAuthorization(f2Auth);

        // when
        when(folderRepository.get().findById(eq(f1.getId()))).thenReturn(Optional.of(f1));
        when(folderRepository.get().findById(eq(f2.getId()))).thenReturn(Optional.of(f2));

        // then
        ParentRequestAuthorization parentAuth = folderService.get().getParentAuth(f2.getId());
        Assertions.assertNull(parentAuth);
    }

    @Test
    public void getParentAuth_parentFolderHasNotAuth_noAuthReturned() {
        // given
        UUID projectId = UUID.randomUUID();
        Folder f1 = EntitiesGenerator.generateFolder("f1", projectId, null);

        Folder f2 = EntitiesGenerator.generateFolder("f2", projectId, f1.getId());
        InheritFromParentRequestAuthorization f2Auth = EntitiesGenerator.generateInheritFromParentRequestAuth(f1.getId());
        f2.setAuthorization(f2Auth);

        // when
        when(folderRepository.get().findById(eq(f1.getId()))).thenReturn(Optional.of(f1));
        when(folderRepository.get().findById(eq(f2.getId()))).thenReturn(Optional.of(f2));

        // then
        ParentRequestAuthorization parentAuth = folderService.get().getParentAuth(f2.getId());
        Assertions.assertEquals(f1.getId(), parentAuth.getId());
        Assertions.assertEquals("f1", parentAuth.getName());
        // null == noauth
        Assertions.assertNull(parentAuth.getType());
    }

    @Test
    public void save_thenSaveInSuper_withUpdatedChildren() {
        UUID projectId = UUID.randomUUID();
        Folder folderParent = EntitiesGenerator.generateFolder("FolderParent", projectId);
        Folder folderChild = EntitiesGenerator.generateFolder("FolderChild", projectId, folderParent.getId());
        Request request1 = EntitiesGenerator.generateHttpRequest("request1", projectId, null);
        //mock
        FolderService folderServiceSpy = spy(folderService.get());
        Folder parentFolder = new Folder(folderParent);
        Folder childFolder = new Folder(folderChild);
        Request childRequest1 = new HttpRequest((HttpRequest) request1);
        Request childRequest2 = new HttpRequest((HttpRequest) request1);
        childRequest1.setFolderId(parentFolder.getId());
        childRequest2.setFolderId(parentFolder.getId());

        when(folderRepository.get().findAllByProjectId(parentFolder.getProjectId()))
                .thenReturn(Arrays.asList(parentFolder, childFolder))
                .thenReturn(Arrays.asList(parentFolder, childFolder));
        when(requestRepository.get().findAllByFolderId(parentFolder.getId()))
                .thenReturn(Arrays.asList(childRequest1, childRequest2));
        //action
        Folder expectedFolder = new Folder(folderParent);
        expectedFolder.setChildFolders("[\"" + childFolder.getName() + "\"]");
        expectedFolder.setChildRequests("[\"" + childRequest1.getName() + "\",\"" + childRequest2.getName() + "\"]");
        ArgumentCaptor<Folder> argumentCaptorFolder = ArgumentCaptor.forClass(Folder.class);
        folderServiceSpy.save(parentFolder);
        //check
        verify(folderRepository.get(), times(1)).save(argumentCaptorFolder.capture());
        assertEquals(expectedFolder, argumentCaptorFolder.getValue());
    }

    @Test
    public void saveAll_thenSaveInSuper_withUpdatedChildren() {
        UUID projectId = UUID.randomUUID();
        Folder folderParent = EntitiesGenerator.generateFolder("FolderParent", projectId);
        Folder folderChild = EntitiesGenerator.generateFolder("FolderChild", projectId, folderParent.getId());
        Request request1 = EntitiesGenerator.generateHttpRequest("request1", projectId, null);
        //mock
        FolderService folderServiceSpy = spy(folderService.get());

        Folder parentFolder = new Folder(folderParent);
        Folder childFolder1 = new Folder(folderChild);
        childFolder1.setId(new UUID(10,2));
        childFolder1.setName("FolderChild1");
        Folder childFolder2 = new Folder(folderChild);
        childFolder2.setId(new UUID(10,1));
        childFolder2.setName("FolderChild2");

        Request childRequest1 = new HttpRequest((HttpRequest) request1);
        childRequest1.setId(new UUID(20,2));
        childRequest1.setName("Name1");
        childRequest1.setFolderId(parentFolder.getId());

        Request childRequest2 = new HttpRequest((HttpRequest) request1);
        childRequest2.setId(new UUID(20,1));
        childRequest2.setName("Name2");
        childRequest2.setFolderId(parentFolder.getId());

        when(folderRepository.get().findAllByProjectId(any(UUID.class)))
                .thenReturn(Arrays.asList(parentFolder, childFolder1, childFolder2));
        when(requestRepository.get().findAllByFolderId(parentFolder.getId()))
                .thenReturn(Arrays.asList(childRequest2, childRequest1));
        //action
        Folder expectedFolder = new Folder(parentFolder);
        expectedFolder.setChildFolders("[\"" + childFolder2.getName() + "\",\"" + childFolder1.getName() + "\"]");
        expectedFolder.setChildRequests("[\"" + childRequest1.getName() + "\",\"" + childRequest2.getName() + "\"]");
        ArgumentCaptor<List<Folder>> argumentCaptorFolder = ArgumentCaptor.forClass(List.class);
        folderServiceSpy.saveAll(Arrays.asList(parentFolder));

        //check
        verify(folderRepository.get(), times(2)).saveAll(argumentCaptorFolder.capture());
        List<List<Folder>> actualFolders = argumentCaptorFolder.getAllValues();
        assertEquals(2, actualFolders.size());
        List<Folder> firstList = actualFolders.get(0);
        assertEquals(1, firstList.size());
        assertEquals(expectedFolder, firstList.get(0));
        assertEquals(0, actualFolders.get(1).size());
    }
}
