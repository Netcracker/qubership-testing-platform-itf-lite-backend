package org.qubership.atp.itf.lite.backend.ei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateBasicRequestAuthorization;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateFolder;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateHttpRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateInheritFromParentRequestAuth;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomBearerAuthorization;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequestWithFormData;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomOAuth2AuthorizationSaveRequest;
import static org.qubership.atp.itf.lite.backend.utils.Constants.COPY_POSTFIX;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ei.node.constants.Constant;
import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ExportScope;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.ei.node.services.FileService;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.ei.node.services.ObjectSaverToDiskService;
import org.qubership.atp.itf.lite.backend.components.export.ExportStrategiesRegistry;
import org.qubership.atp.itf.lite.backend.components.export.strategies.AtpExportStrategy;
import org.qubership.atp.itf.lite.backend.configuration.MapperConfiguration;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.FolderRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestRepository;
import org.qubership.atp.itf.lite.backend.ei.service.FolderImporterService;
import org.qubership.atp.itf.lite.backend.ei.service.RequestImporterService;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth2AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BasicRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BearerRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.InheritFromParentRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth2RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;
import org.qubership.atp.itf.lite.backend.service.FolderService;
import org.qubership.atp.itf.lite.backend.service.GridFsService;
import org.qubership.atp.itf.lite.backend.service.RequestService;
import org.qubership.atp.itf.lite.backend.utils.FileUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
@Isolated
public class AtpItfLiteImportExecutorTest {

    private FolderService folderService;
    @Mock
    private RequestService requestService;
    @Mock
    private FolderRepository folderRepository;
    @Mock
    private RequestRepository requestRepository;
    @Mock
    private GridFsService gridFsService;
    private final ObjectSaverToDiskService objectSaverToDiskService = new ObjectSaverToDiskService(new FileService(), false);
    private final ObjectLoaderFromDiskService objectLoaderFromDiskService = new ObjectLoaderFromDiskService();
    private AtpItfLiteImportExecutor importExecutor;
    private AtpItfLiteExportExecutor exportExecutor;

    @Captor
    private ArgumentCaptor<List<Request>> captureRequests;
    @Captor
    private ArgumentCaptor<List<Folder>> captureFolders;

    private UUID projectId;
    private List<Folder> folders;
    List<Request> requests;
    private static OAuth2AuthorizationSaveRequest oAuth2AuthorizationSaveRequest;

    private static final String DEFAULT_RESOURCES_PATH = "src/test/resources";

    @BeforeEach
    public void setUp() {
        folderService = mock(FolderService.class);
        ModelMapper modelMapper1 = mock(ModelMapper.class);
        FolderImporterService folderImporterService =
                new FolderImporterService(objectLoaderFromDiskService, folderService, modelMapper1);
        ModelMapper modelMapper = new MapperConfiguration().modelMapper();
        RequestImporterService requestImporterService = new RequestImporterService(objectLoaderFromDiskService, requestService, modelMapper, gridFsService);
        importExecutor = new AtpItfLiteImportExecutor(folderImporterService,
                requestImporterService, objectLoaderFromDiskService);
        AtpExportStrategy atpExportStrategy = new AtpExportStrategy(
                requestRepository,
                folderRepository,
                objectSaverToDiskService,
                mock(FileService.class),
                gridFsService,
                new ObjectMapper());
        exportExecutor = new AtpItfLiteExportExecutor(new ExportStrategiesRegistry(Arrays.asList(atpExportStrategy)));

        projectId = UUID.randomUUID();
        folders = new ArrayList<>();
        folders.add(generateFolder("folder", projectId));

        HttpRequest httpRequest = generateHttpRequest("httpRequest", projectId);
        httpRequest.setFolderId(folders.get(0).getId());
        httpRequest.setRequestParams(Collections.singletonList(
                new RequestParam(UUID.randomUUID(), "test", "test", "", false)));
        httpRequest.setRequestHeaders(Collections.singletonList(
                new RequestHeader(UUID.randomUUID(), "test1", "test1", "", false)));
        oAuth2AuthorizationSaveRequest = generateRandomOAuth2AuthorizationSaveRequest();
        httpRequest.setAuthorization(modelMapper.map(oAuth2AuthorizationSaveRequest, OAuth2RequestAuthorization.class));

        HttpRequest httpRequestWithFormData = generateRandomHttpRequestWithFormData();
        httpRequestWithFormData.setProjectId(projectId);
        httpRequestWithFormData.setFolderId(folders.get(0).getId());
        oAuth2AuthorizationSaveRequest = generateRandomOAuth2AuthorizationSaveRequest();
        httpRequestWithFormData.setAuthorization(modelMapper.map(oAuth2AuthorizationSaveRequest,
                OAuth2RequestAuthorization.class));

        HttpRequest httpRequestWithBearerAuth = generateHttpRequest("httpRequest1", projectId);
        httpRequestWithBearerAuth.setAuthorization(modelMapper.map(generateRandomBearerAuthorization(),
                BearerRequestAuthorization.class));

        HttpRequest httpRequestWithBasicAuth = generateHttpRequest("httpRequest2", projectId);
        httpRequestWithBasicAuth.setAuthorization(modelMapper.map(generateBasicRequestAuthorization(),
                BasicRequestAuthorization.class));

        HttpRequest httpRequestWithInheritAuth = generateHttpRequest("httpRequest3", projectId);
        httpRequestWithInheritAuth.setAuthorization(
                modelMapper.map(generateInheritFromParentRequestAuth(UUID.randomUUID()),
                        InheritFromParentRequestAuthorization.class));
        requests = new ArrayList<>();
        requests.addAll(Arrays.asList(httpRequest, httpRequestWithFormData, httpRequestWithBearerAuth,
                httpRequestWithBasicAuth, httpRequestWithInheritAuth));
    }

    @AfterEach
    public void clear() throws IOException {
        FileUtils.deleteDirectoryRecursively(getRootPath());
    }

    @Test
    public void importData_importedCorrectly() throws RuntimeException {
        ExportImportData exportData = new ExportImportData(projectId, new ExportScope(),
                ExportFormat.ATP);
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_ITF_LITE_FOLDERS.getValue(),
                folders.stream().map(entity -> entity.getId().toString()).collect(Collectors.toSet()));
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_ITF_LITE_REQUESTS.getValue(),
                requests.stream().map(entity -> entity.getId().toString()).collect(Collectors.toSet()));
        exportData.getExportScope().getEntities().put(Constant.ENTITY_PROJECTS, Sets.newHashSet(projectId.toString()));

        createExportDir(exportData);

        importExecutor.importData(exportData, getRootPath());
        verify(folderService, times(1)).saveAll(any());
        verify(requestService, times(1)).saveAll(captureRequests.capture());

        List<Request> actualRequests = captureRequests.getValue();
        actualRequests.forEach(request -> {
            if (request.getAuthorization() != null) {
                if (RequestAuthorizationType.OAUTH2.equals(request.getAuthorization().getType())) {
                    checkOauth2Request(request);
                } else if (RequestAuthorizationType.BEARER.equals(request.getAuthorization().getType())) {
                    checkBearerRequest(request);
                } else if (RequestAuthorizationType.BASIC.equals(request.getAuthorization().getType())) {
                    checkBasicRequest(request);
                } else if (RequestAuthorizationType.INHERIT_FROM_PARENT.equals(request.getAuthorization().getType())) {
                    checkInheritRequest(request);
                }
            }
        });
    }

    @Test
    public void importDataTest_dataContainsDuplications_importedCorrectly() throws RuntimeException {
        // given
        ExportImportData exportData = new ExportImportData(projectId, new ExportScope(),
                ExportFormat.ATP);
        String expectedFolderName1 = "folder";
        String expectedFolderName2 = "folder Copy";
        folders = new ArrayList<>();
        folders.add(generateFolder(expectedFolderName1, projectId));
        folders.add(generateFolder(expectedFolderName2, projectId));
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_ITF_LITE_FOLDERS.getValue(),
                folders.stream().map(entity -> entity.getId().toString()).collect(Collectors.toSet()));
        String expectedRequestName1 = "httpRequest";
        String expectedRequestName2 = "httpRequest Copy";
        HttpRequest httpRequest = generateHttpRequest(expectedRequestName1, projectId);
        httpRequest.setFolderId(folders.get(0).getId());
        HttpRequest httpRequestCopy = generateHttpRequest(expectedRequestName2, projectId);
        httpRequestCopy.setFolderId(folders.get(0).getId());
        requests = new ArrayList<>();
        requests.add(httpRequest);
        requests.add(httpRequestCopy);
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_ITF_LITE_REQUESTS.getValue(),
                requests.stream().map(entity -> entity.getId().toString()).collect(Collectors.toSet()));
        exportData.getExportScope().getEntities().put(Constant.ENTITY_PROJECTS, Sets.newHashSet(projectId.toString()));

        createExportDir(exportData);

        List<Folder> existedFolders = new ArrayList<>();
        Folder existedFolder1 = generateFolder(expectedFolderName1, projectId);
        Folder existedFolder2 = generateFolder(expectedFolderName2, projectId);
        existedFolders.add(existedFolder1);
        existedFolders.add(existedFolder2);

        List<Request> existedRequests = new ArrayList<>();
        Request existedRequest1 = generateHttpRequest(expectedRequestName1, projectId);
        existedRequest1.setFolderId(folders.get(0).getId());
        Request existedRequest2 = generateHttpRequest(expectedRequestName2, projectId);
        existedRequest2.setFolderId(folders.get(0).getId());
        existedRequests.add(existedRequest1);
        existedRequests.add(existedRequest2);

        // when
        when(requestService.getAllRequestsByProjectIdFolderIdsRequestIds(any(), any(), any())).thenReturn(existedRequests);
        doCallRealMethod().when(requestService).addPostfixIfNameIsTaken(any(), any());
        when(folderService.getAllByProjectIdAndParentId(any(), any())).thenReturn(existedFolders);
        doCallRealMethod().when(folderService).addPostfixIfFolderNameInDestinationIsTaken(any(), any());

        importExecutor.importData(exportData, getRootPath());
        // then
        verify(folderService, times(1)).saveAll(captureFolders.capture());
        verify(requestService, times(1)).saveAll(captureRequests.capture());

        List<Folder> actualFolders = captureFolders.getValue();
        assertEquals(2, actualFolders.size());
        assertTrue(actualFolders.stream().anyMatch(folder -> folder.getName().equals(expectedFolderName2 + COPY_POSTFIX)));
        assertTrue(actualFolders.stream().anyMatch(folder -> folder.getName().equals(expectedFolderName2 + COPY_POSTFIX + COPY_POSTFIX)));

        List<Request> actualRequests = captureRequests.getValue();
        assertEquals(2, actualRequests.size());
        assertTrue(actualRequests.stream().anyMatch(request -> request.getName().equals(expectedRequestName2 + COPY_POSTFIX)));
        assertTrue(actualRequests.stream().anyMatch(request -> request.getName().equals(expectedRequestName2 + COPY_POSTFIX + COPY_POSTFIX)));

    }

    private void checkInheritRequest(Request request) {
        InheritFromParentRequestAuthorization inheritFromParentRequestAuthorization =
                (InheritFromParentRequestAuthorization) request.getAuthorization();
        assertNotNull(inheritFromParentRequestAuthorization.getAuthorizationFolderId());
    }

    private void checkBasicRequest(Request request) {
        BasicRequestAuthorization basicRequestAuthorization =
                (BasicRequestAuthorization) request.getAuthorization();
        assertNotNull(basicRequestAuthorization.getUsername());
        assertNotNull(basicRequestAuthorization.getPassword());
    }

    private void checkBearerRequest(Request request) {
        BearerRequestAuthorization bearerRequestAuthorization =
                (BearerRequestAuthorization) request.getAuthorization();
        assertNotNull(bearerRequestAuthorization.getToken());
    }

    private void checkOauth2Request(Request request) {
        OAuth2RequestAuthorization actualRequestAuthorization =
                (OAuth2RequestAuthorization) request.getAuthorization();
        assertEquals(oAuth2AuthorizationSaveRequest.getUrl(), actualRequestAuthorization.getUrl());
        assertEquals(oAuth2AuthorizationSaveRequest.getClientId(), actualRequestAuthorization.getClientId());
        assertEquals(oAuth2AuthorizationSaveRequest.getClientSecret(), actualRequestAuthorization.getClientSecret());
        assertEquals(oAuth2AuthorizationSaveRequest.getUsername(), actualRequestAuthorization.getUsername());
        assertEquals(oAuth2AuthorizationSaveRequest.getPassword(), actualRequestAuthorization.getPassword());
        assertEquals(oAuth2AuthorizationSaveRequest.getScope(), actualRequestAuthorization.getScope());
    }

    @Test
    public void validateTest() {
        ExportImportData exportData = new ExportImportData(projectId, new ExportScope(),
                ExportFormat.ATP, false, true, UUID.randomUUID(),
                new HashMap<>(), new HashMap<>(), null, false);
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_ITF_LITE_FOLDERS.getValue(),
                folders.stream().map(entity -> entity.getId().toString()).collect(Collectors.toSet()));
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_ITF_LITE_REQUESTS.getValue(),
                requests.stream().map(entity -> entity.getId().toString()).collect(Collectors.toSet()));
        exportData.getExportScope().getEntities().put(Constant.ENTITY_PROJECTS, Sets.newHashSet(projectId.toString()));

        createExportDir(exportData);

        ValidationResult result = importExecutor.validateData(exportData, getRootPath());
        assertEquals(8, result.getReplacementMap().size());
        assertTrue(result.isValid());
        assertNull(result.getDetails());
    }

    private void createExportDir(ExportImportData data) {
        when(folderRepository.findAllByProjectId(any())).thenReturn(folders);
        when(requestRepository.findAllByProjectId(any())).thenReturn(requests);
        exportExecutor.exportToFolder(data, getRootPath());
    }

    public Path getRootPath() {
        return Paths.get(DEFAULT_RESOURCES_PATH, "exportImportData");
    }

}
