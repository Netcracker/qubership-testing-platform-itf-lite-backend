package org.qubership.atp.itf.lite.backend.ei;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateBearerRequestAuthorization;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateFolder;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateHttpRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateInheritFromParentRequestAuth;
import static org.qubership.atp.itf.lite.backend.utils.Constants.ATP_ITF_LITE_ROOT_REQUESTS;
import static org.qubership.atp.itf.lite.backend.utils.Constants.COLLECTION;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.ei.node.constants.Constant;
import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ExportScope;
import org.qubership.atp.ei.node.services.FileService;
import org.qubership.atp.ei.node.services.ObjectSaverToDiskService;
import org.qubership.atp.itf.lite.backend.components.export.ExportStrategiesRegistry;
import org.qubership.atp.itf.lite.backend.components.export.strategies.AtpExportStrategy;
import org.qubership.atp.itf.lite.backend.components.export.strategies.PostmanExportStrategy;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.FolderRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestRepository;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.enums.ValueType;
import org.qubership.atp.itf.lite.backend.enums.auth.OAuth2GrantType;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BearerRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth2RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileInfo;
import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;
import org.qubership.atp.itf.lite.backend.service.GridFsService;
import org.qubership.atp.itf.lite.backend.utils.Constants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
@Isolated
public class AtpItfLiteExportExecutorTest {

    private RequestRepository requestRepository;
    private FolderRepository folderRepository;
    private GridFsService gridFsService;
    private AtpItfLiteExportExecutor exportExecutor;
    private ExportStrategiesRegistry exportStrategiesRegistry;

    private UUID projectId;
    Path rootPostmanPath;

    private static final String DEFAULT_RESOURCES_PATH = "src/test/resources";

    @BeforeEach
    public void setUpAtpExport() {
        rootPostmanPath = null;
        projectId = UUID.randomUUID();
        folderRepository = mock(FolderRepository.class);
        gridFsService = mock(GridFsService.class);
        requestRepository = mock(RequestRepository.class);
        FileService fileService = new FileService();
        AtpExportStrategy atpExportStrategy = new AtpExportStrategy(
                requestRepository,
                folderRepository,
                new ObjectSaverToDiskService(fileService, true),
                fileService,
                gridFsService,
                new ObjectMapper());
        PostmanExportStrategy postmanExportStrategy = new PostmanExportStrategy(
                requestRepository,
                folderRepository,
                new ObjectSaverToDiskService(fileService, true),
                fileService,
                gridFsService);
        exportStrategiesRegistry = new ExportStrategiesRegistry(Arrays.asList(atpExportStrategy, postmanExportStrategy));
        exportExecutor = new AtpItfLiteExportExecutor(exportStrategiesRegistry);
    }

    @AfterEach
    public void after() throws IOException {
        FileUtils.deleteDirectory(getPath(Constants.FOLDERS, Folder.class.getSimpleName()).toFile());
        FileUtils.deleteDirectory(getPath(Constants.REQUESTS, HttpRequest.class.getSimpleName()).toFile());
        FileUtils.deleteDirectory(getPath(Constants.FILES).toFile());
        if (rootPostmanPath != null) {
            FileUtils.deleteDirectory(rootPostmanPath.toFile());
        }
    }

    @Test
    public void exportData_exportedCorrectly() {
        // given
        Folder rootFolder = generateFolder("folder#1", projectId);
        Folder firstLevelFolder1 = generateFolder("folder#1_1", projectId, rootFolder.getId());
        Folder secondLevelFolder1 = generateFolder("folder#1_1_1", projectId, firstLevelFolder1.getId());
        Folder firstLevelFolder2 = generateFolder("folder#1_2", projectId, rootFolder.getId());
        Folder secondLevelFolder2 = generateFolder("folder#1_2_1", projectId, firstLevelFolder2.getId());
        List<Folder> folders = Arrays.asList(rootFolder, firstLevelFolder1, secondLevelFolder1, firstLevelFolder2, secondLevelFolder2);
        HttpRequest httpRequest = generateHttpRequest("httpRequest", projectId);
        List<Request> requests = Arrays.asList(httpRequest);
        ExportImportData exportData = new ExportImportData(projectId, new ExportScope(),
                ExportFormat.ATP);
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_ITF_LITE_FOLDERS.getValue(),
                Sets.newHashSet(folders.get(2).getId().toString()));
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_ITF_LITE_REQUESTS.getValue(),
                requests.stream().map(entity -> entity.getId().toString()).collect(Collectors.toSet()));
        exportData.getExportScope().getEntities().put(Constant.ENTITY_PROJECTS, Sets.newHashSet(projectId.toString()));

        // when
        when(folderRepository.findAllByProjectId(any())).thenReturn(folders);
        when(requestRepository.findAllByProjectId(any())).thenReturn(requests);
        Map<UUID, List<FileInfo>> mapOfFiles = new HashMap<>();
        List<FileInfo> fileInfo = Collections.singletonList(
                new FileInfo(null, "fileName.txt",
                requests.get(0).getId(), UUID.randomUUID(), "binary", "text/html", 0L));
        mapOfFiles.put(requests.get(0).getId(), fileInfo);
        when(gridFsService.getFileInfosByRequestIds(any())).thenReturn(mapOfFiles);
        Map<UUID, InputStream> file = new HashMap<>();
        file.put(fileInfo.get(0).getFileId(), new ByteArrayInputStream("string".getBytes()));
        when(gridFsService.getFilesByFileInfos(any())).thenReturn(file);

        exportExecutor.exportToFolder(exportData, getRootPath());
        // then
        List<Folder> actualFolders = getActualFolders();
        List<Folder> expectedFolders = folders.subList(0, 3);
        assertTrue(expectedFolders.size() == actualFolders.size() && expectedFolders.containsAll(actualFolders)
                && actualFolders.containsAll(expectedFolders));
        List<Request> actualRequests = getActualHttpRequests();
        List<Request> expectedRequests = requests;
        Assertions.assertEquals(expectedRequests, actualRequests);
        checkFiles(fileInfo.get(0).getFileId(), fileInfo.get(0).getRequestId());
    }

    @Test
    public void exportToPostman_exportedCorrectly() throws IOException {
        Folder rootFolder = generateFolder("rootFolder", projectId);
        Folder folder1 = generateFolder("folder#1", projectId, rootFolder.getId());
        folder1.setId(new UUID(0,1));
        folder1.setAuthorization(generateBearerRequestAuthorization("token"));

        Folder folder1_1 = generateFolder("folder#1_1", projectId, folder1.getId());
        folder1_1.setId(new UUID(0,2));
        folder1_1.setAuthorization(generateInheritFromParentRequestAuth(folder1.getId()));

        Folder folder1_2 = generateFolder("folder#1_2", projectId, folder1.getId());
        folder1_2.setId(new UUID(0,3));
        Folder folder2 = generateFolder("folder#2", projectId, rootFolder.getId());
        folder2.setId(new UUID(0,4));
        Folder folder2_1 = generateFolder("folder#2_1", projectId, folder2.getId());
        folder2_1.setId(new UUID(0,5));
        Folder folder2_2 = generateFolder("folder#2_2", projectId, folder2.getId());
        folder2_2.setId(new UUID(0,6));
        //Generate different HttpRequests to cover all
        HttpRequest getRawBodyBearer = generateHttpRequest("GET.RAW_BODY.NO_AUTHORIZATION", projectId, folder1_1.getId(), TransportType.REST, 0);
        getRawBodyBearer.setId(new UUID(1,1));
        getRawBodyBearer.setUrl("https://1.com");
        getRawBodyBearer.setHttpMethod(HttpMethod.GET);
        getRawBodyBearer.setBody(new RequestBody("{\"raw\":\"text\"}", RequestBodyType.JSON));
        BearerRequestAuthorization bearer = new BearerRequestAuthorization();
        bearer.setType(RequestAuthorizationType.BEARER);
        bearer.setToken("it is token bearer");
        getRawBodyBearer.setAuthorization(bearer);

        HttpRequest soapNoAuthorization = generateHttpRequest("SOAP NO_AUTHORIZATION", projectId, folder1_2.getId(), TransportType.SOAP, 0);
        soapNoAuthorization.setId(new UUID(1,2));
        soapNoAuthorization.setUrl("https://2.com");
        soapNoAuthorization.setHttpMethod(HttpMethod.GET);
        soapNoAuthorization.setRequestHeaders(Arrays.asList(new RequestHeader(UUID.randomUUID(), "h1", "v1", "descr1", false)));
        soapNoAuthorization.setBody(new RequestBody("<tag>raw</tag>", RequestBodyType.XML));

        HttpRequest postPreUserCredentialsBinaryBody = generateHttpRequest("POST with prerequest and User AUTHORIZATION binary body", projectId, folder2_1.getId(), TransportType.REST, 0);
        postPreUserCredentialsBinaryBody.setId(new UUID(1,3));
        postPreUserCredentialsBinaryBody.setUrl("https://3.com/projectId/{{projectId}}");
        postPreUserCredentialsBinaryBody.setHttpMethod(HttpMethod.POST);
        postPreUserCredentialsBinaryBody.setRequestHeaders(Arrays.asList(new RequestHeader(UUID.randomUUID(), "h1", "v1", "descr1", true)));
        postPreUserCredentialsBinaryBody.setPreScripts("some prerequest script");
        postPreUserCredentialsBinaryBody.setBody(new RequestBody("", RequestBodyType.Binary));
        OAuth2RequestAuthorization passwordAuthorization = new OAuth2RequestAuthorization();
        passwordAuthorization.setType(RequestAuthorizationType.OAUTH2);
        passwordAuthorization.setGrantType(OAuth2GrantType.PASSWORD_CREDENTIALS);
        passwordAuthorization.setHeaderPrefix("Bearer");
        passwordAuthorization.setUrl("https://accessTokenUrl");
        passwordAuthorization.setClientId("clientId");
        passwordAuthorization.setClientSecret("encrypted client secret");
        passwordAuthorization.setUsername("username");
        passwordAuthorization.setPassword("password");
        postPreUserCredentialsBinaryBody.setAuthorization(passwordAuthorization);
        FileInfo fileInfoBinary = new FileInfo(null, "fileName.txt", postPreUserCredentialsBinaryBody.getId(), UUID.randomUUID(), "binary", "text/html", 0L);

        HttpRequest postPrePostClientCredentialsFormDataBody = generateHttpRequest("POST with pre and post and Client AUTHORIZATION formdata body", projectId, folder2_2.getId(), TransportType.REST, 0);
        postPrePostClientCredentialsFormDataBody.setId(new UUID(1,4));
        postPrePostClientCredentialsFormDataBody.setUrl("https://4.com/projectId/{{projectId}}?q1=v1&q2");
        postPrePostClientCredentialsFormDataBody.setHttpMethod(HttpMethod.POST);
        postPrePostClientCredentialsFormDataBody.setPreScripts("it is pre script");
        postPrePostClientCredentialsFormDataBody.setPostScripts("it is post script");
        OAuth2RequestAuthorization clientAuthorization = new OAuth2RequestAuthorization();
        clientAuthorization.setType(RequestAuthorizationType.OAUTH2);
        clientAuthorization.setGrantType(OAuth2GrantType.CLIENT_CREDENTIALS);
        clientAuthorization.setHeaderPrefix("Bearer");
        clientAuthorization.setUrl("https://accessTokenUrl");
        clientAuthorization.setClientId("clientId");
        clientAuthorization.setClientSecret("encrypted client secret");
        postPrePostClientCredentialsFormDataBody.setAuthorization(clientAuthorization);
        RequestBody formDataBody = new RequestBody("", RequestBodyType.FORM_DATA);
        FormDataPart formDataPart1 = new FormDataPart("k1", ValueType.TEXT, "v1", null, "contenttype", "descr1", false);
        FormDataPart formDataPart2 = new FormDataPart("k2", ValueType.TEXT, "v2", null, null, "descr2", true);
        FormDataPart formDataPart3 = new FormDataPart("f1", ValueType.FILE, "file1.txt", UUID.randomUUID(), null, "descr3", false);
        FormDataPart formDataPart4 = new FormDataPart("f2", ValueType.FILE, "file2", UUID.randomUUID(), null, "descr4", true);
        formDataBody.setFormDataBody(Arrays.asList(formDataPart1, formDataPart2, formDataPart3, formDataPart4));
        postPrePostClientCredentialsFormDataBody.setBody(formDataBody);
        FileInfo fileInfoFormData1 = new FileInfo(null, formDataPart3.getValue(), postPrePostClientCredentialsFormDataBody.getId(), UUID.randomUUID(), "binary", "text/html", 0L);
        FileInfo fileInfoFormData2 = new FileInfo(null, formDataPart4.getValue(), postPrePostClientCredentialsFormDataBody.getId(), UUID.randomUUID(), "binary", "text/html", 0L);

        HttpRequest postAuthorizationCodeGraphQlBody = generateHttpRequest("POST with code AUTHORIZATION GraphQl body", projectId, folder2.getId(), TransportType.REST, 0);
        postAuthorizationCodeGraphQlBody.setId(new UUID(1,5));
        postAuthorizationCodeGraphQlBody.setUrl("https://5.com:8080/projectId/{{projectId}}?q1=v1&q2");
        postAuthorizationCodeGraphQlBody.setHttpMethod(HttpMethod.POST);
        OAuth2RequestAuthorization codeAuthorization = new OAuth2RequestAuthorization();
        codeAuthorization.setType(RequestAuthorizationType.OAUTH2);
        codeAuthorization.setGrantType(OAuth2GrantType.AUTHORIZATION_CODE);
        codeAuthorization.setHeaderPrefix("Bearer");
        codeAuthorization.setAuthUrl("https://authUrl");
        codeAuthorization.setUrl("https://accessTokenUrl");
        codeAuthorization.setClientId("clientId");
        codeAuthorization.setClientSecret("encrypted client secret");
        postAuthorizationCodeGraphQlBody.setAuthorization(clientAuthorization);
        RequestBody graphQlBody = new RequestBody("", RequestBodyType.GraphQL);
        graphQlBody.setQuery("{\"query\"}");
        graphQlBody.setVariables("{variables for graphql}");
        postAuthorizationCodeGraphQlBody.setBody(graphQlBody);

        // Should be imported with parent auth type
        HttpRequest withInheritAuthType = generateHttpRequest("With inherit auth type", projectId, folder1.getId(), TransportType.REST, 0);
        withInheritAuthType.setId(new UUID(1,6));
        withInheritAuthType.setUrl("http://localhost:8080");
        withInheritAuthType.setHttpMethod(HttpMethod.GET);
        withInheritAuthType.setAuthorization(generateInheritFromParentRequestAuth(folder1.getId()));

        //Mocks
        when(folderRepository.findAllByIdInOrderByOrder(any())).thenReturn(Arrays.asList(folder1, folder2));
        when(folderRepository.findAllByProjectIdAndParentIdOrderByOrder(eq(projectId), eq(folder1_1.getId()))).thenReturn(null);
        when(folderRepository.findAllByProjectIdAndParentIdOrderByOrder(eq(projectId), eq(folder1_2.getId()))).thenReturn(null);
        when(folderRepository.findAllByProjectIdAndParentIdOrderByOrder(eq(projectId), eq(folder2_1.getId()))).thenReturn(null);
        when(folderRepository.findAllByProjectIdAndParentIdOrderByOrder(eq(projectId), eq(folder2_2.getId()))).thenReturn(null);
        when(folderRepository.findAllByProjectIdAndParentIdOrderByOrder(eq(projectId), eq(folder1.getId()))).thenReturn(Arrays.asList(folder1_1, folder1_2));
        when(folderRepository.findAllByProjectIdAndParentIdOrderByOrder(eq(projectId), eq(folder2.getId()))).thenReturn(Arrays.asList(folder2_1, folder2_2));
        when(requestRepository.findAllByFolderIdOrderByOrder(eq(folder1.getId()))).thenReturn(Arrays.asList(withInheritAuthType));
        when(requestRepository.findAllByFolderIdOrderByOrder(eq(folder1_1.getId()))).thenReturn(Arrays.asList(getRawBodyBearer));
        when(requestRepository.findAllByFolderIdOrderByOrder(eq(folder1_2.getId()))).thenReturn(Arrays.asList(soapNoAuthorization));
        when(requestRepository.findAllByFolderIdOrderByOrder(eq(folder2_1.getId()))).thenReturn(Arrays.asList(postPreUserCredentialsBinaryBody));
        when(requestRepository.findAllByFolderIdOrderByOrder(eq(folder2_2.getId()))).thenReturn(Arrays.asList(postPrePostClientCredentialsFormDataBody));
        when(requestRepository.findAllByFolderIdOrderByOrder(eq(folder2.getId()))).thenReturn(Arrays.asList(postAuthorizationCodeGraphQlBody));
        when(gridFsService.getFileByFileInfo(fileInfoBinary)).thenReturn(new ByteArrayInputStream("string".getBytes()));
        when(gridFsService.getFileByFileInfo(fileInfoFormData1)).thenReturn(new ByteArrayInputStream("formdata1".getBytes()));
        when(gridFsService.getFileByFileInfo(fileInfoFormData2)).thenReturn(new ByteArrayInputStream("formdata2".getBytes()));
        when(gridFsService.getFileInfoByRequestId(eq(postPreUserCredentialsBinaryBody.getId()))).thenReturn(fileInfoBinary);
        when(gridFsService.getFileInfosByRequestId(postPrePostClientCredentialsFormDataBody.getId())).thenReturn(Arrays.asList(fileInfoFormData1, fileInfoFormData2));

        //action
        ExportImportData exportData = new ExportImportData(projectId, new ExportScope(), ExportFormat.POSTMAN);
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_ITF_LITE_FOLDERS.getValue(),
                Sets.newHashSet(folder1.getId().toString(), folder2.getId().toString()));
        exportData.getExportScope().getEntities().put(Constant.ENTITY_PROJECTS, Sets.newHashSet(projectId.toString()));
        rootPostmanPath = getRootPath().resolve(UUID.randomUUID().toString());
        exportExecutor.exportToFolder(exportData, rootPostmanPath);

        //checks
        Path collection = rootPostmanPath.resolve(COLLECTION);
        assertTrue(collection.toFile().exists());
        Path expectedPath = getDefaultPath().resolve("ExportToPostmanCollection");
        String file1Name = "folder#1.collection.json";
        assertTrue(collection.resolve(file1Name).toFile().exists());
        assertEquals(FileUtils.readFileToString(expectedPath.resolve(file1Name).toFile(), "utf-8"),
                FileUtils.readFileToString(collection.resolve(file1Name).toFile(), "utf-8"));
        String file2Name = "folder#2.collection.json";
        assertTrue(collection.resolve(file2Name).toFile().exists());
        assertEquals(FileUtils.readFileToString(expectedPath.resolve(file2Name).toFile(), "utf-8"),
                FileUtils.readFileToString(collection.resolve(file2Name).toFile(), "utf-8"));
        Path pathFiles = collection.resolve(Constants.FILES);
        assertTrue(pathFiles.toFile().exists());
        File binaryFile = pathFiles.resolve(postPreUserCredentialsBinaryBody.getId().toString()).resolve(fileInfoBinary.getFileName()).toFile();
        assertTrue(binaryFile.exists());
        assertEquals("string", FileUtils.readFileToString(binaryFile, "utf-8"));
        File formDataFile1 = pathFiles.resolve(postPrePostClientCredentialsFormDataBody.getId().toString()).resolve(fileInfoFormData1.getFileName()).toFile();
        assertTrue(formDataFile1.exists());
        assertEquals("formdata1", FileUtils.readFileToString(formDataFile1, "utf-8"));
        File formDataFile2 = pathFiles.resolve(postPrePostClientCredentialsFormDataBody.getId().toString()).resolve(fileInfoFormData2.getFileName()).toFile();
        assertTrue(formDataFile2.exists());
        assertEquals("formdata2", FileUtils.readFileToString(formDataFile2, "utf-8"));
    }

    @Test
    public void exportToPostman_whenOnlyRequests_exportedCorrectly() throws IOException {
        HttpRequest request = generateHttpRequest("GET.NO_BODY.NO_AUTHORIZATION", projectId, null, TransportType.REST, 0);
        request.setId(new UUID(3,1));
        request.setUrl("someurl");
        request.setHttpMethod(HttpMethod.GET);
        request.setBody(new RequestBody("", null));

        //Mocks
        when(requestRepository.findAllByProjectIdAndIdInOrderByOrder(eq(projectId), anySet())).thenReturn(Collections.singletonList(request));

        //action
        ExportImportData exportData = new ExportImportData(projectId, new ExportScope(), ExportFormat.POSTMAN);
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_ITF_LITE_REQUESTS.getValue(), Sets.newHashSet(request.getId().toString()));
        exportData.getExportScope().getEntities().put(Constant.ENTITY_PROJECTS, Sets.newHashSet(projectId.toString()));
        rootPostmanPath = getRootPath().resolve(UUID.randomUUID().toString());
        exportExecutor.exportToFolder(exportData, rootPostmanPath);

        //checks
        Path collection = rootPostmanPath.resolve(COLLECTION);
        assertTrue(collection.toFile().exists());
        Path expectedPath = getDefaultPath().resolve("ExportToPostmanCollection");
        String fileName = ATP_ITF_LITE_ROOT_REQUESTS + ".collection.json";
        assertTrue(collection.resolve(fileName).toFile().exists());
        assertEquals(FileUtils.readFileToString(expectedPath.resolve(fileName).toFile(), "utf-8"),
                FileUtils.readFileToString(collection.resolve(fileName).toFile(), "utf-8"));
    }

    @Test
    public void exportToNtt_exportedSkipped() {
        //action
        ExportImportData exportData = new ExportImportData(projectId, new ExportScope(), ExportFormat.NTT);
        exportData.getExportScope().getEntities().put(ServiceScopeEntities.ENTITY_ITF_LITE_REQUESTS.getValue(), Sets.newHashSet());
        exportData.getExportScope().getEntities().put(Constant.ENTITY_PROJECTS, Sets.newHashSet(projectId.toString()));
        rootPostmanPath = getRootPath().resolve(UUID.randomUUID().toString());
        exportExecutor.exportToFolder(exportData, rootPostmanPath);

        //checks
        assertFalse(rootPostmanPath.toFile().exists());
    }

    private void checkFiles(UUID fileId, UUID requestId) {
        File dir = getPath(Constants.FILES, requestId.toString()).toFile();
        File[] files = dir.listFiles();
        Assertions.assertEquals(2, files.length);
        assertTrue(getPath(Constants.FILES, requestId.toString(), fileId.toString()).toFile().exists());
        assertTrue(getPath(Constants.FILES, requestId.toString(), fileId+ ".json").toFile().exists());
    }

    private List<Folder> getActualFolders() {
        String dirName = Folder.class.getSimpleName();
        File dir = getPath(Constants.FOLDERS, dirName).toFile();
        File[] files = dir.listFiles();
        return Arrays.stream(files)
                .map(file -> readObjectFromFilePath(Folder.class, Constants.FOLDERS, dirName, file.getName()))
                .collect(Collectors.toList());
    }

    private List<Request> getActualHttpRequests() {
        String httpRequestsDirName = HttpRequest.class.getSimpleName();
        File httpRequestsDir = getPath(Constants.REQUESTS, httpRequestsDirName).toFile();
        File[] httpRequestFiles = httpRequestsDir.listFiles();
        return Arrays.stream(httpRequestFiles)
                .map(file -> readObjectFromFilePath(HttpRequest.class, Constants.REQUESTS, httpRequestsDirName,
                        file.getName()))
                .collect(Collectors.toList());
    }

    public <T> T readObjectFromFilePath(Class<T> type, String... paths) {
        try {
            byte[] bytes = Files.readAllBytes(getPath(paths));
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(bytes, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Path getPath(String... path) {
        return Paths.get(getRootFilePath(), path);
    }

    private String getRootFilePath() {
        return getRootFile().getPath();
    }

    private File getRootFile() {
        return getRootPath().toFile();
    }

    public Path getRootPath() {
        String[] allSegments = AtpItfLiteExportExecutorTest.class.getName().split("[.]");
        return Paths.get(getDefaultPath().toString(), allSegments);
    }

    public Path getDefaultPath() {
        return Paths.get(DEFAULT_RESOURCES_PATH);
    }
}
