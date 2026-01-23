package org.qubership.atp.itf.lite.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateBearerAuthMap;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateOAuth2AuthMap;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.configuration.MapperConfiguration;
import org.qubership.atp.itf.lite.backend.enums.ContextVariableType;
import org.qubership.atp.itf.lite.backend.enums.ImportCollectionError;
import org.qubership.atp.itf.lite.backend.enums.ImportCollectionStatus;
import org.qubership.atp.itf.lite.backend.enums.auth.OAuth2GrantType;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.feign.dto.ExecuteRequestDto;
import org.qubership.atp.itf.lite.backend.feign.service.CatalogueService;
import org.qubership.atp.itf.lite.backend.feign.service.EnvironmentFeignService;
import org.qubership.atp.itf.lite.backend.feign.service.RamService;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.api.request.CollectionExecuteRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.ContextVariable;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderUpsetRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.ImportCollectionsRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.GroupResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.ImportCollectionsResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;
import org.qubership.atp.itf.lite.backend.service.context.ExecutorContextEnricher;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class CollectionServiceTest {

    private final ThreadLocal<FolderService> folderService = new ThreadLocal<>();
    private final ThreadLocal<RequestService> requestService = new ThreadLocal<>();
    private final ThreadLocal<RequestAuthorizationService> requestAuthorizationService = new ThreadLocal<>();
    private final ThreadLocal<EnvironmentFeignService> environmentService = new ThreadLocal<>();
    private final ThreadLocal<RamService> ramService = new ThreadLocal<>();
    private final ThreadLocal<CatalogueService> catalogueService = new ThreadLocal<>();
    private final ThreadLocal<MetricService> metricService = new ThreadLocal<>();
    private final ThreadLocal<DynamicVariablesService> dynamicVariablesService = new ThreadLocal<>();
    private final ThreadLocal<CookieService> cookieService = new ThreadLocal<>();
    private final ThreadLocal<CollectionsService> collectionsService = new ThreadLocal<>();

    private static final String PATH_TO_ZIP = "src/test/resources/tests/postmanCollection.zip";
    private static final String PATH_TO_JSON = "src/test/resources/tests/test_collection.json";
    private static final String PATH_TO_GRAPHQL = "src/test/resources/tests/test_collection_graphql.json";
    private static final String PATH_TO_JSON_WITH_EVENT = "src/test/resources/tests/test_collection_with_event.json";
    private static final String PATH_TO_JSON_WITH_VARIABLES = "src/test/resources/tests/postman_dynamic_variables.json";
    private static final String PATH_TO_JSON_WITH_URLENCODED = "src/test/resources/tests/test_collection_with_url_encoded.json";

    @BeforeAll
    public static void init() {
        AuthorizationUtils.setModelMapper(new MapperConfiguration().modelMapper());
        AuthorizationUtils.setObjectMapper(new MapperConfiguration().objectMapper());
        AuthorizationUtils.setTemplateResolverService(mock(TemplateResolverService.class));
    }

    @BeforeEach
    public void setUp() {
        FolderService folderServiceMock = mock(FolderService.class);
        RequestService requestServiceMock = mock(RequestService.class);
        RequestAuthorizationService requestAuthorizationServiceMock = mock(RequestAuthorizationService.class);
        EnvironmentFeignService environmentServiceMock = mock(EnvironmentFeignService.class);
        RamService ramServiceMock = mock(RamService.class);
        CatalogueService catalogueServiceMock = mock(CatalogueService.class);
        MetricService metricServiceMock = mock(MetricService.class);
        DynamicVariablesService dynamicVariablesServiceMock = mock(DynamicVariablesService.class);
        CookieService cookieServiceMock = mock(CookieService.class);
        ExecutorContextEnricher executorContextEnricherMock = mock(ExecutorContextEnricher .class);
        folderService.set(folderServiceMock);
        requestService.set(requestServiceMock);
        requestAuthorizationService.set(requestAuthorizationServiceMock);
        environmentService.set(environmentServiceMock);
        ramService.set(ramServiceMock);
        catalogueService.set(catalogueServiceMock);
        metricService.set(metricServiceMock);
        dynamicVariablesService.set(dynamicVariablesServiceMock);
        cookieService.set(cookieServiceMock);
        collectionsService.set(new CollectionsService(folderServiceMock, requestServiceMock,
                requestAuthorizationServiceMock, environmentServiceMock, ramServiceMock, catalogueServiceMock,
                metricServiceMock, dynamicVariablesServiceMock, cookieServiceMock, executorContextEnricherMock));
    }

    @Test
    public void importCollectionGoodZIP() throws Exception {
        File zipFile = new File(PATH_TO_ZIP);
        FileInputStream input = new FileInputStream(zipFile);
        MultipartFile multipartFile = new MockMultipartFile("file",
                zipFile.getName(), "text/plain", IOUtils.toByteArray(input));
        ImportCollectionsRequest request = new ImportCollectionsRequest(UUID.randomUUID(),
                "Test collection", null);
        when(folderService.get().createFolder(any())).thenReturn(new Folder());
        when(requestService.get().createRequest(any(Request.class))).thenAnswer(args -> args.getArguments()[0]);
        List<ImportCollectionsResponse> result = collectionsService.get().importCollections(multipartFile, request);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void importCollectionGoodJSON() throws Exception {
        // given
        File jsonFile = new File(PATH_TO_JSON);
        FileInputStream input = new FileInputStream(jsonFile);
        MultipartFile multipartFile = new MockMultipartFile("file",
                jsonFile.getName(), "text/plain", IOUtils.toByteArray(input));
        ImportCollectionsRequest request = new ImportCollectionsRequest(UUID.randomUUID(),
                "Test collection", null);
        Request newRequest = new HttpRequest();
        newRequest.setId(UUID.randomUUID());

        // when
        when(folderService.get().createFolder(any())).thenReturn(new Folder());
        when(requestService.get().createRequest(any(Request.class))).thenReturn(newRequest);
        List<ImportCollectionsResponse> result = collectionsService.get().importCollections(multipartFile, request);

        // then
        ArgumentCaptor<Map<String, String>> oAuth2AuthorizationParametersMapCaptor = ArgumentCaptor.forClass(HashMap.class);
        ArgumentCaptor<Map<String, String>> bearerAuthorizationParametersMapCaptor = ArgumentCaptor.forClass(HashMap.class);
        ArgumentCaptor<FolderUpsetRequest> createFolderRequestCapture =
                ArgumentCaptor.forClass(FolderUpsetRequest.class);
        verify(folderService.get(), times(3)).createFolder(createFolderRequestCapture.capture());
        ArgumentCaptor<Request> createRequestCapture = ArgumentCaptor.forClass(Request.class);
        verify(requestService.get(), times(12)).createRequest(createRequestCapture.capture());
        verify(requestAuthorizationService.get(), times(2)).parseAuthorizationFromMap(
                oAuth2AuthorizationParametersMapCaptor.capture(), eq(RequestAuthorizationType.OAUTH2));
        verify(requestAuthorizationService.get(), times(3)).parseAuthorizationFromMap(
                bearerAuthorizationParametersMapCaptor.capture(), eq(RequestAuthorizationType.BEARER));
        // responses contains info about not imported requests
        List<ImportCollectionsResponse> expectedResponses = Arrays.asList(
                new ImportCollectionsResponse(
                        "requestWithFormDataBody",
                        newRequest.getId(),
                        "Test collection",
                        "Request was imported without a file document.txt",
                        ImportCollectionStatus.WARNING,
                        ImportCollectionError.FORMDATA_FILE_REQUIRED,
                        result.get(0).getFormDataPartId()
                ),
                new ImportCollectionsResponse(
                        "requestWithFormDataBody",
                        newRequest.getId(),
                        "Test collection",
                        "Request was imported without a file ",
                        ImportCollectionStatus.WARNING,
                        ImportCollectionError.FORMDATA_FILE_REQUIRED,
                        result.get(1).getFormDataPartId()
                ),
                new ImportCollectionsResponse(
                        "requestWithBinaryBody",
                        newRequest.getId(),
                        "Test collection",
                        "Request was imported without a file.",
                        ImportCollectionStatus.WARNING,
                        ImportCollectionError.BINARY_FILE_REQUIRED,
                        null
                )
        );
        Assertions.assertEquals(expectedResponses, result);

        // OAuth2 check
        // first element is auth parameters for password_credentials
        // second element is auth parameters for client_credentials
        // order depends on order in test_collection.json
        List<Map<String, String>> authParametersMaps = oAuth2AuthorizationParametersMapCaptor.getAllValues();
        // password_credentials verify
        Map<String, String> passwordCredentialsMap = generateOAuth2AuthMap(OAuth2GrantType.PASSWORD_CREDENTIALS);
        Assertions.assertEquals(passwordCredentialsMap.size(), authParametersMaps.get(0).size());
        Assertions.assertTrue(passwordCredentialsMap.entrySet().stream().allMatch(expectedValue ->
                expectedValue.getValue().equals(authParametersMaps.get(0).get(expectedValue.getKey()))));
        // client_credentials verify
        Map<String, String> clientCredentialsMap = generateOAuth2AuthMap(OAuth2GrantType.CLIENT_CREDENTIALS);
        Assertions.assertEquals(clientCredentialsMap.size(), authParametersMaps.get(1).size());
        Assertions.assertTrue(clientCredentialsMap.entrySet().stream().allMatch(expectedValue ->
                expectedValue.getValue().equals(authParametersMaps.get(1).get(expectedValue.getKey()))));

        // Bearer check
        List<Map<String, String>> bearerAuthParametersMaps = bearerAuthorizationParametersMapCaptor.getAllValues();
        Map<String, String> bearerAuthMap = generateBearerAuthMap();
        Assertions.assertEquals(bearerAuthMap.size(), bearerAuthParametersMaps.get(1).size());
        Assertions.assertTrue(bearerAuthMap.entrySet().stream().allMatch(expectedValue ->
                expectedValue.getValue().equals(bearerAuthParametersMaps.get(1).get(expectedValue.getKey()))));
        List<Request> createdRequests = createRequestCapture.getAllValues();
        HttpRequest urlEncodedRequest = (HttpRequest) createdRequests.get(1);
        Assertions.assertEquals("key1=val1&key2=", urlEncodedRequest.getBody().getContent());
    }

    @Test
    public void importCollectionGoodGRAPHQL() throws Exception {
        // given
        File jsonFile = new File(PATH_TO_GRAPHQL);
        FileInputStream input = new FileInputStream(jsonFile);
        MultipartFile multipartFile = new MockMultipartFile("file",
                jsonFile.getName(), "text/plain", IOUtils.toByteArray(input));
        ImportCollectionsRequest request = new ImportCollectionsRequest(UUID.randomUUID(),
                "Test graphgl collection", null);
        // when
        when(folderService.get().createFolder(any())).thenReturn(new Folder());
        List<ImportCollectionsResponse> result = collectionsService.get().importCollections(multipartFile, request);
        verify(requestService.get(), times(13)).createRequest(any(Request.class));
        // then
        // responses contains info about not imported requests
        List<ImportCollectionsResponse> expectedResponses = new ArrayList<>();
        Assertions.assertEquals(expectedResponses, result);
    }

    @Test
    public void test_importCollectionWithRequestContainedUrlEncodedBody_passed() throws Exception {
        // given
        File jsonFile = new File(PATH_TO_JSON_WITH_URLENCODED);
        FileInputStream input = new FileInputStream(jsonFile);
        MultipartFile multipartFile = new MockMultipartFile("file",
                jsonFile.getName(), "text/plain", IOUtils.toByteArray(input));
        ImportCollectionsRequest request = new ImportCollectionsRequest(UUID.randomUUID(),
                "Test urlencoded collection", null);
        // when
        when(folderService.get().createFolder(any())).thenReturn(new Folder());
        collectionsService.get().importCollections(multipartFile, request);
        verify(requestService.get()).createRequest((HttpRequest)argThat(newRequest -> ((HttpRequest)newRequest).getRequestHeaders()
                .stream().anyMatch(header -> header.getValue().equals(Constants.URL_ENCODED_HEADER_VALUE))));
    }

    @Test
    public void importCollections_CollectionsWithPreAndPostScripts_ScriptsShouldBeParsed() throws Exception {
        String expectedPostScript1 = "tests[\"response code is 401\"] = responseCode.code === 401;\n" +
                "tests[\"response has WWW-Authenticate header\"] = (postman.getResponseHeader('WWW-Authenticate'));\n" +
                "\n" +
                "var authenticateHeader = postman.getResponseHeader('WWW-Authenticate'),\n" +
                "realmStart = authenticateHeader.indexOf('\"',authenticateHeader.indexOf(\"realm\")) + 1 ,\n" +
                "realmEnd = authenticateHeader.indexOf('\"',realmStart),\n" +
                "realm = authenticateHeader.slice(realmStart,realmEnd),\n" +
                "nonceStart = authenticateHeader.indexOf('\"',authenticateHeader.indexOf(\"nonce\")) + 1,\n" +
                "nonceEnd = authenticateHeader.indexOf('\"',nonceStart),\n" +
                "nonce = authenticateHeader.slice(nonceStart,nonceEnd);\n" +
                "\n" +
                "postman.setGlobalVariable('echo_digest_realm', realm);\n" +
                "postman.setGlobalVariable('echo_digest_nonce', nonce);";
        String expectedPostScript2 = "pm.test(\"test\", function() {\n" +
                "console.log(pm.collectionVariables.get(\"name\"));\n" +
                "});";
        String expectedPreScript = "pm.collectionVariables.set(\"name\", pm.collectionVariables.get(\"name\") + \"_newValue\");";
        File jsonFile = new File(PATH_TO_JSON_WITH_EVENT);
        FileInputStream input = new FileInputStream(jsonFile);
        MultipartFile multipartFile = new MockMultipartFile("file",
                jsonFile.getName(), "text/plain", IOUtils.toByteArray(input));
        ImportCollectionsRequest request = new ImportCollectionsRequest(UUID.randomUUID(),
                "Test collection", null);

        when(folderService.get().createFolder(any())).thenReturn(new Folder());
        ArgumentCaptor<Request> requestsCaptor = ArgumentCaptor.forClass(Request.class);
        when(requestService.get().createRequest(requestsCaptor.capture())).thenAnswer(args -> args.getArguments()[0]);
        doCallRealMethod().when(dynamicVariablesService.get()).insertDynamicVariablesIntoPreScripts(any(), any());

        collectionsService.get().importCollections(multipartFile, request);
        List<Request> requests = requestsCaptor.getAllValues();

        Assertions.assertEquals(expectedPostScript1, requests.get(0).getPostScripts());
        Assertions.assertEquals(expectedPostScript2, requests.get(1).getPostScripts());
        Assertions.assertEquals(expectedPreScript, requests.get(1).getPreScripts());
    }

    @Test
    public void importCollections_CollectionsWithPreAndPostScriptsWithVariables_ScriptsShouldBeParsed() throws Exception {
        // given
        String expectedPreScript = "pm.variables.set('$timestamp', pm.variables.replaceIn('{{$timestamp}}'));\r\n" +
                "pm.variables.set('$des', pm.variables.replaceIn('{{$des}}'));\r\n"
                + "pm.variables.set('$guid', pm.variables.replaceIn('{{$guid}}'));\r\n"
                + "{$user scrip}\n"
                + "blablalb";
        String expectedParamsFirst = "RequestParam(key=asd, value=asd, description=, disabled=false, generated=false)";
        String expectedParamsSecond = "RequestParam(key={{$guid}}, value={{$timestamp}}, description={{$descr}}asdsa, disabled=false, generated=false)";
        String expectedRequestHeader = "RequestHeader(key={{$headerKey}}, value={{$headerV}}, description={{$descHeader}}hh, disabled=false, generated=false)";
        String expectedBody = "asdsad{{$bodyyy}}";

        File jsonFile = new File(PATH_TO_JSON_WITH_VARIABLES);
        FileInputStream input = new FileInputStream(jsonFile);
        MultipartFile multipartFile = new MockMultipartFile("file",
                jsonFile.getName(), "text/plain", IOUtils.toByteArray(input));
        ImportCollectionsRequest request = new ImportCollectionsRequest(UUID.randomUUID(),
                "Test collection", null);
        ArgumentCaptor<Request> requestsCaptor = ArgumentCaptor.forClass(Request.class);

        // when
        when(folderService.get().createFolder(any())).thenReturn(new Folder());
        when(requestService.get().createRequest(requestsCaptor.capture())).thenAnswer(args -> args.getArguments()[0]);
        doCallRealMethod().when(dynamicVariablesService.get()).insertDynamicVariablesIntoPreScripts(any(), any());

        collectionsService.get().importCollections(multipartFile, request);

        List<Request> requests = requestsCaptor.getAllValues();
        HttpRequest httpRequest = (HttpRequest) requests.get(0);
        String actualPreScripts = requests.get(1).getPreScripts();

        // then
        Assertions.assertAll(
                () -> Assertions.assertEquals(expectedPreScript, actualPreScripts, "Not correctly create pre scripts with new variables"),
        () -> Assertions.assertEquals(expectedParamsFirst, httpRequest.getRequestParams().get(0).toString(), "Not correctly collect headers"),
        () -> Assertions.assertEquals(expectedParamsSecond, httpRequest.getRequestParams().get(1).toString(), "Not correctly collect headers"),
        () -> Assertions.assertEquals(expectedRequestHeader, httpRequest.getRequestHeaders().get(0).toString(), "Not correctly collect headers"),
        () -> Assertions.assertEquals(expectedBody, httpRequest.getBody().getContent(), "Not correctly collect content body"),
        () -> Assertions.assertEquals(RequestBodyType.JSON, httpRequest.getBody().getType(), "Not correctly body type"),
        () -> Assertions.assertEquals(HttpMethod.GET, httpRequest.getHttpMethod(), "Not correctly http method")
        );

    }

    @Test
    public void executeCollection_CollectionRequestHasContextVariables_ExecutionRequestDtoShoudContainContextVariablesMap() {
        // given
        String authToken = "authToken";
        List<ContextVariable> contextVariables = new ArrayList<ContextVariable>(){{
            add(new ContextVariable("key_1", "value_1", ContextVariableType.GLOBAL));
            add(new ContextVariable("key_2", "value_2", ContextVariableType.COLLECTION));
            add(new ContextVariable("key_3", "value_3", ContextVariableType.DATA));
            add(new ContextVariable("key_4", "value_4", ContextVariableType.LOCAL));
            add(new ContextVariable("key_5", "value_5", ContextVariableType.ENVIRONMENT));
        }};
        UUID projectId = UUID.randomUUID();
        CollectionExecuteRequest request = new CollectionExecuteRequest();
        request.setName("Test Collection");
        request.setEnvironmentIds(Collections.singletonList(UUID.randomUUID()));
        request.setEmailRecipients(Collections.singletonList("test@example.com"));
        request.setEmailTemplateId(UUID.randomUUID());
        request.setEmailSubject("Test Subject");
        request.setTaToolIds(Collections.singletonList(UUID.randomUUID()));
        request.setLogCollectorTemplateId(UUID.randomUUID());
        request.setProjectId(projectId);
        request.setContextVariables(contextVariables);
        request.setFlags(Collections.emptyList());
        request.setTreeNodes(Collections.singletonList(new GroupResponse(EntitiesGenerator.generateRandomHttpRequest(), null)));
        request.setPropagateCookies(true);
        UUID defaultTestPlanId = UUID.randomUUID();

        // when
        when(ramService.get().getDefaultCollectionRunTestPlanId(projectId)).thenReturn(defaultTestPlanId);
        when(environmentService.get().getEnvironmentSystems(any()))
                .thenReturn(Collections.singletonList(EntitiesGenerator.generateEnvironmentSystem(UUID.randomUUID(), "system")));
        when(catalogueService.get().execute(eq(authToken), any())).thenReturn(Collections.singletonList(UUID.randomUUID()));
        collectionsService.get().executeCollection(authToken, request);

        // then
        verify(ramService.get()).getDefaultCollectionRunTestPlanId(projectId);
        verify(cookieService.get(), times(1)).getNotExpiredCookiesByUserIdAndProjectId(eq(projectId));
        verify(cookieService.get(), times(1)).save(any());

        ArgumentCaptor<ExecuteRequestDto> executeRequestDtoCaptor = ArgumentCaptor.forClass(ExecuteRequestDto.class);
        verify(catalogueService.get()).execute(eq(authToken), executeRequestDtoCaptor.capture());
        ExecuteRequestDto capturedExecuteRequestDto = executeRequestDtoCaptor.getValue();

        Assertions.assertNotNull(capturedExecuteRequestDto);

        Assertions.assertEquals(capturedExecuteRequestDto.getName(), request.getName());
        Assertions.assertEquals(capturedExecuteRequestDto.getEnvironmentIds(), request.getEnvironmentIds());
        Assertions.assertEquals(capturedExecuteRequestDto.getEmailRecipients(), request.getEmailRecipients());
        Assertions.assertEquals(capturedExecuteRequestDto.getEmailTemplateId(), request.getEmailTemplateId());
        Assertions.assertEquals(capturedExecuteRequestDto.getEmailSubject(), request.getEmailSubject());
        Assertions.assertEquals(capturedExecuteRequestDto.getTaToolIds(), request.getTaToolIds());
        Assertions.assertEquals(capturedExecuteRequestDto.getLogCollectorTemplateId(), request.getLogCollectorTemplateId());
        Assertions.assertEquals(capturedExecuteRequestDto.getProjectId(), request.getProjectId());
        Assertions.assertEquals(capturedExecuteRequestDto.getTestPlanId(), defaultTestPlanId);
        Assertions.assertEquals(capturedExecuteRequestDto.getThreadCount(), 1);
        Assertions.assertEquals(capturedExecuteRequestDto.getIsMandatoryCheck(), request.isMandatoryCheck());
        Assertions.assertEquals(capturedExecuteRequestDto.getIsSsmCheck(), request.isSsmCheck());
        Assertions.assertEquals(capturedExecuteRequestDto.getIsIgnoreFailedChecks(), request.isIgnoreFailedChecks());
        Assertions.assertEquals(capturedExecuteRequestDto.getTestScenarios().get(0).getTestScenarioName(), request.getName());
        Assertions.assertEquals(capturedExecuteRequestDto.getDataSetStorageId(), request.getDataSetStorageId());
        Assertions.assertEquals(capturedExecuteRequestDto.getDatasetId(), request.getDataSetId());
        Assertions.assertEquals(capturedExecuteRequestDto.getContextVariables(), request.convertContextVariablesToMap());
    }

    @Test
    public void executeCollection_ConvertConbtextVariables_ContextVariablesHasCorrectPrefix() {
        List<ContextVariable> contextVariables = new ArrayList<ContextVariable>(){{
            add(new ContextVariable("key_1", "value_1", ContextVariableType.GLOBAL));
            add(new ContextVariable("key_2", "value_2", ContextVariableType.COLLECTION));
            add(new ContextVariable("key_3", "value_3", ContextVariableType.DATA));
            add(new ContextVariable("key_4", "value_4", ContextVariableType.LOCAL));
            add(new ContextVariable("key_5", "value_5", ContextVariableType.ENVIRONMENT));
        }};
        CollectionExecuteRequest request = new CollectionExecuteRequest();
        request.setContextVariables(contextVariables);
        Map<String, Object> contextMap = request.convertContextVariablesToMap();
        Assertions.assertTrue(contextVariables.stream().allMatch(variable -> contextMap.containsKey(variable.getContextVariableType().getContextScope().getPrefix() + variable.getKey())));
    }

    @Test
    public void processContentTypeHeader_emptyHeadersList_expectSkippedProcessing() {
        RequestBody requestBody = new RequestBody();
        List<RequestHeader> requestHeaders = new ArrayList<>();

        collectionsService.get().processContentTypeHeader(requestHeaders, requestBody);

        assertEquals(requestHeaders, requestHeaders, "Headers list shouldn't be changed");
    }

    @Test
    public void processContentTypeHeader_headersListWithUserAndAutoGeneratedHeaders_expectAddingUserHeader() {
        RequestBody requestBody = new RequestBody();
        List<RequestHeader> requestHeaders = new ArrayList<>();

        RequestHeader autoGeneratedHeader = new RequestHeader("Content-Type", "application/xml", "", false, true);
        requestHeaders.add(autoGeneratedHeader);

        RequestHeader userAddedHeader = new RequestHeader("Content-Type", "application/json", "", false, false);
        requestHeaders.add(userAddedHeader);

        collectionsService.get().processContentTypeHeader(requestHeaders, requestBody);

        assertTrue(autoGeneratedHeader.isDisabled(), "Auto generated Content-Type header should be disabled");
        assertFalse(userAddedHeader.isDisabled(), "User added Content-Type header should be enabled");
    }

    @Test
    public void processContentTypeHeader_headersListWithDisabledUserAndAutoGeneratedHeaders_expectAddingAutoGeneratedHeader() {
        RequestBody requestBody = new RequestBody();
        List<RequestHeader> requestHeaders = new ArrayList<>();

        RequestHeader autoGeneratedHeader = new RequestHeader("Content-Type", "application/xml", "", false, true);
        requestHeaders.add(autoGeneratedHeader);

        RequestHeader userDisabledHeader = new RequestHeader("Content-Type", "application/json", "", true, false);
        requestHeaders.add(userDisabledHeader);

        collectionsService.get().processContentTypeHeader(requestHeaders, requestBody);

        assertFalse(autoGeneratedHeader.isDisabled(), "Auto generated Content-Type header should be enabled");
        assertTrue(userDisabledHeader.isDisabled(), "User added Content-Type header should be disabled");
    }
}
