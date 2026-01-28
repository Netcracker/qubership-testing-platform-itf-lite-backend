package org.qubership.atp.itf.lite.backend.service;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateFolder;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateHttpRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateHttpRequestSaveFromHttpRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateHttpRequestWithName;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomAuthorization;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomBearerAuthorization;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequestEntitySaveRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomOAuth2AuthorizationSaveRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestEditFromRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestEntitiesCopyRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestEntitiesDeleteRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestEntitiesMoveRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestEntityCopyRequestFromRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestEntityMoveRequestFromRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestExecuteRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestHeader;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestParameter;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateZipArchiveFromBytes;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.mockTimer;
import static org.qubership.atp.itf.lite.backend.utils.Constants.COPY_POSTFIX;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.tika.mime.MimeTypeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.configuration.FeignClientsProperties;
import org.qubership.atp.itf.lite.backend.configuration.MapperConfiguration;
import org.qubership.atp.itf.lite.backend.configuration.RequestResponseSizeProperties;
import org.qubership.atp.itf.lite.backend.converters.CurlFormatToRequestConverter;
import org.qubership.atp.itf.lite.backend.converters.RequestToCurlFormatConverter;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestExecutionDetailsRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestRepository;
import org.qubership.atp.itf.lite.backend.enums.ContextVariableType;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.enums.ValueType;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.exceptions.ItfLiteException;
import org.qubership.atp.itf.lite.backend.exceptions.file.ItfLiteMaxFileException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteHttpRequestExecuteException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteResponseSizeLimitException;
import org.qubership.atp.itf.lite.backend.feign.clients.ItfPlainFeignClient;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanExecuteScriptResponseDto;
import org.qubership.atp.itf.lite.backend.feign.service.ItfFeignService;
import org.qubership.atp.itf.lite.backend.feign.service.JsScriptEngineService;
import org.qubership.atp.itf.lite.backend.feign.service.RamService;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.RequestRuntimeOptions;
import org.qubership.atp.itf.lite.backend.model.api.request.ContextVariable;
import org.qubership.atp.itf.lite.backend.model.api.request.CurlStringImportRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.ExecutionCollectionRequestExecuteRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderDeleteRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.IdWithModifiedWhen;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitiesBulkDelete;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitiesCopyRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitiesMoveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityCopyRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityCreateRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityEditRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityMoveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestOrderChangeRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.Settings;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpParamSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.GroupResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.JsExecutionResult;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionHeaderResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.collections.ExecuteStepResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.itf.ItfParametersResolveResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.itf.ItfProjectIdResolveResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;
import org.qubership.atp.itf.lite.backend.model.entities.FileBody;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BearerRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.InheritFromParentRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth2RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.collection.run.CollectionRunRequest;
import org.qubership.atp.itf.lite.backend.model.entities.collection.run.CollectionRunStackRequest;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.qubership.atp.itf.lite.backend.model.entities.history.HttpRequestExecutionDetails;
import org.qubership.atp.itf.lite.backend.model.entities.history.RequestExecution;
import org.qubership.atp.itf.lite.backend.model.entities.history.RequestExecutionDetails;
import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;
import org.qubership.atp.itf.lite.backend.service.context.ExecutorContextEnricher;
import org.qubership.atp.itf.lite.backend.service.history.iface.DeleteHistoryService;
import org.qubership.atp.itf.lite.backend.service.rest.HttpClientService;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;
import org.qubership.atp.itf.lite.backend.utils.RequestTestUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.qubership.atp.ram.enums.TestingStatuses;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RequestServiceTest {

    private final ThreadLocal<RequestRepository> repository = new ThreadLocal<>();
    private final ThreadLocal<ItfFeignService> itfFeignService = new ThreadLocal<>();
    private final ThreadLocal<ItfPlainFeignClient> itfPlainFeignClient = new ThreadLocal<>();
    private final ThreadLocal<FeignClientsProperties> feignClientsProperties = new ThreadLocal<>();
    private final ThreadLocal<GridFsService> gridFsService = new ThreadLocal<>();
    private final ThreadLocal<RequestExecutionHistoryService> executionHistoryService = new ThreadLocal<>();
    private final ThreadLocal<FolderService> folderService = new ThreadLocal<>();
    private final ThreadLocal<EnvironmentVariableService> environmentVariableService = new ThreadLocal<>();
    private final ThreadLocal<MetricService> metricService = new ThreadLocal<>();
    private final ThreadLocal<HttpClientService> httpClientService = new ThreadLocal<>();
    private final ThreadLocal<JsScriptEngineService> scriptService = new ThreadLocal<>();
    private final ThreadLocal<ItfLiteFileService> fileService = new ThreadLocal<>();
    private final ThreadLocal<TemplateResolverService> templateResolverService = new ThreadLocal<>();
    private final ThreadLocal<RamService> ramService = new ThreadLocal<>();
    private final ThreadLocal<CookieService> cookieService = new ThreadLocal<>();
    private final ThreadLocal<RequestExecutionDetailsRepository> detailsRepository = new ThreadLocal<>();
    private final ThreadLocal<NextRequestService> nextRequestService = new ThreadLocal<>();
    private final ThreadLocal<RequestResponseSizeProperties> requestResponseSizeProperties = new ThreadLocal<>();
    private final ThreadLocal<DeleteHistoryService> deleteHistoryService = new ThreadLocal<>();
    private final ThreadLocal<RequestService> requestService = new ThreadLocal<>();
    private final ThreadLocal<CurlFormatToRequestConverter> curlToRequestConverter = new ThreadLocal<>();
    private static final ModelMapper modelMapper = new MapperConfiguration().modelMapper();
    private static final ObjectMapper objectMapper = new MapperConfiguration().objectMapper();

    @BeforeAll
    public static void init() {
        AuthorizationUtils.setObjectMapper(new ObjectMapper());
        AuthorizationUtils.setTemplateResolverService(mock(TemplateResolverService.class));
        AuthorizationUtils.setModelMapper(new MapperConfiguration().modelMapper());
    }

    @BeforeEach
    public void setUp() throws IOException, AtpDecryptException {
        RequestRepository repositoryMock = mock(RequestRepository.class);
        ItfFeignService itfFeignServiceMock = mock(ItfFeignService.class);
        ItfPlainFeignClient itfPlainFeignClientMock = mock(ItfPlainFeignClient.class);
        FeignClientsProperties feignClientsPropertiesMock = mock(FeignClientsProperties.class);
        GridFsService gridFsServiceMock = mock(GridFsService.class);
        RequestExecutionHistoryService executionHistoryServiceMock = mock(RequestExecutionHistoryService.class);
        FolderService folderServiceMock = mock(FolderService.class);
        RequestAuthorizationService requestAuthorizationServiceMock = mock(RequestAuthorizationService.class);
        EnvironmentVariableService environmentVariableServiceMock = mock(EnvironmentVariableService.class);
        RequestSpecificationService requestSpecificationServiceMock = mock(RequestSpecificationService.class);
        MetricService metricServiceMock = mock(MetricService.class);
        HttpClientService httpClientServiceMock = mock(HttpClientService.class);
        MacrosService macrosServiceMock = mock(MacrosService.class);
        JsScriptEngineService scriptServiceMock = mock(JsScriptEngineService.class);
        ItfLiteFileService fileServiceMock = mock(ItfLiteFileService.class);
        TemplateResolverService templateResolverServiceMock = mock(TemplateResolverService.class);
        RamService ramServiceMock = mock(RamService.class);
        DynamicVariablesService dynamicVariablesServiceMock = mock(DynamicVariablesService.class);
        WritePermissionsService writePermissionsServiceMock = mock(WritePermissionsService.class);
        CookieService cookieServiceMock = mock(CookieService.class);
        RequestExecutionDetailsRepository detailsRepositoryMock = mock(RequestExecutionDetailsRepository.class);
        NextRequestService nextRequestServiceMock = mock(NextRequestService.class);
        RequestResponseSizeProperties requestResponseSizePropertiesMock = mock(RequestResponseSizeProperties.class);
        DeleteHistoryService deleteHistoryServiceMock = mock(DeleteHistoryService.class);
        ExecutorContextEnricher executorContextEnricherMock = mock(ExecutorContextEnricher.class);

        when(repositoryMock.save(any(Request.class))).thenAnswer(answer -> answer.getArguments()[0]);
        when(macrosServiceMock.createMacrosEvaluator(any())).thenReturn(RequestTestUtils.generateEvaluator());

        RequestExecutionDetails requestExecutionDetails = new RequestExecutionDetails();
        requestExecutionDetails.setId(new UUID(0,0));
        RequestExecution requestExecution = new RequestExecution();
        requestExecution.setId(new UUID(0,0));
        requestExecutionDetails.setRequestExecution(requestExecution);
        lenient().when(detailsRepositoryMock.findByRequestExecutionSseId(any(UUID.class))).thenReturn(Optional.of(requestExecutionDetails));
        lenient().when(executionHistoryServiceMock.logRequestJsExecution(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new JsExecutionResult(true, null));
        lenient().when(requestResponseSizePropertiesMock.getRequestSizeLimitInMb()).thenReturn(100);
        lenient().when(requestResponseSizePropertiesMock.getResponseSizeLimitInMb()).thenReturn(100);

        CurlFormatToRequestConverter curlConverter = spy(new CurlFormatToRequestConverter());
        curlToRequestConverter.set(curlConverter);

        repository.set(repositoryMock);
        itfFeignService.set(itfFeignServiceMock);
        itfPlainFeignClient.set(itfPlainFeignClientMock);
        feignClientsProperties.set(feignClientsPropertiesMock);
        gridFsService.set(gridFsServiceMock);
        executionHistoryService.set(executionHistoryServiceMock);
        folderService.set(folderServiceMock);
        environmentVariableService.set(environmentVariableServiceMock);
        metricService.set(metricServiceMock);
        httpClientService.set(httpClientServiceMock);
        scriptService.set(scriptServiceMock);
        fileService.set(fileServiceMock);
        templateResolverService.set(templateResolverServiceMock);
        ramService.set(ramServiceMock);
        cookieService.set(cookieServiceMock);
        detailsRepository.set(detailsRepositoryMock);
        nextRequestService.set(nextRequestServiceMock);
        requestResponseSizeProperties.set(requestResponseSizePropertiesMock);
        deleteHistoryService.set(deleteHistoryServiceMock);
        requestService.set(new RequestService(executorContextEnricherMock, repositoryMock, modelMapper, objectMapper,
                curlConverter, new RequestToCurlFormatConverter(), itfFeignServiceMock,
                itfPlainFeignClientMock, feignClientsPropertiesMock, gridFsServiceMock, executionHistoryServiceMock,
                folderServiceMock, requestAuthorizationServiceMock, environmentVariableServiceMock,
                requestSpecificationServiceMock, metricServiceMock, httpClientServiceMock, macrosServiceMock, scriptServiceMock,
                fileServiceMock, templateResolverServiceMock, ramServiceMock, dynamicVariablesServiceMock, writePermissionsServiceMock,
                cookieServiceMock, detailsRepositoryMock, nextRequestServiceMock, requestResponseSizePropertiesMock, deleteHistoryServiceMock));
    }

    @Test
    public void getResponseBodyType_whenContentTypeIsTextHtml_returnResponseBodyTypeHtml() {
        // given
        Header[] responseHeaders = {
                new BasicHeader("Date", "Wed, 22 May 2024 12:59:46 GMT"),
                new BasicHeader("Content-Type", "text/html"),
                new BasicHeader("Content-Length", "1098"),
                new BasicHeader("Connection", "keep-alive"),
                new BasicHeader("Vary", "Origin"),
                new BasicHeader("Vary", "Access-Control-Request-Method"),
                new BasicHeader("Vary", "Access-Control-Request-Headers"),
                new BasicHeader("Strict-Transport-Security", "max-age=15724800; includeSubDomains")
        };
        // when
        RequestBodyType requestBodyType = requestService.get().getResponseBodyType(responseHeaders);

        // then
        assertEquals(requestBodyType.getName(), RequestBodyType.HTML.getName());
    }

    @Test
    public void getResponseBodyType_whenContentTypeIsApplicationJsonAndContentDisposition_returnResponseBodyTypeBinary() {
        // given
        Header[] responseHeaders = {
                new BasicHeader("Content-Type", "application/json"),
                new BasicHeader("Content-Disposition", "attachment; filename=test.json"),
        };
        // when
        RequestBodyType requestBodyType = requestService.get().getResponseBodyType(responseHeaders);

        // then
        assertEquals(requestBodyType, RequestBodyType.Binary);
    }

    @Test
    public void getRequest_whenRequestIdIsSpecified_shouldReturnRequestByGetMethod() {
        // given
        final UUID requestId = UUID.randomUUID();

        // when
        HttpRequest request = new HttpRequest();
        request.setRequestHeaders(new ArrayList<>());
        when(repository.get().findById(requestId)).thenReturn(Optional.of(request));
        requestService.get().getRequest(requestId);
        // then
        verify(repository.get()).findById(requestId);
    }

    @Test
    public void getPermissionFolderIdsByRequestIds_whenHaveEmptyPermissionFolderId_emptySet() {
        // given
        HttpRequest request = new HttpRequest();
        request.setName("name");
        List<Request> requests = new ArrayList<>();
        requests.add(request);

        // when
        when(repository.get().findAllByIdIn(any()))
                .thenReturn(requests);
        Set<UUID> uuidSet = requestService.get().getPermissionFolderIdsByRequestIds(any());

        // then
        assertEquals(Collections.emptySet(), uuidSet);
    }

    @Test
    public void generateRequestForHistory_whenParseObjectWithModelMapper() {
        // given
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        request.setHttpMethod(HttpMethod.PUT);
        request.setUrl("http://localhost/mockingbird-transport-rest/97029243-8ad2-450e-8176-2020316085d2/bucket/s3/NFM_2_20240423_5_2.zip");
        request.setRequestParams(new ArrayList<>());
        request.setRequestHeaders(new ArrayList<>());

        RequestBody requestBody = new RequestBody();
        requestBody.setContent(null);
        requestBody.setQuery(null);
        requestBody.setVariables(null);
        requestBody.setType(RequestBodyType.Binary);
        requestBody.setFormDataBody(null);
        request.setBody(requestBody);

        request.setCookies(new ArrayList<>());

        FileData fileData = new FileData();
        fileData.setContent(null);
        fileData.setFileName("NFM_2_20240423_5_3.zip");
        fileData.setFileId(null);
        fileData.setContentType(null);
        request.setFile(fileData);

        String expected = "HttpRequestEntitySaveRequest(httpMethod=PUT, "
                + "url=http://localhost/mockingbird-transport-rest/97029243-8ad2-450e-8176-2020316085d2/bucket/s3/NFM_2_20240423_5_2.zip, "
                + "requestParams=[], requestHeaders=[], body=RequestBody(content=null, query=null, variables=null, type=Binary, formDataBody=null, binaryBody=null),"
                + " file=FileData(content=null, fileName=NFM_2_20240423_5_3.zip, fileId=null, contentType=null), "
                + "cookies=[])";

        // when
        RequestEntitySaveRequest requestEntitySaveRequest  = requestService.get().generateRequestForHistory(request);

        // then
        String actual = requestEntitySaveRequest.toString();
        assertEquals(expected, actual,
                "Not correctly parsed request");
    }

    @Test
    public void getSettingTest() {
        // given
        final UUID requestId = UUID.randomUUID();
        Request request = generateRandomHttpRequest();
        request.setAutoCookieDisabled(true);

        // when
        when(repository.get().findById(requestId)).thenReturn(Optional.of(request));
        Settings settings = requestService.get().getSettings(requestId);

        // then
        assertEquals(request.getId(), settings.getId());
        assertEquals(request.getName(), settings.getName());
        assertEquals(true, settings.isAutoCookieDisabled());
    }

    @Test
    public void getRequest_whenRequestIdIsNotFound_expectedEntityNotFoundException() {
        // given
        final UUID requestId = UUID.randomUUID();

        // when
        when(repository.get().findById(requestId)).thenReturn(Optional.empty());
        AtpEntityNotFoundException exception = assertThrows(
                AtpEntityNotFoundException.class,
                () -> requestService.get().getRequest(requestId)
        );
        // then
        String requestEntityName = Request.class.getSimpleName();
        String expectedErrorMessage = String.format(AtpEntityNotFoundException.DEFAULT_ID_MESSAGE, requestEntityName, requestId);
        assertEquals(expectedErrorMessage, exception.getMessage());
        verify(repository.get()).findById(requestId);
    }

    @Test
    public void getAllRequests_whenProjectIdAndFolderIdIsNotSpecified_shouldReturnProjectDataByFindAllMethod() {
        // given
        final UUID projectId = null;
        final UUID folderId = null;
        // when
        requestService.get().getAllRequests(projectId, folderId);
        // then
        ArgumentCaptor<Specification<Request>> captorSpecificationRequest = ArgumentCaptor.forClass(Specification.class);
        verify(repository.get()).findAll(captorSpecificationRequest.capture());
    }

    @Test
    public void getAllRequests_whenProjectIdIsSpecifiedAndFolderIdIsNotSpecified_shouldReturnProjectDataByFindAllByProjectIdMethod() {
        // given
        final UUID projectId = UUID.randomUUID();
        final UUID folderId = null;
        // when
        requestService.get().getAllRequests(projectId, folderId);
        // then
        ArgumentCaptor<Specification<Request>> captorSpecificationRequest = ArgumentCaptor.forClass(Specification.class);
        verify(repository.get()).findAll(captorSpecificationRequest.capture());
    }

    @Test
    public void getAllRequests_whenFolderIdIsSpecifiedAndRequestIdIsNotSpecified_shouldReturnProjectDataByFindAllByFolderIdMethod() {
        // given
        final UUID folderId = UUID.randomUUID();
        final UUID projectId = null;
        // when
        requestService.get().getAllRequests(projectId, folderId);
        // then
        ArgumentCaptor<Specification<Request>> captorSpecificationRequest = ArgumentCaptor.forClass(Specification.class);
        verify(repository.get()).findAll(captorSpecificationRequest.capture());
    }

    @Test
    public void getAllRequests_whenProjecIdAndFolderIdAreSpecified_shouldReturnProjectDataByFindAllByProjectIdAndFolderIdMethod() {
        // given
        final UUID projectId = UUID.randomUUID();
        final UUID folderId = UUID.randomUUID();

        // when
        requestService.get().getAllRequests(projectId, folderId);
        // then
        ArgumentCaptor<Specification<Request>> captorSpecificationRequest = ArgumentCaptor.forClass(Specification.class);
        verify(repository.get()).findAll(captorSpecificationRequest.capture());
    }

    @Test
    public void getAllRequests_whenProjectIdAndFolderIdsAndRequestIdsAreSpecified_shouldReturnProjectDataByFindAllByProjectIdAndFolderIdInAndIdInMethod() {
        // given
        final UUID requestId = UUID.randomUUID();
        final UUID projectId = UUID.randomUUID();
        final UUID folderId = UUID.randomUUID();

        // when
        requestService.get().getAllRequestsByProjectIdFolderIdsRequestIds(projectId, singleton(folderId),
                singleton(requestId));
        // then
        ArgumentCaptor<Specification<Request>> captorSpecificationRequest = ArgumentCaptor.forClass(Specification.class);
        verify(repository.get()).findAll(captorSpecificationRequest.capture());
    }

    @Test
    public void getAllRequests_whenProjectIdIsSpecified_shouldReturnProjectDataByFindAllByProjectIdMethod() {
        // given
        final UUID projectId = UUID.randomUUID();

        // when
        requestService.get().getAllRequestsByProjectIdFolderIdsRequestIds(projectId, null, null);
        // then
        ArgumentCaptor<Specification<Request>> captorSpecificationRequest = ArgumentCaptor.forClass(Specification.class);
        verify(repository.get()).findAll(captorSpecificationRequest.capture());
    }

    @Test
    public void getAllRequests_whenProjectIdAndFolderIdsAreSpecified_shouldReturnProjectDataByFindAllByProjectIdAndFolderIdInMethod() {
        // given
        final UUID projectId = UUID.randomUUID();
        final UUID folderId = UUID.randomUUID();

        // when
        requestService.get().getAllRequestsByProjectIdFolderIdsRequestIds(projectId, singleton(folderId), null);
        // then
        ArgumentCaptor<Specification<Request>> captorSpecificationRequest = ArgumentCaptor.forClass(Specification.class);
        verify(repository.get()).findAll(captorSpecificationRequest.capture());
    }

    @Test
    public void getAllRequests_whenProjectIdAndRequestIdsAreSpecified_shouldReturnProjectDataByByProjectIdAndIdInMethod() {
        // given
        final UUID requestId = UUID.randomUUID();
        final UUID projectId = UUID.randomUUID();

        // when
        requestService.get().getAllRequestsByProjectIdFolderIdsRequestIds(projectId, null, singleton(requestId));
        // then
        ArgumentCaptor<Specification<Request>> captorSpecificationRequest = ArgumentCaptor.forClass(Specification.class);
        verify(repository.get()).findAll(captorSpecificationRequest.capture());
    }

    @Test
    public void createRequests_whenRequestListSpecified_shouldSaveRequestsBySaveAllMethod() {
        // given
        final Request request1 = new HttpRequest();
        final Request request2 = new HttpRequest();
        final List<Request> requests = asList(request1, request2);
        // when
        requestService.get().createRequests(requests);
        // then
        verify(repository.get()).saveAll(requests);
    }

    @Test
    public void createHttpRequest_whenAllCreationRequestSpecified_shouldCreateRequestBySaveMethod() {
        // given
        RequestEntityCreateRequest request = new RequestEntityCreateRequest("Some Request", UUID.randomUUID(),
                UUID.randomUUID(), TransportType.REST);
        // when
        requestService.get().createRequest(request);
        // then
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(repository.get()).save(requestCaptor.capture());
        assertTrue(requestCaptor.getValue() instanceof HttpRequest);
    }

    @Test
    public void saveHttpRequest_whenAllSaveRequestSpecified_shouldUpdateRequestBySaveMethod() {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        HttpRequestEntitySaveRequest httpRequestEntitySaveRequest = generateHttpRequestSaveFromHttpRequest(httpRequest);
        String newName = "New request name";
        httpRequestEntitySaveRequest.setName(newName);
        String newUrl = "http://new.new";
        httpRequestEntitySaveRequest.setUrl(newUrl);
        HttpMethod newMethod = HttpMethod.PUT;
        httpRequestEntitySaveRequest.setHttpMethod(newMethod);
        RequestBody newBody = new RequestBody("{\"property\": \"value\"}", RequestBodyType.JSON);
        httpRequestEntitySaveRequest.setBody(newBody);
        // remove one of headers
        HttpHeaderSaveRequest requestHeader = httpRequestEntitySaveRequest.getRequestHeaders().get(0);
        HttpHeaderSaveRequest generatedHeader = new HttpHeaderSaveRequest("key", "value", "", false, true);
        httpRequestEntitySaveRequest.setRequestHeaders(Arrays.asList(requestHeader, generatedHeader));
        // remove one of parameters
        HttpParamSaveRequest requestParam = httpRequestEntitySaveRequest.getRequestParams().get(0);
        httpRequestEntitySaveRequest.setRequestParams(Collections.singletonList(requestParam));
        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(httpRequest));
        when(repository.get().save(any(Request.class))).thenAnswer(answer -> answer.getArguments()[0]);
        HttpRequest actualRequest = (HttpRequest) requestService.get().saveRequest(httpRequest.getId(),
                httpRequestEntitySaveRequest, new ArrayList<>(), Optional.empty());
        // then
        verify(repository.get()).save(any());
        assertEquals(httpRequest.getId(), actualRequest.getId());
        assertEquals(httpRequest.getProjectId(), actualRequest.getProjectId());
        assertEquals(httpRequest.getFolderId(), actualRequest.getFolderId());
        assertEquals(httpRequest.getTransportType(), actualRequest.getTransportType());
        assertEquals(newName, actualRequest.getName());
        assertEquals(newUrl, actualRequest.getUrl());
        assertEquals(newMethod, actualRequest.getHttpMethod());
        assertEquals(newBody, actualRequest.getBody());
        // http request contains only one header, without generated header
        assertEquals(Collections.singletonList(httpRequest.getRequestHeaders().get(0)),
                actualRequest.getRequestHeaders());
        // http request contains ony one parameter
        assertEquals(Collections.singletonList(httpRequest.getRequestParams().get(0)),
                actualRequest.getRequestParams());
    }

    @Test
    public void saveHttpRequest_oldRequestContainsDuplicateHeaders_headerShouldBeRemoved() {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        HttpRequestEntitySaveRequest httpRequestEntitySaveRequest = generateHttpRequestSaveFromHttpRequest(httpRequest);
        httpRequestEntitySaveRequest.setRequestHeaders(httpRequestEntitySaveRequest.getRequestHeaders().subList(0, 2));

        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(httpRequest));
        when(repository.get().save(any(Request.class))).thenAnswer(answer -> answer.getArguments()[0]);
        HttpRequest actualRequest = (HttpRequest) requestService.get().saveRequest(httpRequest.getId(),
                httpRequestEntitySaveRequest, new ArrayList<>(), Optional.empty());
        // then
        verify(repository.get()).save(any());

        // http request not contains duplicated headers
        assertEquals(2, httpRequest.getRequestHeaders().size());
    }

    @Test
    public void saveHttpRequest_withFileBody_fileInfoShouldBeSaveForRequest() {
        HttpRequest request = generateRandomHttpRequest();
        HttpRequestEntitySaveRequest httpRequestEntitySaveRequest = generateHttpRequestSaveFromHttpRequest(request);
        FileBody fileBody = new FileBody("name", UUID.randomUUID());
        when(repository.get().findById(any())).thenReturn(Optional.of(request));

        requestService.get().saveRequest(request.getId(),
                httpRequestEntitySaveRequest, new ArrayList<>(), Optional.of(fileBody));

        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(repository.get(), times(1)).save(requestArgumentCaptor.capture());
        HttpRequest result = (HttpRequest) requestArgumentCaptor.getValue();
        assertEquals(fileBody.getFileName(), result.getBody().getBinaryBody().getFileName());
        assertEquals(fileBody.getFileId(), result.getBody().getBinaryBody().getFileId());
    }

    @Test()
    public void saveRequest_whenRequestNotFoundInDatabase_shouldThrowEntityNotFoundException() {
        // given
        String newName = "New request name";
        HttpRequest httpRequest = generateRandomHttpRequest();
        HttpRequestEntitySaveRequest httpRequestEntitySaveRequest = generateHttpRequestSaveFromHttpRequest(httpRequest);
        httpRequestEntitySaveRequest.setName(newName);
        // when
        when(repository.get().findById(any())).thenReturn(Optional.empty());
        // then
        assertThrows(AtpEntityNotFoundException.class, () -> requestService.get().saveRequest(httpRequest.getId(),
                httpRequestEntitySaveRequest, new ArrayList<>(), Optional.empty()));
    }

    @Test
    public void saveRequest_withInheritAuthorizationType_parentFolderAuthorizationIsNull_thenAuthFolderIdIsNull() {
        // given
        HttpRequest r = generateHttpRequest("test", null);
        InheritFromParentRequestAuthorization authRequest = new InheritFromParentRequestAuthorization();
        authRequest.setType(RequestAuthorizationType.INHERIT_FROM_PARENT);
        r.setAuthorization(authRequest);
        UUID parentFolderId = UUID.randomUUID();
        r.setFolderId(parentFolderId);
        Folder parentFolder = generateFolder("test", null);
        HttpRequestEntitySaveRequest saveRequest = generateHttpRequestSaveFromHttpRequest(r);

        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(r));
        when(folderService.get().getFolder(eq(parentFolderId))).thenReturn(parentFolder);
        when(repository.get().save(any())).thenAnswer(args -> args.getArguments()[0]);
        Request savedRequest = requestService.get().saveRequest(UUID.randomUUID(), saveRequest, null, Optional.empty());

        // then
        assertNull(((InheritFromParentRequestAuthorization) savedRequest.getAuthorization())
                .getAuthorizationFolderId());
    }

    @Test
    public void saveRequest_withInheritAuthorizationType_parentFolderAuthorizationSpecified_thenAuthFolderIdNotNull() {
        // given
        HttpRequest r = generateHttpRequest("test", null);
        InheritFromParentRequestAuthorization authRequest = new InheritFromParentRequestAuthorization();
        authRequest.setType(RequestAuthorizationType.INHERIT_FROM_PARENT);
        r.setAuthorization(authRequest);
        UUID parentFolderId = UUID.randomUUID();
        r.setFolderId(parentFolderId);
        Folder parentFolder = generateFolder("test", null);
        BearerRequestAuthorization bearerAuth = new BearerRequestAuthorization();
        bearerAuth.setType(RequestAuthorizationType.BEARER);
        bearerAuth.setToken("token");
        parentFolder.setAuthorization(bearerAuth);
        HttpRequestEntitySaveRequest saveRequest = generateHttpRequestSaveFromHttpRequest(r);

        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(r));
        doCallRealMethod().when(folderService.get()).updateAuthorizationFolderId(any(Request.class));
        doCallRealMethod().when(folderService.get())
                .updateAuthorizationFolderId(any(RequestAuthorization.class), any(UUID.class));
        when(folderService.get().getFolder(eq(parentFolderId))).thenReturn(parentFolder);
        when(repository.get().save(any())).thenAnswer(args -> args.getArguments()[0]);
        Request savedRequest = requestService.get().saveRequest(UUID.randomUUID(), saveRequest, null, Optional.empty());

        // then
        assertEquals(parentFolderId, ((InheritFromParentRequestAuthorization) savedRequest.getAuthorization())
                .getAuthorizationFolderId());
    }

    @Test
    public void editHttpRequest_whenAllEditRequestSpecified_shouldEditRequestBySaveMethod() {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        RequestEntityEditRequest requestEntityEditRequest = generateRequestEditFromRequest(httpRequest);
        String newName = "New request name";
        requestEntityEditRequest.setName(newName);
        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(httpRequest));
        when(repository.get().save(any(Request.class))).thenAnswer(answer -> answer.getArguments()[0]);
        HttpRequest actualRequest = (HttpRequest) requestService.get().editRequest(httpRequest.getId(), requestEntityEditRequest);
        // then
        verify(repository.get()).save(any());
        assertEquals(httpRequest.getId(), actualRequest.getId());
        assertEquals(httpRequest.getProjectId(), actualRequest.getProjectId());
        assertEquals(httpRequest.getFolderId(), actualRequest.getFolderId());
        assertEquals(httpRequest.getTransportType(), actualRequest.getTransportType());
        assertEquals(httpRequest.getUrl(), actualRequest.getUrl());
        assertEquals(httpRequest.getHttpMethod(), actualRequest.getHttpMethod());
        assertEquals(httpRequest.getBody(), actualRequest.getBody());
        assertEquals(httpRequest.getRequestHeaders(), actualRequest.getRequestHeaders());
        assertEquals(httpRequest.getRequestParams(), actualRequest.getRequestParams());
        assertEquals(newName, actualRequest.getName());
    }

    @Test()
    public void editRequest_whenRequestNotFoundInDatabase_shouldThrowEntityNotFoundException() {
        // given
        String newName = "New request name";
        HttpRequest httpRequest = generateRandomHttpRequest();
        RequestEntityEditRequest requestEntityEditRequest = generateRequestEditFromRequest(httpRequest);
        requestEntityEditRequest.setName(newName);
        // when
        when(repository.get().findById(any())).thenReturn(Optional.empty());
        // then
        assertThrows(AtpEntityNotFoundException.class, () -> requestService.get().editRequest(httpRequest.getId(),
                requestEntityEditRequest));
    }

    @Test()
    public void copyRequest_whenRequestNotFoundInDatabase_shouldThrowEntityNotFoundExceptionException() {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        RequestEntityCopyRequest requestEntityCopyRequest = generateRequestEntityCopyRequestFromRequest(httpRequest);
        // when
        when(repository.get().findById(any())).thenReturn(Optional.empty());
        // then
        assertThrows(AtpEntityNotFoundException.class, () -> requestService.get().copyRequest(httpRequest.getId(),
                requestEntityCopyRequest));
    }

    @Test
    public void copyRequest_whenAllCopyRequestSpecifiedAndNotFoundInFolder_shouldCopyRequestBySaveMethod() {
        // given
        final UUID folderId = UUID.randomUUID();
        final UUID projectId = UUID.randomUUID();
        HttpRequest originHttpRequest = generateRandomHttpRequest();
        originHttpRequest.setAuthorization(generateRandomAuthorization());
        RequestEntityCopyRequest requestEntityCopyRequest = generateRequestEntityCopyRequestFromRequest(originHttpRequest);
        requestEntityCopyRequest.setFolderId(folderId);
        Folder targetFolder = generateFolder(folderId, "targetFolder", projectId, folderId, 0, "");
        targetFolder.setPermissionFolderId(folderId);

        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(originHttpRequest));
        when(repository.get().findAllByFolderId(any())).thenReturn(new ArrayList<>());
        when(repository.get().save(any(Request.class))).thenAnswer(answer -> answer.getArguments()[0]);
        when(folderService.get().getFolder(folderId)).thenReturn(targetFolder);
        HttpRequest actualRequest = (HttpRequest) requestService.get().copyRequest(originHttpRequest.getId(), requestEntityCopyRequest);
        // then
        verify(repository.get()).save(any());
        assertNotEquals(originHttpRequest.getId(), actualRequest.getId());
        assertEquals(folderId, actualRequest.getFolderId());
        assertEquals(originHttpRequest.getName(), actualRequest.getName());
        assertEquals(originHttpRequest.getTransportType(), actualRequest.getTransportType());
        assertEquals(originHttpRequest.getProjectId(), actualRequest.getProjectId());
        assertEquals(originHttpRequest.getUrl(), actualRequest.getUrl());
        assertEquals(originHttpRequest.getHttpMethod(), actualRequest.getHttpMethod());
        assertEquals(originHttpRequest.getBody(), actualRequest.getBody());
        assertEquals(originHttpRequest.getRequestHeaders(), actualRequest.getRequestHeaders());
        assertEquals(originHttpRequest.getRequestParams(), actualRequest.getRequestParams());
        assertNotNull(actualRequest.getAuthorization());
        assertNull(actualRequest.getAuthorization().getId());
        assertNotEquals(originHttpRequest.getAuthorization(), actualRequest.getAuthorization());
        assertThat(actualRequest.getAuthorization())
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(originHttpRequest.getAuthorization());
        assertEquals(targetFolder.getPermissionFolderId(), actualRequest.getPermissionFolderId());
    }

    @Test
    public void copyRequest_whenRequestWithBearerAuthorization_shouldCopyRequestBySaveMethod() {
        // given
        final UUID folderId = UUID.randomUUID();
        HttpRequest originHttpRequest = generateRandomHttpRequest();
        originHttpRequest.setAuthorization(generateRandomBearerAuthorization());
        RequestEntityCopyRequest requestEntityCopyRequest = generateRequestEntityCopyRequestFromRequest(originHttpRequest);
        requestEntityCopyRequest.setFolderId(folderId);
        Folder targetFolder = generateFolder(folderId, "targetFolder", UUID.randomUUID(), folderId, 0, "");
        targetFolder.setPermissionFolderId(folderId);
        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(originHttpRequest));
        when(repository.get().findAllByFolderId(any())).thenReturn(new ArrayList<>());
        when(repository.get().save(any(Request.class))).thenAnswer(answer -> answer.getArguments()[0]);
        when(folderService.get().getFolder(folderId)).thenReturn(targetFolder);
        HttpRequest actualRequest = (HttpRequest) requestService.get().copyRequest(originHttpRequest.getId(), requestEntityCopyRequest);
        // then
        verify(repository.get()).save(any());
        assertNotEquals(originHttpRequest.getId(), actualRequest.getId());
        assertEquals(folderId, actualRequest.getFolderId());
        assertEquals(originHttpRequest.getName(), actualRequest.getName());
        assertEquals(originHttpRequest.getTransportType(), actualRequest.getTransportType());
        assertEquals(originHttpRequest.getProjectId(), actualRequest.getProjectId());
        assertEquals(originHttpRequest.getUrl(), actualRequest.getUrl());
        assertEquals(originHttpRequest.getHttpMethod(), actualRequest.getHttpMethod());
        assertEquals(originHttpRequest.getBody(), actualRequest.getBody());
        assertEquals(originHttpRequest.getRequestHeaders(), actualRequest.getRequestHeaders());
        assertEquals(originHttpRequest.getRequestParams(), actualRequest.getRequestParams());
        assertNotNull(actualRequest.getAuthorization());
        assertNull(actualRequest.getAuthorization().getId());
        assertNotEquals(originHttpRequest.getAuthorization(), actualRequest.getAuthorization());
        assertThat(actualRequest.getAuthorization())
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(originHttpRequest.getAuthorization());
    }

    @Test
    public void copyRequest_whenAllCopyRequestSpecifiedAndFoundInFolder_shouldCopyRequestBySaveMethod() {
        // given
        final UUID folderId = UUID.randomUUID();
        HttpRequest httpRequest = generateRandomHttpRequest();
        RequestEntityCopyRequest requestEntityCopyRequest = generateRequestEntityCopyRequestFromRequest(httpRequest);
        requestEntityCopyRequest.setFolderId(folderId);
        Folder targetFolder = generateFolder(folderId, "targetFolder", UUID.randomUUID(), folderId, 0, "");
        targetFolder.setPermissionFolderId(folderId);

        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(httpRequest));
        when(repository.get().findAllByProjectIdAndFolderId(any(), any())).thenReturn(singletonList(httpRequest));
        when(repository.get().save(any(Request.class))).thenAnswer(answer -> answer.getArguments()[0]);
        when(folderService.get().getFolder(folderId)).thenReturn(targetFolder);
        HttpRequest actualRequest = (HttpRequest) requestService.get().copyRequest(httpRequest.getId(), requestEntityCopyRequest);
        // then
        verify(repository.get()).save(any());
        assertNotEquals(httpRequest.getId(), actualRequest.getId());
        assertEquals(folderId, actualRequest.getFolderId());
        assertEquals(httpRequest.getName() + COPY_POSTFIX, actualRequest.getName());
        assertEquals(httpRequest.getTransportType(), actualRequest.getTransportType());
        assertEquals(httpRequest.getProjectId(), actualRequest.getProjectId());
        assertEquals(httpRequest.getUrl(), actualRequest.getUrl());
        assertEquals(httpRequest.getHttpMethod(), actualRequest.getHttpMethod());
        assertEquals(httpRequest.getBody(), actualRequest.getBody());
        assertEquals(httpRequest.getRequestHeaders(), actualRequest.getRequestHeaders());
        assertEquals(httpRequest.getRequestParams(), actualRequest.getRequestParams());
        assertEquals(targetFolder.getPermissionFolderId(), actualRequest.getPermissionFolderId());
    }

    @Test
    public void copyHttpRequest_whenRequestAfterPostfixAdditionSpecifiedAndFoundInFolder_shouldCopyRequestBySaveMethod() {
        // given
        final UUID folderId = UUID.randomUUID();
        HttpRequest httpRequest = generateRandomHttpRequest();
        RequestEntityCopyRequest requestEntityCopyRequest = generateRequestEntityCopyRequestFromRequest(httpRequest);
        requestEntityCopyRequest.setFolderId(folderId);
        Request requestWithPostfix = generateRandomHttpRequest();
        requestWithPostfix.setName(requestWithPostfix.getName() + COPY_POSTFIX);
        List<Request> folderRequests = asList(httpRequest, requestWithPostfix);
        Folder targetFolder = generateFolder(folderId, "targetFolder", UUID.randomUUID(), folderId, 0, "");
        targetFolder.setPermissionFolderId(folderId);

        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(httpRequest));
        when(repository.get().findAllByProjectIdAndFolderId(any(), any())).thenReturn(folderRequests);
        when(repository.get().save(any(Request.class))).thenAnswer(answer -> answer.getArguments()[0]);
        when(folderService.get().getFolder(folderId)).thenReturn(targetFolder);
        HttpRequest actualRequest = (HttpRequest) requestService.get().copyRequest(httpRequest.getId(), requestEntityCopyRequest);
        // then
        verify(repository.get()).save(any());
        assertNotEquals(httpRequest.getId(), actualRequest.getId());
        assertEquals(folderId, actualRequest.getFolderId());
        assertEquals(httpRequest.getName() + COPY_POSTFIX + COPY_POSTFIX, actualRequest.getName());
        assertEquals(httpRequest.getTransportType(), actualRequest.getTransportType());
        assertEquals(httpRequest.getProjectId(), actualRequest.getProjectId());
        assertEquals(httpRequest.getUrl(), actualRequest.getUrl());
        assertEquals(httpRequest.getHttpMethod(), actualRequest.getHttpMethod());
        assertEquals(httpRequest.getBody(), actualRequest.getBody());
        assertEquals(httpRequest.getRequestHeaders(), actualRequest.getRequestHeaders());
        assertEquals(httpRequest.getRequestParams(), actualRequest.getRequestParams());
        assertEquals(targetFolder.getPermissionFolderId(), actualRequest.getPermissionFolderId());
    }

    @Test
    public void copyRequest_whenRequestHasNullFolderId_shouldCopyRequestBySaveMethod() {
        // given
        HttpRequest originHttpRequest = generateRandomHttpRequest();
        originHttpRequest.setAuthorization(generateRandomBearerAuthorization());
        RequestEntityCopyRequest requestEntityCopyRequest = generateRequestEntityCopyRequestFromRequest(originHttpRequest);
        requestEntityCopyRequest.setFolderId(null);
        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(originHttpRequest));
        when(repository.get().findAllByFolderId(any())).thenReturn(new ArrayList<>());
        when(repository.get().save(any(Request.class))).thenAnswer(answer -> answer.getArguments()[0]);
        HttpRequest actualRequest = (HttpRequest) requestService.get().copyRequest(originHttpRequest.getId(), requestEntityCopyRequest);
        // then
        verify(repository.get()).save(any());
        assertNotEquals(originHttpRequest.getId(), actualRequest.getId());
        assertEquals(originHttpRequest.getName(), actualRequest.getName());
        assertEquals(originHttpRequest.getTransportType(), actualRequest.getTransportType());
        assertEquals(originHttpRequest.getProjectId(), actualRequest.getProjectId());
        assertEquals(originHttpRequest.getUrl(), actualRequest.getUrl());
        assertEquals(originHttpRequest.getHttpMethod(), actualRequest.getHttpMethod());
        assertEquals(originHttpRequest.getBody(), actualRequest.getBody());
        assertEquals(originHttpRequest.getRequestHeaders(), actualRequest.getRequestHeaders());
        assertEquals(originHttpRequest.getRequestParams(), actualRequest.getRequestParams());
        assertNull(actualRequest.getPermissionFolderId());
        assertNull(actualRequest.getFolderId());
    }

    @Test()
    public void copyRequests_whenOneOfRequestsNotFoundInDatabase_responseErrorResultShouldContainNotFoundRequest() {
        // given
        final UUID folderId = UUID.randomUUID();
        UUID notFoundRequestId = UUID.randomUUID();
        RequestEntitiesCopyRequest requestEntitiesCopyRequest = generateRequestEntitiesCopyRequest();
        HttpRequest httpRequest = generateRandomHttpRequest();
        requestEntitiesCopyRequest.setRequestIds(Sets.newHashSet(httpRequest.getId(), notFoundRequestId));
        requestEntitiesCopyRequest.setFolderId(folderId);
        Folder targetFolder = generateFolder(folderId, "targetFolder", UUID.randomUUID(), folderId, 0, "");
        targetFolder.setPermissionFolderId(folderId);

        // when
        when(folderService.get().getFolder(folderId)).thenReturn(targetFolder);
        when(repository.get().findAllByProjectIdAndIdIn(any(), any())).thenReturn(singletonList(httpRequest));
        requestService.get().copyRequests(requestEntitiesCopyRequest);
        // then
        verify(repository.get()).saveAll(any());
    }

    @Test()
    public void copyRequests_whenRequestHasNullFolderId_ActualRequestShoiuldHaveNullInFolderIds() {
        // given
        UUID notFoundRequestId = UUID.randomUUID();
        RequestEntitiesCopyRequest requestEntitiesCopyRequest = generateRequestEntitiesCopyRequest();
        HttpRequest httpRequest = generateRandomHttpRequest();
        requestEntitiesCopyRequest.setRequestIds(Sets.newHashSet(httpRequest.getId(), notFoundRequestId));
        requestEntitiesCopyRequest.setFolderId(null);

        when(repository.get().findAllByProjectIdAndIdIn(any(), any())).thenReturn(singletonList(httpRequest));
        requestService.get().copyRequests(requestEntitiesCopyRequest);

        // then
        verify(repository.get()).saveAll(any());
        verify(folderService.get(), times(0)).get(any());
    }

    @Test
    public void copyRequests_whenAllCopyRequestsSpecifiedAndNotFoundInFolder_shouldCopyRequestsBySaveAllMethod() {
        // given
        final UUID folderId = UUID.randomUUID();
        HttpRequest request1 = generateRandomHttpRequest();
        HttpRequest oldRequest1 = new HttpRequest();
        modelMapper.map(request1, oldRequest1);
        HttpRequest request2 = generateRandomHttpRequest();
        HttpRequest oldRequest2 = new HttpRequest();
        modelMapper.map(request2, oldRequest2);
        RequestEntitiesCopyRequest requestEntitiesCopyRequest = generateRequestEntitiesCopyRequest();
        requestEntitiesCopyRequest.setRequestIds(Sets.newHashSet(request1.getId(), request2.getId()));
        requestEntitiesCopyRequest.setFolderId(folderId);
        Folder targetFolder = generateFolder(folderId, "targetFolder", UUID.randomUUID(), folderId, 0, "");
        targetFolder.setPermissionFolderId(folderId);

        // when
        when(repository.get().findAllByProjectIdAndIdIn(any(), any())).thenReturn(Arrays.asList(request1, request2));
        when(repository.get().findAllByFolderId(any())).thenReturn(new ArrayList<>());
        when(folderService.get().getFolder(folderId)).thenReturn(targetFolder);
        requestService.get().copyRequests(requestEntitiesCopyRequest);
        // then
        ArgumentCaptor<List<Request>> captureRequests = ArgumentCaptor.forClass(ArrayList.class);
        verify(repository.get()).saveAll(captureRequests.capture());
        List<Request> copyRequests = captureRequests.getValue();
        assertEquals(2, copyRequests.size());
        HttpRequest actualRequest1 = (HttpRequest) copyRequests.get(0);
        HttpRequest actualRequest2 = (HttpRequest) copyRequests.get(1);
        assertNotEquals(oldRequest1.getId(), actualRequest1.getId());
        assertNotEquals(oldRequest2.getId(), actualRequest2.getId());
        assertEquals(oldRequest1.getName(), actualRequest1.getName());
        assertEquals(oldRequest2.getName(), actualRequest2.getName());
        assertEquals(oldRequest1.getTransportType(), actualRequest1.getTransportType());
        assertEquals(oldRequest2.getTransportType(), actualRequest2.getTransportType());
        assertEquals(folderId, actualRequest1.getFolderId());
        assertEquals(folderId, actualRequest2.getFolderId());
        assertEquals(oldRequest1.getProjectId(), actualRequest1.getProjectId());
        assertEquals(oldRequest2.getProjectId(), actualRequest2.getProjectId());
        assertEquals(oldRequest1.getUrl(), actualRequest1.getUrl());
        assertEquals(oldRequest2.getUrl(), actualRequest2.getUrl());
        assertEquals(oldRequest1.getHttpMethod(), actualRequest1.getHttpMethod());
        assertEquals(oldRequest2.getHttpMethod(), actualRequest2.getHttpMethod());
        assertEquals(oldRequest1.getBody(), actualRequest1.getBody());
        assertEquals(oldRequest2.getBody(), actualRequest2.getBody());
        assertEquals(oldRequest1.getRequestHeaders().size(), actualRequest1.getRequestHeaders().size());
        assertEquals(oldRequest2.getRequestHeaders().size(), actualRequest2.getRequestHeaders().size());
        assertEquals(oldRequest1.getRequestParams().size(), actualRequest1.getRequestParams().size());
        assertEquals(oldRequest2.getRequestParams().size(), actualRequest2.getRequestParams().size());
        assertEquals(targetFolder.getPermissionFolderId(), actualRequest1.getPermissionFolderId());
        assertEquals(targetFolder.getPermissionFolderId(), actualRequest2.getPermissionFolderId());
    }

    @Test
    public void copyRequests_whenRequestWithBearerAuthorization_shouldCopyRequestsBySaveAllMethod() {
        // given
        final UUID folderId = UUID.randomUUID();
        HttpRequest request1 = generateRandomHttpRequest();
        request1.setAuthorization(generateRandomBearerAuthorization());
        HttpRequest oldRequest1 = new HttpRequest();
        modelMapper.map(request1, oldRequest1);
        HttpRequest request2 = generateRandomHttpRequest();
        HttpRequest oldRequest2 = new HttpRequest();
        modelMapper.map(request2, oldRequest2);
        RequestEntitiesCopyRequest requestEntitiesCopyRequest = generateRequestEntitiesCopyRequest();
        requestEntitiesCopyRequest.setRequestIds(Sets.newHashSet(request1.getId(), request2.getId()));
        requestEntitiesCopyRequest.setFolderId(folderId);
        Folder targetFolder = generateFolder(folderId, "targetFolder", UUID.randomUUID(), folderId, 0, "");
        targetFolder.setPermissionFolderId(folderId);

        // when
        when(repository.get().findAllByProjectIdAndIdIn(any(), any())).thenReturn(Arrays.asList(request1, request2));
        when(repository.get().findAllByFolderId(any())).thenReturn(new ArrayList<>());
        when(folderService.get().getFolder(folderId)).thenReturn(targetFolder);
        requestService.get().copyRequests(requestEntitiesCopyRequest);
        // then
        ArgumentCaptor<List<Request>> captureRequests = ArgumentCaptor.forClass(ArrayList.class);
        verify(repository.get()).saveAll(captureRequests.capture());
        List<Request> copyRequests = captureRequests.getValue();
        assertEquals(2, copyRequests.size());
        HttpRequest actualRequest1 = (HttpRequest) copyRequests.get(0);
        HttpRequest actualRequest2 = (HttpRequest) copyRequests.get(1);
        assertNotEquals(oldRequest1.getId(), actualRequest1.getId());
        assertNotEquals(oldRequest2.getId(), actualRequest2.getId());
        assertEquals(oldRequest1.getName(), actualRequest1.getName());
        assertEquals(oldRequest2.getName(), actualRequest2.getName());
        assertEquals(oldRequest1.getTransportType(), actualRequest1.getTransportType());
        assertEquals(oldRequest2.getTransportType(), actualRequest2.getTransportType());
        assertEquals(folderId, actualRequest1.getFolderId());
        assertEquals(folderId, actualRequest2.getFolderId());
        assertEquals(oldRequest1.getProjectId(), actualRequest1.getProjectId());
        assertEquals(oldRequest2.getProjectId(), actualRequest2.getProjectId());
        assertEquals(oldRequest1.getUrl(), actualRequest1.getUrl());
        assertEquals(oldRequest2.getUrl(), actualRequest2.getUrl());
        assertEquals(oldRequest1.getHttpMethod(), actualRequest1.getHttpMethod());
        assertEquals(oldRequest2.getHttpMethod(), actualRequest2.getHttpMethod());
        assertEquals(oldRequest1.getBody(), actualRequest1.getBody());
        assertEquals(oldRequest2.getBody(), actualRequest2.getBody());
        assertEquals(oldRequest1.getRequestHeaders().size(), actualRequest1.getRequestHeaders().size());
        assertEquals(oldRequest2.getRequestHeaders().size(), actualRequest2.getRequestHeaders().size());
        assertEquals(oldRequest1.getRequestParams().size(), actualRequest1.getRequestParams().size());
        assertEquals(oldRequest2.getRequestParams().size(), actualRequest2.getRequestParams().size());
        assertThat(oldRequest1.getAuthorization())
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(actualRequest1.getAuthorization());
    }

    @Test
    public void copyRequests_whenRequestAfterPostfixAdditionSpecifiedAndFoundInFolder_shouldCopyRequestsBySaveAllMethod() throws IOException {
        // given
        final UUID folderId = UUID.randomUUID();
        HttpRequest request1 = generateHttpRequestWithName("Request1");
        HttpRequest oldRequest1 = new HttpRequest();
        modelMapper.map(request1, oldRequest1);
        RequestEntitiesCopyRequest requestEntitiesCopyRequest = generateRequestEntitiesCopyRequest();
        requestEntitiesCopyRequest.setRequestIds(Sets.newHashSet(request1.getId()));
        requestEntitiesCopyRequest.setFolderId(folderId);
        HttpRequest folderRequestWithPostfix = new HttpRequest();
        modelMapper.map(request1, folderRequestWithPostfix);
        folderRequestWithPostfix.setId(UUID.randomUUID());
        folderRequestWithPostfix.setName(folderRequestWithPostfix.getName() + COPY_POSTFIX);
        HttpRequest folderRequest1 = new HttpRequest();
        modelMapper.map(request1, folderRequest1);
        List<Request> folderRequests = Arrays.asList(folderRequest1, folderRequestWithPostfix);
        byte[] zippedBytes = generateZipArchiveFromBytes("test.txt", "test".getBytes(StandardCharsets.UTF_8));
        FileData dictionaryFileData = new FileData(zippedBytes, "test.txt");
        Folder targetFolder = generateFolder(folderId, "targetFolder", UUID.randomUUID(), folderId, 0, "");
        targetFolder.setPermissionFolderId(folderId);

        // when
        when(repository.get().findAllByProjectIdAndIdIn(any(), any())).thenReturn(Arrays.asList(request1));
        when(repository.get().findAllByProjectIdAndFolderId(any(), any())).thenReturn(folderRequests);
        when(gridFsService.get().downloadFile(any())).thenReturn(Optional.of(dictionaryFileData));
        when(folderService.get().getFolder(folderId)).thenReturn(targetFolder);
        requestService.get().copyRequests(requestEntitiesCopyRequest);
        // then
        ArgumentCaptor<List<Request>> captureRequests = ArgumentCaptor.forClass(ArrayList.class);
        ArgumentCaptor<Request> captureRequest = ArgumentCaptor.forClass(Request.class);
        verify(repository.get()).saveAll(captureRequests.capture());

        List<Request> copyRequests = captureRequests.getValue();
        assertEquals(1, copyRequests.size());
        HttpRequest actualRequest1 = (HttpRequest) copyRequests.get(0);
        assertNotEquals(oldRequest1.getId(), actualRequest1.getId());
        assertEquals(oldRequest1.getName() + COPY_POSTFIX + COPY_POSTFIX, actualRequest1.getName());
        assertEquals(oldRequest1.getTransportType(), actualRequest1.getTransportType());
        assertEquals(folderId, actualRequest1.getFolderId());
        assertEquals(oldRequest1.getProjectId(), actualRequest1.getProjectId());
        assertEquals(oldRequest1.getUrl(), actualRequest1.getUrl());
        assertEquals(oldRequest1.getHttpMethod(), actualRequest1.getHttpMethod());
        assertEquals(oldRequest1.getBody(), actualRequest1.getBody());
        assertEquals(oldRequest1.getRequestHeaders().size(), actualRequest1.getRequestHeaders().size());
        assertEquals(oldRequest1.getRequestParams().size(), actualRequest1.getRequestParams().size());
        assertEquals(targetFolder.getPermissionFolderId(), actualRequest1.getPermissionFolderId());
    }

    @Test
    public void copyRequests_httpRequestWithBinaryFile_fileShouldBeSavedAndRequestBodyShouldBeUpdated() throws IOException {
        final UUID requestId = UUID.randomUUID();
        final UUID projectId = UUID.randomUUID();
        final UUID folderId = UUID.randomUUID();
        RequestEntitiesCopyRequest baseRequest =
                new RequestEntitiesCopyRequest(projectId, Collections.singleton(requestId), folderId);
        HttpRequest request = generateHttpRequest("request", projectId, folderId);
        request.setTransportType(TransportType.REST);
        request.setBody(new RequestBody(new FileBody(), RequestBodyType.Binary));
        request.setRequestHeaders(new ArrayList<>());
        request.setRequestParams(new ArrayList<>());
        when(repository.get().findAllByProjectIdAndIdIn(any(), any())).thenReturn(Collections.singletonList(request));
        when(repository.get().findAllByFolderId(any())).thenReturn(Collections.singletonList(request));
        when(folderService.get().getFolder(any())).thenReturn(generateFolder(folderId.toString(), projectId));
        FileBody fileInfo = new FileBody("name", UUID.randomUUID());
        doReturn(Optional.of(fileInfo)).when(fileService.get()).copyFileForCopiedRequest(any(), any(), any(), any());
        request.setId(UUID.randomUUID());
        when(repository.get().save(any())).thenReturn(request);

        requestService.get().copyRequests(baseRequest);

        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(repository.get(), times(2)).save(requestArgumentCaptor.capture());

        Request secondSavedRequest = requestArgumentCaptor.getAllValues().get(1);
        RequestBody body = ((HttpRequest) secondSavedRequest).getBody();
        assertEquals(RequestBodyType.Binary, body.getType());
        assertEquals(fileInfo.getFileName(), body.getBinaryBody().getFileName());
        assertEquals(fileInfo.getFileId(), body.getBinaryBody().getFileId());
    }

    @Test
    public void copyRequest_httpRequestWithBinaryFile_fileShouldBeSavedAndRequestBodyShouldBeUpdated() throws IOException {
        final UUID requestId = UUID.randomUUID();
        final UUID projectId = UUID.randomUUID();
        final UUID folderId = UUID.randomUUID();
        RequestEntityCopyRequest baseRequest =
                new RequestEntityCopyRequest(projectId, folderId);
        HttpRequest request = generateHttpRequest("request", projectId, folderId);
        request.setTransportType(TransportType.REST);
        request.setBody(new RequestBody(new FileBody(), RequestBodyType.Binary));
        request.setRequestHeaders(new ArrayList<>());
        request.setRequestParams(new ArrayList<>());
        when(repository.get().findById(any())).thenReturn(Optional.of(request));
        when(repository.get().findAllByFolderId(any())).thenReturn(Collections.singletonList(request));
        when(folderService.get().getFolder(any())).thenReturn(generateFolder(folderId.toString(), projectId));
        FileBody fileInfo = new FileBody("name", UUID.randomUUID());
        doReturn(Optional.of(fileInfo)).when(fileService.get()).copyFileForCopiedRequest(any(), any(), any(), any());
        request.setId(UUID.randomUUID());
        when(repository.get().save(any())).thenReturn(request);

        requestService.get().copyRequest(requestId, baseRequest);

        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(repository.get(), times(2)).save(requestArgumentCaptor.capture());

        Request secondSavedRequest = requestArgumentCaptor.getAllValues().get(1);
        RequestBody body = ((HttpRequest) secondSavedRequest).getBody();
        assertEquals(RequestBodyType.Binary, body.getType());
        assertEquals(fileInfo.getFileName(), body.getBinaryBody().getFileName());
        assertEquals(fileInfo.getFileId(), body.getBinaryBody().getFileId());
    }

    @Test
    public void uploadBinaryFile_restWithMultipartFile_updateFileInfoFOrRequest() throws IOException {
        final UUID requestId = UUID.randomUUID();
        final UUID projectId = UUID.randomUUID();
        final UUID folderId = UUID.randomUUID();
        HttpRequest request = generateHttpRequest("request", projectId, folderId);
        request.setTransportType(TransportType.REST);
        request.setBody(new RequestBody(new FileBody(), RequestBodyType.Binary));
        MockMultipartFile file = new MockMultipartFile("name.json", "content".getBytes());

        when(repository.get().findById(any())).thenReturn(Optional.of(request));
        FileBody fileBody = new FileBody("name", UUID.randomUUID());
        doReturn(fileBody).when(fileService.get()).uploadFileForRequest(any(), any(), any());

        requestService.get().uploadBinaryFile(requestId, file);

        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(repository.get(), times(1)).save(requestArgumentCaptor.capture());
        HttpRequest result = (HttpRequest) requestArgumentCaptor.getValue();
        assertEquals(fileBody.getFileName(), result.getBody().getBinaryBody().getFileName());
        assertEquals(fileBody.getFileId(), result.getBody().getBinaryBody().getFileId());
    }

    @Test
    public void uploadBinaryFile_soapWithMultipartFile_updateFileInfoFOrRequest() throws IOException {
        final UUID requestId = UUID.randomUUID();
        final UUID projectId = UUID.randomUUID();
        final UUID folderId = UUID.randomUUID();
        HttpRequest request = generateHttpRequest("request", projectId, folderId);
        request.setTransportType(TransportType.SOAP);
        request.setBody(new RequestBody(new FileBody(), RequestBodyType.Binary));
        MockMultipartFile file = new MockMultipartFile("name.json", "content".getBytes());

        when(repository.get().findById(any())).thenReturn(Optional.of(request));
        FileBody fileBody = new FileBody("name", UUID.randomUUID());
        doReturn(fileBody).when(fileService.get()).uploadFileForRequest(any(), any(), any());

        requestService.get().uploadBinaryFile(requestId, file);

        ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(repository.get(), times(1)).save(requestArgumentCaptor.capture());
        HttpRequest result = (HttpRequest) requestArgumentCaptor.getValue();
        assertEquals(fileBody.getFileName(), result.getBody().getBinaryBody().getFileName());
        assertEquals(fileBody.getFileId(), result.getBody().getBinaryBody().getFileId());
    }

    @Test()
    public void moveRequest_whenRequestNotFoundInDatabase_shouldThrowEntityNotFoundException() {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        RequestEntityMoveRequest requestEntityMoveRequest = generateRequestEntityMoveRequestFromRequest(httpRequest);
        // when
        when(repository.get().findById(any())).thenReturn(Optional.empty());
        // then
        assertThrows(AtpEntityNotFoundException.class, () -> requestService.get().moveRequest(httpRequest.getId(),
                requestEntityMoveRequest));
    }

    @Test
    public void moveHttpRequest_whenAllMoveRequestSpecified_shouldMoveRequestBySaveMethod() {
        // given
        final UUID folderId = UUID.randomUUID();
        HttpRequest httpRequest = generateRandomHttpRequest();
        RequestEntityMoveRequest requestEntityMoveRequest = generateRequestEntityMoveRequestFromRequest(httpRequest);
        requestEntityMoveRequest.setFolderId(folderId);
        Folder targetFolder = generateFolder(folderId, "targetFolder", UUID.randomUUID(), folderId, 0, "");
        targetFolder.setPermissionFolderId(folderId);

        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(httpRequest));
        when(repository.get().save(any(Request.class))).thenAnswer(answer -> answer.getArguments()[0]);
        when(folderService.get().getFolder(folderId)).thenReturn(targetFolder);
        HttpRequest actualRequest = (HttpRequest) requestService.get().moveRequest(httpRequest.getId(), requestEntityMoveRequest);
        // then
        verify(repository.get()).save(any());
        assertEquals(httpRequest.getId(), actualRequest.getId());
        assertEquals(folderId, actualRequest.getFolderId());
        assertEquals(httpRequest.getName(), actualRequest.getName());
        assertEquals(httpRequest.getTransportType(), actualRequest.getTransportType());
        assertEquals(httpRequest.getProjectId(), actualRequest.getProjectId());
        assertEquals(httpRequest.getUrl(), actualRequest.getUrl());
        assertEquals(httpRequest.getHttpMethod(), actualRequest.getHttpMethod());
        assertEquals(httpRequest.getBody(), actualRequest.getBody());
        assertEquals(httpRequest.getRequestHeaders(), actualRequest.getRequestHeaders());
        assertEquals(httpRequest.getRequestParams(), actualRequest.getRequestParams());
        assertEquals(targetFolder.getPermissionFolderId(), actualRequest.getPermissionFolderId());
    }

    @Test()
    public void moveRequests_whenOneOfRequestsNotFoundInDatabase_responseErrorResultShouldContainNotFoundRequest() {
        // given
        final UUID folderId = UUID.randomUUID();
        UUID notFoundRequestId = UUID.randomUUID();
        HttpRequest httpRequest = generateRandomHttpRequest();
        RequestEntitiesMoveRequest requestEntitiesMoveRequest = generateRequestEntitiesMoveRequest();
        requestEntitiesMoveRequest.setFolderId(folderId);
        requestEntitiesMoveRequest.setRequestIds(Sets.newHashSet(
                new IdWithModifiedWhen(httpRequest.getId(), null),
                new IdWithModifiedWhen(notFoundRequestId, null)));
        Folder targetFolder = generateFolder(folderId, "targetFolder", UUID.randomUUID(), folderId, 0, "");
        targetFolder.setPermissionFolderId(folderId);

        // when
        when(folderService.get().getFolder(folderId)).thenReturn(targetFolder);
        when(repository.get().findAllByProjectIdAndIdIn(any(), any())).thenReturn(singletonList(httpRequest));
        requestService.get().moveRequests(requestEntitiesMoveRequest);
        // then
        verify(repository.get()).saveAll(any());
    }

    @Test
    public void moveRequests_whenAllMoveRequestsSpecified_shouldMoveRequestsBySaveAllMethod() {
        // given
        final UUID folderId = UUID.randomUUID();
        Request request1 = generateRandomHttpRequest();
        RequestEntitiesMoveRequest requestEntitiesMoveRequest = generateRequestEntitiesMoveRequest();
        requestEntitiesMoveRequest.setRequestIds(Sets.newHashSet(
                new IdWithModifiedWhen(request1.getId(), null)));
        requestEntitiesMoveRequest.setFolderId(folderId);
        Folder targetFolder = generateFolder(folderId, "targetFolder", UUID.randomUUID(), folderId, 0, "");
        targetFolder.setPermissionFolderId(folderId);

        // when
        when(repository.get().findAllByProjectIdAndIdIn(any(), any())).thenReturn(Arrays.asList(request1));
        when(folderService.get().getFolder(folderId)).thenReturn(targetFolder);
        requestService.get().moveRequests(requestEntitiesMoveRequest);
        // then
        verify(repository.get()).saveAll(any());
        assertEquals(folderId, request1.getFolderId());
    }

    @Test
    public void deleteRequest_shouldThrowEntityNotFoundException() {
        // given
        final UUID requestId = UUID.randomUUID();

        // when
        when(repository.get().findById(any())).thenReturn(Optional.empty());
        // then
        assertThrows(AtpEntityNotFoundException.class, () -> requestService.get().deleteRequest(requestId));
    }

    @Test
    public void deleteRequest_shouldDeleteRequestByDeleteMethod() {
        // given
        final UUID requestId = UUID.randomUUID();
        HttpRequest httpRequest = generateRandomHttpRequest();

        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(httpRequest));
        requestService.get().deleteRequest(requestId);
        // then
        verify(repository.get()).delete(any());
    }

    @Test
    public void bulkDeleteRequests_listOfDeletedRequestIdsSpecified_shouldSuccessfullyDelete() {
        // given
        UUID notFoundRequestId = UUID.randomUUID();
        HttpRequest httpRequest = generateRandomHttpRequest();
        RequestEntitiesBulkDelete requestEntitiesBulkDelete = generateRequestEntitiesDeleteRequest(
                Sets.newHashSet(httpRequest.getId(), notFoundRequestId));
        GroupResponse groupResponse = new GroupResponse();
        List<Folder> list = new ArrayList<>();
        list.add(new Folder());

        // when
        when(repository.get().findAllById(any())).thenReturn(singletonList(httpRequest));
        when(folderService.get().getFoldersByIds(any())).thenReturn(list);
        when(folderService.get().getRequestTreeByParentFolderId(any())).thenReturn(groupResponse);
        when(folderService.get().getFolder(any())).thenReturn(new Folder());

        requestService.get().bulkDeleteRequests(requestEntitiesBulkDelete);

        // then
        verify(repository.get()).deleteAll(any());
    }

    @Test
    public void bulkDeleteRequests_shouldDeleteRequestsByDeleteAllMethod() {
        // given
        Request request1 = generateRandomHttpRequest();
        RequestEntitiesBulkDelete requestEntitiesBulkDelete = generateRequestEntitiesDeleteRequest(
                Sets.newHashSet(request1.getId())
        );
        requestEntitiesBulkDelete.setRequestIds(Sets.newHashSet(request1.getId()));
        GroupResponse groupResponse = new GroupResponse();

        List<Folder> list = new ArrayList<>();
        list.add(new Folder());

        // when
        when(repository.get().findAllById(any())).thenReturn(Arrays.asList(request1));
        when(folderService.get().getFoldersByIds(any())).thenReturn(list);
        when(folderService.get().getRequestTreeByParentFolderId(any())).thenReturn(groupResponse);
        when(folderService.get().getFolder(any())).thenReturn(new Folder());

        requestService.get().bulkDeleteRequests(requestEntitiesBulkDelete);
        // then
        verify(repository.get()).deleteAll(any());
    }

    @Test
    public void bulkDeleteRequests_successResult() {
        // given
        final UUID projectId = UUID.randomUUID();
        Request request1 = generateRandomHttpRequest();
        request1.setProjectId(projectId);
        RequestEntitiesBulkDelete requestEntitiesBulkDelete = generateRequestEntitiesDeleteRequest(
                Sets.newHashSet(request1.getId())
        );
        requestEntitiesBulkDelete.setProjectId(projectId);
        requestEntitiesBulkDelete.setRequestIds(Sets.newHashSet(request1.getId()));
        List<Folder> list = new ArrayList<>();
        list.add(EntitiesGenerator.generateFolder(UUID.fromString("2e22f779-f2a3-4977-9ba1-b47071c603e2"),
                "name", projectId, UUID.fromString("93353be2-94f6-48d2-aa28-dcea371554b0"), 0, ""));

        FolderDeleteRequest expectedFolderDeleteRequest = new FolderDeleteRequest();
        expectedFolderDeleteRequest.setProjectId(projectId);
        expectedFolderDeleteRequest.setIds(Collections.singleton(UUID.fromString("2e22f779-f2a3-4977-9ba1-b47071c603e2")));

        // when
        when(repository.get().findAllById(any())).thenReturn(Arrays.asList(request1));
        when(folderService.get().getFoldersByIds(any())).thenReturn(list);

        requestService.get().bulkDeleteRequests(requestEntitiesBulkDelete);

        // then
        ArgumentCaptor<FolderDeleteRequest> argumentCaptor = ArgumentCaptor.forClass(FolderDeleteRequest.class);
        verify(repository.get()).deleteAll(any());
        verify(folderService.get()).deleteFolders(argumentCaptor.capture());

        assertEquals(expectedFolderDeleteRequest, argumentCaptor.getValue());
    }

    @Test
    void exportRequest_postRequestWithVelocityTypeSpecified_shouldSuccessfullyConvert() throws URISyntaxException {
        // given
        final UUID requestId = UUID.randomUUID();
        String bodyString = "{\"id\":\"123\"}";
        String expectedResult = "curl -X POST -H \"Content-Type: application/json\" -H \"My-Header: my-value\" "
                + "-H \"My-Header: my-value\" -d '{\"id\":\"123\"}' 'http://test.test?name1=name1&name2=name2'";
        String contextId = "123";
        RequestBody body = new RequestBody(bodyString, RequestBodyType.Velocity);
        HttpRequest httpRequest = generateRandomHttpRequest();
        httpRequest.setBody(body);
        ItfParametersResolveResponse itfResponse = new ItfParametersResolveResponse(bodyString);
        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(httpRequest));
        when(itfFeignService.get().processVelocity(any(), any())).thenReturn(itfResponse);
        when(feignClientsProperties.get().getIsFeignAtpItfEnabled()).thenReturn(true);
        String actualResult = requestService.get().exportRequest(requestId, null, contextId, null);
        // then
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void importRequest_httpRequestFound_shouldCallConverter() {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        CurlStringImportRequest importRequest = new CurlStringImportRequest();
        importRequest.setRequestString("curl -X POST -H \"Content-Type: application/json\" -d '{\"id\": \"123\"}' " +
                "'http://test.test/?name=name'");
        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(httpRequest));
        requestService.get().importRequest(importRequest);

        // then
        verify(curlToRequestConverter.get()).convertCurlStringToRequest(eq(httpRequest), any());
    }

    @Test
    public void exportRequest_requestWithFormDataTypeSpecified_shouldSuccessfullyConvertWithEmptyBody() throws URISyntaxException {
        // given
        String expectedResult = "curl -X POST -H \"Content-Type: multipart/form-data\" -F 'test=test' 'http://test.test'";
        HttpRequest exportedRequest = generateRandomHttpRequest();
        exportedRequest.setHttpMethod(HttpMethod.POST);
        exportedRequest.setUrl("http://test.test");
        exportedRequest.setRequestParams(new ArrayList<>());
        exportedRequest.setRequestHeaders(new ArrayList<>());
        exportedRequest.getRequestHeaders().add(new RequestHeader("Content-Type", "multipart/form-data", "", false));
        RequestBody exportedBody = new RequestBody();
        exportedBody.setType(RequestBodyType.FORM_DATA);
        FormDataPart formDataPart = new FormDataPart();
        formDataPart.setKey("test");
        formDataPart.setValue("test");
        exportedBody.setFormDataBody(singletonList(formDataPart));
        exportedRequest.setBody(exportedBody);
        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(exportedRequest));
        String actualResult = requestService.get().exportRequest(exportedRequest.getId(), null, "", null);
        // then
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void importRequest_curlStringWithGraphQlJsonBodySpecified_shouldSuccessfullyConvert() {
        // given
        HttpRequest expectedRequest = generateRandomHttpRequest();
        expectedRequest.setHttpMethod(HttpMethod.POST);
        expectedRequest.setUrl("http://test.test/api/graphql-server/graphql");
        expectedRequest.setRequestParams(new ArrayList<>());
        expectedRequest.setRequestHeaders(new ArrayList<>());
        expectedRequest.getRequestHeaders().add(new RequestHeader("Content-Type", "application/json", "", false));
        expectedRequest.getRequestHeaders().add(new RequestHeader("Authorization", "Bearer {{authToken}}", "", false));
        RequestBody expectedBody = new RequestBody();
        expectedBody.setContent("{\"query\":\"query searchBillingAccount($filter: [String!]) {\\r\\n    searchBillingAccount @ filter(filters: $filter) {\\r\\n        id\\r\\n        name\\r\\n        billingMethod{\\r\\n            id\\r\\n            name\\r\\n        }\\r\\n        accountNumber\\r\\n        status\\r\\n        customer{\\r\\n            id\\r\\n            name\\r\\n            }\\r\\n        relatedProducts{\\r\\n            id\\r\\n            name\\r\\n            status\\r\\n            }\\r\\n\\r\\n    }\\r\\n}\\r\\n\",\"variables\":{\"filter\":[\"msisdn=590110865\"]}}");
        expectedBody.setQuery("query searchBillingAccount($filter: [String!]) {\n"
                + "    searchBillingAccount @ filter(filters: $filter) {\n"
                + "        id\n"
                + "        name\n"
                + "        billingMethod{\n"
                + "            id\n"
                + "            name\n"
                + "        }\n"
                + "        accountNumber\n"
                + "        status\n"
                + "        customer{\n"
                + "            id\n"
                + "            name\n"
                + "            }\n"
                + "        relatedProducts{\n"
                + "            id\n"
                + "            name\n"
                + "            status\n"
                + "            }\n"
                + "\n"
                + "    }\n"
                + "}\n");
        expectedBody.setVariables("{\"filter\":[\"msisdn=590110865\"]}");
        expectedBody.setType(RequestBodyType.GraphQL);
        expectedRequest.setBody(expectedBody);
        // TODO: Discuss if we should set requestMethod (if not present in curl) based on --data parameter.
        String curlString = "curl -X POST --location --globoff 'http://test.test/api/graphql-server/graphql' \\\n"
                + "--header 'Content-Type: application/json' \\\n"
                + "--header 'Authorization: Bearer {{authToken}}' \\\n"
                + "--data '{\"query\":\"query searchBillingAccount($filter: [String!]) {\\r\\n    searchBillingAccount @ filter(filters: $filter) {\\r\\n        id\\r\\n        name\\r\\n        billingMethod{\\r\\n            id\\r\\n            name\\r\\n        }\\r\\n        accountNumber\\r\\n        status\\r\\n        customer{\\r\\n            id\\r\\n            name\\r\\n            }\\r\\n        relatedProducts{\\r\\n            id\\r\\n            name\\r\\n            status\\r\\n            }\\r\\n\\r\\n    }\\r\\n}\\r\\n\",\"variables\":{\"filter\":[\"msisdn=590110865\"]}}'";
        CurlStringImportRequest importRequest = new CurlStringImportRequest(expectedRequest.getId(), curlString);
        // when
        when(repository.get().findById(any())).thenReturn(Optional.of(expectedRequest));
        requestService.get().importRequest(importRequest);

        // then
        verify(curlToRequestConverter.get()).convertCurlStringToRequest(eq(expectedRequest), any());
    }

    @Test
    public void executeRequest_postRequestSpecified_shouldSuccessfullyExecuted() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        HttpRequestEntitySaveRequest httpSaveRequest = generateRandomHttpRequestEntitySaveRequest();
        String bodyString = "{\"id\":\"123\"}";

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[]{
                new BasicHeader("Content-Type", "application/json")
        });
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(bodyString.getBytes()));
        when(response.getEntity()).thenReturn(entity);
        RequestExecutionHeaderResponse headerResponse = new RequestExecutionHeaderResponse("Content-Type", "application/json");
        // when
        mockTimer(metricService.get());
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        mockTimer(metricService.get());
        RequestExecutionDetails requestExecutionDetails = new RequestExecutionDetails();
        requestExecutionDetails.setId(UUID.randomUUID());
        RequestExecution requestExecution = new RequestExecution();
        requestExecution.setId(UUID.randomUUID());
        requestExecutionDetails.setRequestExecution(requestExecution);
        when(detailsRepository.get().findByRequestExecutionSseId(any(UUID.class))).thenReturn(Optional.of(requestExecutionDetails));
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        RequestExecutionResponse actualResponse = requestService.get().executeRequest(httpSaveRequest, "", "",
                sseId, Optional.empty(), httpSaveRequest.getEnvironmentId(), null);
        RequestExecutionResponse expectedResponse = RequestExecutionResponse.builder()
                .id(httpSaveRequest.getId())
                .responseHeaders(Collections.singletonList(headerResponse))
                .body(bodyString)
                .bodyType(RequestBodyType.JSON)
                .statusCode("200")
                .testsPassed(true)
                .statusText("OK")
                .duration(actualResponse.getDuration())
                .startedWhen(actualResponse.getStartedWhen())
                .executedWhen(actualResponse.getExecutedWhen())
                .contextVariables(actualResponse.getContextVariables())
                .executionId(requestExecutionDetails.getRequestExecution().getId())
                .cookies(actualResponse.getCookies())
                .build();
        // then
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void executeRequest_postRequestGraphQLSpecified_shouldSuccessfullyExecuted() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        String bodyString = "{\"id\":\"123\"}";

        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        request.setId(UUID.randomUUID());
        request.setName("Request");
        request.setProjectId(UUID.randomUUID());
        request.setTransportType(TransportType.REST);
        request.setFolderId(UUID.randomUUID());
        request.setHttpMethod(HttpMethod.POST);
        request.setUrl("http://test.test/api/graphql-server/graphql");
        request.setRequestParams(new ArrayList<>());
        request.setRequestHeaders(new ArrayList<>());
        request.getRequestHeaders().add(new HttpHeaderSaveRequest("Content-Type", "application/json", "", false));
        request.getRequestHeaders().add(new HttpHeaderSaveRequest("Authorization", "Bearer {{authToken}}", "", false));
        RequestBody body = new RequestBody();
        body.setContent("{\"query\":\"query searchBillingAccount($filter: [String!]) {\\r\\n    searchBillingAccount @ filter(filters: $filter) {\\r\\n        id\\r\\n        name\\r\\n        billingMethod{\\r\\n            id\\r\\n            name\\r\\n        }\\r\\n        accountNumber\\r\\n        status\\r\\n        customer{\\r\\n            id\\r\\n            name\\r\\n            }\\r\\n        relatedProducts{\\r\\n            id\\r\\n            name\\r\\n            status\\r\\n            }\\r\\n\\r\\n    }\\r\\n}\\r\\n\",\"variables\":{\"filter\":[\"msisdn=590110865\"]}}");
        body.setQuery("query searchBillingAccount($filter: [String!]) {\n"
                + "    searchBillingAccount @ filter(filters: $filter) {\n"
                + "        id\n"
                + "        name\n"
                + "        billingMethod{\n"
                + "            id\n"
                + "            name\n"
                + "        }\n"
                + "        accountNumber\n"
                + "        status\n"
                + "        customer{\n"
                + "            id\n"
                + "            name\n"
                + "            }\n"
                + "        relatedProducts{\n"
                + "            id\n"
                + "            name\n"
                + "            status\n"
                + "            }\n"
                + "\n"
                + "    }\n"
                + "}\n");
        body.setVariables("{\"filter\":[\"msisdn=590110865\"]}");
        body.setType(RequestBodyType.GraphQL);
        request.setBody(body);

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[]{
                new BasicHeader("Content-Type", "application/json")
        });
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(bodyString.getBytes()));
        when(response.getEntity()).thenReturn(entity);
        RequestExecutionHeaderResponse headerResponse = new RequestExecutionHeaderResponse("Content-Type", "application/json");
        // when
        mockTimer(metricService.get());
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        mockTimer(metricService.get());
        RequestExecutionResponse actualResponse = requestService.get().executeRequest(request, "", "", sseId,
                Optional.empty(), request.getEnvironmentId(), null);
        RequestExecutionResponse expectedResponse = RequestExecutionResponse.builder()
                .id(request.getId())
                .responseHeaders(Collections.singletonList(headerResponse))
                .body(bodyString)
                .bodyType(RequestBodyType.JSON)
                .statusCode("200")
                .statusText("OK")
                .testsPassed(true)
                .duration(actualResponse.getDuration())
                .startedWhen(actualResponse.getStartedWhen())
                .executedWhen(actualResponse.getExecutedWhen())
                .contextVariables(actualResponse.getContextVariables())
                .executionId(new UUID(0,0))
                .cookies(actualResponse.getCookies())
                .build();
        // then
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void executeHttpRequest_cookieForSessionFoundInDB_allDomainMatched_cookieHeaderShouldBeAdded()
            throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        List<Cookie> cookies = new ArrayList<>();
        Cookie cookie1 = new Cookie();
        cookie1.setKey("Cookie_1");
        cookie1.setDomain("test.test");
        cookie1.setValue("Cookie_1=value; Path=/;");
        cookies.add(cookie1);
        Cookie cookie2 = new Cookie();
        cookie2.setKey("Cookie_2");
        cookie2.setDomain("test.test");
        cookie2.setValue("Cookie_2=value; Path=/;");
        cookies.add(cookie2);
        HttpRequestEntitySaveRequest httpSaveRequest = generateRandomHttpRequestEntitySaveRequest();

        // when
        mockTimer(metricService.get());
        when(cookieService.get().getNotExpiredCookiesByUserIdAndProjectId(eq(httpSaveRequest.getProjectId())))
                .thenReturn(cookies);
        when(cookieService.get().cookieListToRequestHeader(any(URI.class), any())).thenCallRealMethod();
        when(cookieService.get().cookieListToRequestHeader(any(String.class), any())).thenCallRealMethod();
        when(cookieService.get().cookiesToString(any(URI.class), any())).thenCallRealMethod();
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        ArgumentCaptor<HttpUriRequest> httpRequestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);

        requestService.get().executeRequest(httpSaveRequest, "", "", sseId,
                Optional.empty(), httpSaveRequest.getEnvironmentId(), null);

        // then
        verify(httpClient).execute(httpRequestCaptor.capture());
        HttpUriRequest request = httpRequestCaptor.getValue();
        Header cookieHeader = request.getFirstHeader("Cookie");
        assertNotNull(cookieHeader);
        assertEquals("Cookie_1=value;Cookie_2=value", cookieHeader.getValue());
    }

    @Test
    public void getRequestSpecifiedWithDisabledParamsAndHeaders_executeRequest_shouldSuccessfullyExecuted() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        request.setTransportType(TransportType.REST);
        request.setUrl("http://test/service/api/v1/test");
        request.setHttpMethod(HttpMethod.GET);
        request.setProjectId(UUID.randomUUID());
        HttpParamSaveRequest nonDisabledParam = new HttpParamSaveRequest("name", "Eugene", "Name param", false);
        HttpParamSaveRequest disabledParam = new HttpParamSaveRequest("isIndexed", "false", "Indexed param", true);
        request.setRequestParams(asList(nonDisabledParam, disabledParam));
        HttpHeaderSaveRequest nonDisabledHeader = new HttpHeaderSaveRequest("Content-Type", "application/json", "Content type header", false);
        HttpHeaderSaveRequest disabledHeader = new HttpHeaderSaveRequest("Accept", "plain/text", "Accept type header", true);
        request.setRequestHeaders(asList(nonDisabledHeader, disabledHeader));

        Cookie cookie1 = new Cookie();
        cookie1.setId(UUID.randomUUID());
        cookie1.setKey("Cookie_1");
        cookie1.setValue("Cookie_1=value; Path=/;");
        request.setCookies(Collections.singletonList(cookie1));

        ArgumentCaptor<HttpEntityEnclosingRequestBase> requestCaptor = ArgumentCaptor.forClass(HttpEntityEnclosingRequestBase.class);

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        mockTimer(metricService.get());
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        requestService.get().executeRequest(request, "", "", sseId, Optional.empty(),
                request.getEnvironmentId(), null);
        // then
        verify(httpClient).execute(requestCaptor.capture());
        final HttpEntityEnclosingRequestBase capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest.getURI(), "Captured URL shouldn't be null");
        String url = capturedRequest.getURI().toString();
        assertTrue(url.contains(nonDisabledParam.getKey()), "Captured URL should contain non disabled param key");
        assertTrue(url.contains(nonDisabledParam.getValue()), "Captured URL should contain non disabled param value");
        assertFalse(url.contains(disabledParam.getKey()), "Captured URL shouldn't contain disabled param key");
        assertFalse(url.contains(disabledParam.getValue()), "Captured URL shouldn't contain disabled param value");
        assertNull(capturedRequest.getEntity(), "Captured HttpEntity should be null");
        final Header[] capturedHeaders = capturedRequest.getAllHeaders();
        assertNotEquals(0, capturedHeaders.length, "Captured headers shouldn't be empty");
        assertTrue(Arrays.stream(capturedHeaders).anyMatch(h -> h.getName().equals(nonDisabledHeader.getKey())), "Captured headers should contain non disabled header key");
        assertFalse(Arrays.stream(capturedHeaders).anyMatch(h -> h.getName().equals(disabledHeader.getKey())), "Captured headers shouldn't contain disabled header key");
    }

    @Test
    public void executeRequest_nonHttpClientErrorExceptionWillBeThrownDuringExecution_throwsException() throws IOException {
        // given
        final UUID sseId = UUID.randomUUID();
        String expectedExceptionMessage = ItfLiteHttpRequestExecuteException.DEFAULT_MESSAGE;
        HttpRequestEntitySaveRequest httpSaveRequest = generateRandomHttpRequestEntitySaveRequest();

        // when
        mockTimer(metricService.get());
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenThrow(
                new ItfLiteException(expectedExceptionMessage));
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        // then
        ItfLiteException exception = assertThrows(
                ItfLiteException.class,
                () -> requestService.get().executeRequest(httpSaveRequest, "", "", sseId, Optional.empty(),
                        httpSaveRequest.getEnvironmentId(), null)
        );
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    public void executeRequest_postRequestWithVelocityBodySpecified_shouldSuccessfullyExecuted() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        String bodyString = "{\"id\":\"123\"}";
        String contextId = "123";
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        HttpEntity entity = new ByteArrayEntity(bodyString.getBytes());
        Header[] headers = new Header[]{
                new BasicHeader("Content-Type", "application/json")
        };
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        when(response.getAllHeaders()).thenReturn(headers);
        when(response.getEntity()).thenReturn(entity);

        RequestExecutionHeaderResponse headerResponse =
                new RequestExecutionHeaderResponse("Content-Type", "application/json");
        RequestBody body = new RequestBody(bodyString, RequestBodyType.Velocity);
        HttpRequestEntitySaveRequest httpSaveRequest = generateRandomHttpRequestEntitySaveRequest();
        httpSaveRequest.setBody(body);
        ItfParametersResolveResponse itfResponse = new ItfParametersResolveResponse(bodyString);
        String itfUrl = "http://itf.url";
        ItfProjectIdResolveResponse itfProjectIdResponse = new ItfProjectIdResolveResponse("1");

        // when
        mockTimer(metricService.get());
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(itfFeignService.get().processVelocity(any(), any())).thenReturn(itfResponse);
        when(feignClientsProperties.get().getIsFeignAtpItfEnabled()).thenReturn(true);
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        RequestExecutionResponse actualResponse = requestService.get().executeRequest(
                httpSaveRequest, contextId, "", sseId, Optional.empty(), httpSaveRequest.getEnvironmentId(), null);
        RequestExecutionResponse expectedResponse = RequestExecutionResponse.builder()
                .id(httpSaveRequest.getId())
                .responseHeaders(Collections.singletonList(headerResponse))
                .body(bodyString)
                .bodyType(RequestBodyType.JSON)
                .statusCode("200")
                .statusText("OK")
                .duration(actualResponse.getDuration())
                .startedWhen(actualResponse.getStartedWhen())
                .executedWhen(actualResponse.getExecutedWhen())
                .contextVariables(actualResponse.getContextVariables())
                .executionId(new UUID(0,0))
                .cookies(actualResponse.getCookies())
                .testsPassed(true)
                .build();
        // then
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void executeRequest_postRequestSpecifiedAndNotAuthorized_shouldThrowHttpClientException() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        String errorMessage = "{\"message\":\"403 Forbidden - Not authorized to access /api/v4/entities\"}";
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        HttpEntity entity = new ByteArrayEntity(errorMessage.getBytes());
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 403, "FORBIDDEN"));
        when(response.getEntity()).thenReturn(entity);
        HttpRequestEntitySaveRequest httpSaveRequest = generateRandomHttpRequestEntitySaveRequest();

        // when
        mockTimer(metricService.get());
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        RequestExecutionResponse actualResponse = requestService.get().executeRequest(
                httpSaveRequest, "", "", sseId, Optional.empty(), httpSaveRequest.getEnvironmentId(), null);
        RequestExecutionResponse expectedResponse = RequestExecutionResponse.builder()
                .id(httpSaveRequest.getId())
                .responseHeaders(Collections.emptyList())
                .body(errorMessage)
                .bodyType(RequestBodyType.JSON)
                .statusCode(String.valueOf(HttpStatus.FORBIDDEN.value()))
                .statusText(HttpStatus.FORBIDDEN.name())
                .duration(actualResponse.getDuration())
                .startedWhen(actualResponse.getStartedWhen())
                .executedWhen(actualResponse.getExecutedWhen())
                .contextVariables(actualResponse.getContextVariables())
                .executionId(new UUID(0,0))
                .cookies(actualResponse.getCookies())
                .testsPassed(true)
                .build();
        // then
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void executeRequest_withQueryParamsContainingSpecialCharacter_shouldSuccessfullyExecuted() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        mockTimer(metricService.get());
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        request.setUrl("http://test/service/api/v1/test");
        request.setHttpMethod(HttpMethod.GET);
        request.setTransportType(TransportType.REST);
        request.setProjectId(UUID.randomUUID());

        HttpParamSaveRequest nonDisabledParam = new HttpParamSaveRequest(
                "test_string", "[]{}\"&= |\\^`$&+,/:;=?@", "string with special characters", false);
        request.setRequestParams(Collections.singletonList(nonDisabledParam));
        request.setRequestHeaders(new ArrayList<>());

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        HttpRequestEntitySaveRequest httpSaveRequest = generateRandomHttpRequestEntitySaveRequest();

        // when
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        requestService.get().executeRequest(request, "", "", sseId, Optional.empty(),
                httpSaveRequest.getEnvironmentId(), null);

        // then
        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(requestCaptor.capture());

        final HttpUriRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest, "Captured URL shouldn't be null");
        assertTrue(capturedRequest.getURI().toString().contains(nonDisabledParam.getKey()), "Captured URL should contain non disabled param key");
        assertTrue(capturedRequest.getURI().toString().contains("%5B%5D%7B%7D%22%26%3D%20%7C%5C%5E%60%24%26%2B%2C%2F%3A%3B%3D%3F%40"),
                "Captured URL should contain encoded query parameter");
    }

    @Test
    public void getContext_contextIdIsSpecified_shouldSuccessfullyGetContextFromItf() throws URISyntaxException {
        // given
        final UUID projectId = UUID.randomUUID();
        String expectedContext = "{\"server\": \"SomeText\"}";
        String contextId = new BigInteger("1").toString();

        // when
        when(itfFeignService.get().getContext(any(), any())).thenReturn(expectedContext);
        when(feignClientsProperties.get().getIsFeignAtpItfEnabled()).thenReturn(true);
        String actualContext = requestService.get().getContext(projectId, contextId);
        // then
        assertEquals(actualContext, expectedContext);
    }

    @Test
    public void getContext_contextUrlIsSpecified_shouldSuccessfullyGetContextFromItf() throws URISyntaxException {
        // given
        final UUID projectId = UUID.randomUUID();
        String expectedContext = "{\"server\": \"SomeText\"}";
        String itfUrl = "http://itf.url/";
        String contextId = "1";
        String contextUrl = itfUrl + RequestService.ITF_URL_CONTEXT_SEPARATOR + contextId;
        ItfProjectIdResolveResponse itfProjectIdResponse = new ItfProjectIdResolveResponse("1");

        // when
        when(itfPlainFeignClient.get().getContext(any(), any(), any(), any())).thenReturn(expectedContext);
        when(feignClientsProperties.get().getIsFeignAtpItfEnabled()).thenReturn(true);
        String actualContext = requestService.get().getContext(projectId, contextUrl);

        // then
        assertEquals(actualContext, expectedContext);
        ArgumentCaptor<URI> itfUrlCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<String> itfRouteCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UUID> itfProjectIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> contextIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(itfPlainFeignClient.get()).getContext(itfUrlCaptor.capture(), itfRouteCaptor.capture(),
                itfProjectIdCaptor.capture(),
                contextIdCaptor.capture());
        assertEquals(itfUrlCaptor.getValue(), new URI(itfUrl));
        assertEquals(contextIdCaptor.getValue(), contextId);
    }

    @Test
    public void updateRequestHeadersAndParameters_methodApplies_shouldSuccessfullyUpdated() {
        RequestHeader requestHeader1 = generateRequestHeader(" key1 \r", " value1");
        RequestHeader requestHeader2 = generateRequestHeader(" key2 \r\n", "value2 ");
        RequestHeader requestHeader3 = generateRequestHeader(" k\bey\t3", " value3 ");
        List<RequestHeader> headers = Arrays.asList(requestHeader1, requestHeader2, requestHeader3);
        requestService.get().updateHeadersFields(headers);
        assertEquals(asList("key1", "key2", "key3"),
                headers.stream().map(RequestHeader::getKey).collect(Collectors.toList()));
        assertEquals(asList("value1", "value2", "value3"),
                headers.stream().map(RequestHeader::getValue).collect(Collectors.toList()));
        RequestParam requestParam1 = generateRequestParameter(" key1 \r", " value1");
        RequestParam requestParam2 = generateRequestParameter(" key2 \r\n", "value2 ");
        RequestParam requestParam3 = generateRequestParameter(" k\bey\t3", "value3");
        List<RequestParam> parameters = Arrays.asList(requestParam1, requestParam2, requestParam3);
        requestService.get().updateParametersFields(parameters);
        assertEquals(asList("key1", "key2", "key3"),
                parameters.stream().map(RequestParam::getKey).collect(Collectors.toList()));
        assertEquals(asList("value1", "value2", "value3"),
                parameters.stream().map(RequestParam::getValue).collect(Collectors.toList()));
    }

    @Test
    void fewRequestAreExistedAndSomeRequestsHaveNullOrders_testOrder_expectedSuccessfullyChangedOrder() {
        // given
        final UUID projectId = UUID.randomUUID();
        final int order = 4;
        final RequestOrderChangeRequest request = new RequestOrderChangeRequest(projectId, null, order);
        final Request r6 = generateHttpRequest("r6", projectId, null);
        final List<Request> requests = new ArrayList<>(asList(
                generateHttpRequest("r1", projectId, null, null),
                generateHttpRequest("r2", projectId, null, 0),
                generateHttpRequest("r3", projectId, null, null),
                generateHttpRequest("r4", projectId, null, 2),
                generateHttpRequest("r5", projectId, null),
                r6
        ));
        final UUID changedRequestId = r6.getId();
        when(repository.get().findAllByProjectIdAndFolderId(projectId, null)).thenReturn(requests);
        // when
        requestService.get().order(changedRequestId, request);
        // then
        ArgumentCaptor<List<Request>> requestsSaveCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository.get(), times(1)).saveAll(requestsSaveCaptor.capture());
        final List<Request> savedRequests = requestsSaveCaptor.getValue();
        assertNotNull(savedRequests, "Saved requests list shouldn't be null");
        assertFalse(savedRequests.isEmpty(), "Saved requests list shouldn't be empty");
        assertEquals(requests.size(), savedRequests.size(), "Saved requests list size should be equal to fetched from the db");
        final Request changedRequest = requests.get(order);
        assertEquals(changedRequestId, changedRequest.getId(), "Changed request id should be equal to the requested one");
        assertEquals(order, changedRequest.getOrder(), "Changed request order should be equal to the requested value");
    }

    @Test
    void resolveAllVariablesTest_HttpRequestConfiguredWithAuth_successfullyResolved()
            throws AtpDecryptException, URISyntaxException {
        // given
        String bodyString = "{\"id\":\"123\"}";
        RequestBody body = new RequestBody(bodyString, RequestBodyType.Velocity);
        HttpRequest httpRequest = generateRandomHttpRequest();
        httpRequest.setBody(body);
        AuthorizationSaveRequest auth2AuthorizationSaveRequest = generateRandomOAuth2AuthorizationSaveRequest();
        RequestAuthorization authorization = modelMapper.map(auth2AuthorizationSaveRequest,
                OAuth2RequestAuthorization.class);
        httpRequest.setAuthorization(authorization);
        ItfParametersResolveResponse itfResponse = new ItfParametersResolveResponse(bodyString);
        // when
        when(feignClientsProperties.get().getIsFeignAtpItfEnabled()).thenReturn(true);
        when(itfFeignService.get().processVelocity(any(), any())).thenReturn(itfResponse);
        HttpRequest actualRequest = requestService.get().resolveAllVariables(httpRequest, "", true, null);
        // then
        verify(itfFeignService.get(), times(1)).processVelocity(any(), any());
        assertEquals(httpRequest, actualRequest);
    }

    @Test
    public void executeHttpRequestWithLogging_httpRequestSpecifiedForTestRunAndProject_shouldSuccessfullyExecute()
            throws Exception {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        ExecutionCollectionRequestExecuteRequest requestExecuteRequest = generateRequestExecuteRequest();
        // when
        mockTimer(metricService.get());

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);

        requestService.get().executeRequestWithRamAdapterLogging(requestExecuteRequest, httpRequest, null);
    }

    @Test
    public void executeHttpRequestWithLogging_cookieForTestRunFoundInDB_allDomainMatched_cookieHeaderShouldBeAdded()
            throws Exception {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        ExecutionCollectionRequestExecuteRequest requestExecuteRequest = generateRequestExecuteRequest();
        List<Cookie> cookies = new ArrayList<>();
        Cookie cookie1 = new Cookie();
        cookie1.setKey("Cookie_1");
        cookie1.setDomain("test.test");
        cookie1.setValue("Cookie_1=value; Path=/;");
        cookies.add(cookie1);
        Cookie cookie2 = new Cookie();
        cookie2.setKey("Cookie_2");
        cookie2.setDomain("test.test");
        cookie2.setValue("Cookie_2=value; Path=/;");
        cookies.add(cookie2);

        // when
        mockTimer(metricService.get());
        when(ramService.get().writeTestsResults(any(), anyBoolean())).thenReturn(new JsExecutionResult(true, null));
        when(cookieService.get().getAllByExecutionRequestIdAndTestRunId(
                eq(requestExecuteRequest.getExecutionRequestId()), eq(requestExecuteRequest.getTestRunId())))
                .thenReturn(cookies);
        when(cookieService.get().cookieListToRequestHeader(any(URI.class), any())).thenCallRealMethod();
        when(cookieService.get().cookieListToRequestHeader(any(String.class), any())).thenCallRealMethod();
        when(cookieService.get().cookiesToString(any(URI.class), any())).thenCallRealMethod();
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        ArgumentCaptor<HttpUriRequest> httpRequestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);

        requestService.get().executeRequestWithRamAdapterLogging(requestExecuteRequest, httpRequest, null);

        // then
        verify(httpClient).execute(httpRequestCaptor.capture());
        HttpUriRequest request = httpRequestCaptor.getValue();
        Header cookieHeader = request.getFirstHeader("Cookie");
        assertNotNull(cookieHeader);
        assertEquals("Cookie_1=value;Cookie_2=value", cookieHeader.getValue());
    }

    @Test
    public void executeHttpRequestWithLogging_httpRequestSpecifiedForTestRunAndProject_ExecuteStepResponseTestingStatusFailed()
            throws Exception {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        ExecutionCollectionRequestExecuteRequest requestExecuteRequest = generateRequestExecuteRequest();
        // when
        mockTimer(metricService.get());
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenThrow(new ItfLiteHttpRequestExecuteException());
        // then
        ExecuteStepResponse res = requestService.get().executeRequestWithRamAdapterLogging(requestExecuteRequest,
                httpRequest, null);
        assertEquals(TestingStatuses.FAILED, res.getTestingStatus());
    }

    @Test
    public void getRequestByProjectIdAndRequestIdTest_shouldSuccessfullyReturnRequest() {
        // given
        final UUID projectId = UUID.randomUUID();
        HttpRequest httpRequest = generateRandomHttpRequest();

        // when
        when(repository.get().findByProjectIdAndId(any(), any())).thenReturn(Optional.of(httpRequest));
        Request actualRequest = requestService.get().getRequestByProjectIdAndRequestId(projectId, httpRequest.getId());
        // then
        assertEquals(httpRequest, actualRequest);
    }

    @Test
    public void saveRequest_WhenBodyTypeIsFormData_ShouldValidSafe() {
        HttpRequest httpRequest = EntitiesGenerator.generateRandomHttpRequest();
        UUID requestId = httpRequest.getId();
        HttpRequestEntitySaveRequest httpRequestEntitySaveRequest = EntitiesGenerator
                .generateRandomHttpRequestEntitySaveRequestWithFormData();
        httpRequestEntitySaveRequest.setId(requestId);
        when(repository.get().findById(requestId)).thenReturn(Optional.of(httpRequest));
        when(repository.get().save(any(Request.class))).thenAnswer(answer -> answer.getArguments()[0]);
        HttpRequest actualRequest = (HttpRequest) requestService.get()
                .saveRequest(requestId, httpRequestEntitySaveRequest, new ArrayList<>(), Optional.empty());
        verify(repository.get()).save(any());
        assertEquals(requestId, actualRequest.getId());
        RequestBody actualBody = actualRequest.getBody();
        RequestBody expectedBody = httpRequestEntitySaveRequest.getBody();

        assertEquals(actualBody.getType(), expectedBody.getType());
        assertEquals(actualBody.getFormDataBody().size(), expectedBody.getFormDataBody().size());
    }

    @Test
    public void saveRequest_WhenBodyTypeWasFormData_FormDataWithFileWasRemoved_ShouldRemoveFile() {
        HttpRequest httpRequest = EntitiesGenerator.generateRandomHttpRequestWithFormData();
        UUID requestId = httpRequest.getId();
        UUID fileId = UUID.randomUUID();
        httpRequest.getBody().getFormDataBody().get(1).setFileId(fileId);
        HttpRequestEntitySaveRequest httpRequestEntitySaveRequest = EntitiesGenerator
                .generateRandomHttpRequestEntitySaveRequestWithFormData();
        httpRequestEntitySaveRequest.getBody().setFormDataBody(
                singletonList(httpRequestEntitySaveRequest.getBody().getFormDataBody().get(0))
        );
        httpRequestEntitySaveRequest.setId(requestId);
        when(repository.get().findById(requestId)).thenReturn(Optional.of(httpRequest));
        when(repository.get().save(any(Request.class))).thenAnswer(answer -> answer.getArguments()[0]);
        requestService.get().saveRequest(requestId, httpRequestEntitySaveRequest, new ArrayList<>(), Optional.empty());
        verify(gridFsService.get(), times(1)).removeFileByFileId(fileId);
    }

    @Test
    public void saveRequest_WhenFileSizeMoreThan10mb_ShouldThrowException() {
        HttpRequest httpRequest = EntitiesGenerator.generateRandomHttpRequestWithFormData();
        UUID requestId = httpRequest.getId();
        HttpRequestEntitySaveRequest httpRequestEntitySaveRequest = EntitiesGenerator
                .generateRandomHttpRequestEntitySaveRequestWithFormData();
        ReflectionTestUtils.setField(requestService.get(), "maxFileSize", 10485760);
        MultipartFile multipartFile = new MockMultipartFile("test.pdf", new byte[11534336]);
        MultipartFile multipartFile1 = new MockMultipartFile("test.pdf", new byte[10]);
        List<MultipartFile> files = new ArrayList<>();
        files.add(multipartFile);
        files.add(multipartFile1);
        assertThrows(ItfLiteMaxFileException.class,
                () -> requestService.get().saveRequest(requestId, httpRequestEntitySaveRequest, files, Optional.empty()));
    }

    @Test
    public void executeRequest_WhenRequestBodyTypeFormDataSpecified_ShouldSuccessfullyExecuted() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        String bodyString = "{\"id\":\"123\"}";
        String contextId = "123";
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        HttpEntity entity = new ByteArrayEntity(bodyString.getBytes());
        Header[] headers = new Header[]{
                new BasicHeader("Content-Type", "application/json")
        };
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        when(response.getAllHeaders()).thenReturn(headers);
        when(response.getEntity()).thenReturn(entity);

        RequestExecutionHeaderResponse headerResponse =
                new RequestExecutionHeaderResponse("Content-Type", "application/json");
        HttpRequestEntitySaveRequest request = EntitiesGenerator.generateRandomHttpRequestEntitySaveRequestWithFormData();

        // when
        mockTimer(metricService.get());
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));

        List<FileData> fileNameStreamMap = new ArrayList<>();

        Optional<FormDataPart> requestBodyType =
                request.getBody().getFormDataBody().stream()
                        .filter(requestFormDataBody -> ValueType.FILE.equals(requestFormDataBody.getType()))
                        .findFirst();
        String fileNameFromRequest = requestBodyType.get().getValue();
        fileNameStreamMap.add(new FileData(bodyString.getBytes(), fileNameFromRequest));

        RequestExecutionResponse actualResponse = requestService.get().executeRequest(
                request, contextId, "", sseId, Optional.empty(), request.getEnvironmentId(),
                fileNameStreamMap);
        RequestExecutionResponse expectedResponse = RequestExecutionResponse.builder()
                .id(request.getId())
                .responseHeaders(Collections.singletonList(headerResponse))
                .body(bodyString)
                .bodyType(RequestBodyType.JSON)
                .statusCode("200")
                .statusText("OK")
                .duration(actualResponse.getDuration())
                .startedWhen(actualResponse.getStartedWhen())
                .executedWhen(actualResponse.getExecutedWhen())
                .contextVariables(actualResponse.getContextVariables())
                .executionId(new UUID(0,0))
                .cookies(actualResponse.getCookies())
                .testsPassed(true)
                .build();
        // then
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void executeRequest_responseIsNotMeetConfiguredSize_shouldThrowException() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        HttpRequestEntitySaveRequest httpSaveRequest = generateRandomHttpRequestEntitySaveRequest();
        // when
        mockTimer(metricService.get());
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        HttpEntity responseEntity = mock(HttpEntity.class);
        when(response.getEntity()).thenReturn(responseEntity);
        when(response.getEntity().getContent()).thenReturn(new ByteArrayInputStream(new byte[11534336]));
        when(response.getEntity().getContentLength()).thenReturn(10L);
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(requestResponseSizeProperties.get().getResponseSizeLimitInMb()).thenReturn(1);
        // then
        assertThrows(ItfLiteResponseSizeLimitException.class, () -> requestService.get().executeRequest(
                httpSaveRequest, "", "", sseId, Optional.empty(), httpSaveRequest.getEnvironmentId(), null)
        );
    }

    @Test
    public void executeRequest_WhenRequestBodyTypeFormDataSpecified_contentTypeWithBoundaryShouldBeSpecified() throws Exception {
        // given
        String bodyString = "{\"id\":\"123\"}";
        String contextId = "123";
        HttpEntity entity = new ByteArrayEntity(bodyString.getBytes());
        Header[] headers = new Header[]{
                new BasicHeader("Content-Type", "application/json")
        };
        RequestExecutionHeaderResponse headerResponse =
                new RequestExecutionHeaderResponse("Content-Type", "application/json");
        HttpRequestEntitySaveRequest request = EntitiesGenerator.generateRandomHttpRequestEntitySaveRequestWithFormData();
        final UUID sseId = UUID.randomUUID();

        // when
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        when(response.getAllHeaders()).thenReturn(headers);
        when(response.getEntity()).thenReturn(entity);
        mockTimer(metricService.get());
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));

        List<FileData> fileNameStreamMap = new ArrayList<>();

        Optional<FormDataPart> requestBodyType =
                request.getBody().getFormDataBody().stream()
                        .filter(requestFormDataBody -> ValueType.FILE.equals(requestFormDataBody.getType()))
                        .findFirst();
        String fileNameFromRequest = requestBodyType.get().getValue();
        fileNameStreamMap.add(new FileData(bodyString.getBytes(), fileNameFromRequest));

        RequestExecutionResponse actualResponse = requestService.get().executeRequest(
                request, contextId, "", sseId, Optional.empty(), request.getEnvironmentId(),
                fileNameStreamMap);

        // then
        RequestExecutionResponse expectedResponse = RequestExecutionResponse.builder()
                .id(request.getId())
                .responseHeaders(Collections.singletonList(headerResponse))
                .body(bodyString)
                .bodyType(RequestBodyType.JSON)
                .statusCode("200")
                .statusText("OK")
                .duration(actualResponse.getDuration())
                .startedWhen(actualResponse.getStartedWhen())
                .executedWhen(actualResponse.getExecutedWhen())
                .contextVariables(actualResponse.getContextVariables())
                .executionId(new UUID(0,0))
                .cookies(actualResponse.getCookies())
                .testsPassed(true)
                .build();
        assertEquals(expectedResponse, actualResponse);
        ArgumentCaptor<HttpEntityEnclosingRequestBase> requestCapture =
                ArgumentCaptor.forClass(HttpEntityEnclosingRequestBase.class);
        verify(httpClient, times(1)).execute(requestCapture.capture());
        HttpEntityEnclosingRequestBase httpRequest = requestCapture.getValue();
        assertTrue(httpRequest.getHeaders("Content-Type")[0].getValue().contains("boundary"));
    }

    @Test
    public void executeRequest_whenUserDefinedOwnBoundaryForFormDataParts_contentTypeWithBoundaryShouldBeSpecified() throws Exception {
        // given
        String bodyString = "{\"id\":\"123\"}";
        String contextId = "123";
        HttpEntity entity = new ByteArrayEntity(bodyString.getBytes());
        Header[] headers = new Header[]{
                new BasicHeader("Content-Type", "application/json")
        };
        RequestExecutionHeaderResponse headerResponse =
                new RequestExecutionHeaderResponse("Content-Type", "application/json");
        HttpRequestEntitySaveRequest request = EntitiesGenerator.generateRandomHttpRequestEntitySaveRequestWithFormData();
        request.getRequestHeaders().add(new HttpHeaderSaveRequest("Content-Type", "multipart/formdata; boundary=test", null));
        final UUID sseId = UUID.randomUUID();

        // when
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        when(response.getAllHeaders()).thenReturn(headers);
        when(response.getEntity()).thenReturn(entity);
        mockTimer(metricService.get());
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));

        List<FileData> fileNameStreamMap = new ArrayList<>();

        Optional<FormDataPart> requestBodyType =
                request.getBody().getFormDataBody().stream()
                        .filter(requestFormDataBody -> ValueType.FILE.equals(requestFormDataBody.getType()))
                        .findFirst();
        String fileNameFromRequest = requestBodyType.get().getValue();
        fileNameStreamMap.add(new FileData(bodyString.getBytes(), fileNameFromRequest));

        RequestExecutionResponse actualResponse = requestService.get().executeRequest(
                request, contextId, "", sseId, Optional.empty(), request.getEnvironmentId(),
                fileNameStreamMap);

        // then
        RequestExecutionResponse expectedResponse = RequestExecutionResponse.builder()
                .id(request.getId())
                .responseHeaders(Collections.singletonList(headerResponse))
                .body(bodyString)
                .bodyType(RequestBodyType.JSON)
                .statusCode("200")
                .statusText("OK")
                .duration(actualResponse.getDuration())
                .startedWhen(actualResponse.getStartedWhen())
                .executedWhen(actualResponse.getExecutedWhen())
                .contextVariables(actualResponse.getContextVariables())
                .executionId(new UUID(0,0))
                .cookies(actualResponse.getCookies())
                .testsPassed(true)
                .build();
        assertEquals(expectedResponse, actualResponse);
        ArgumentCaptor<HttpEntityEnclosingRequestBase> requestCapture =
                ArgumentCaptor.forClass(HttpEntityEnclosingRequestBase.class);
        verify(httpClient, times(1)).execute(requestCapture.capture());
        HttpEntityEnclosingRequestBase httpRequest = requestCapture.getValue();
        assertTrue(httpRequest.getHeaders("Content-Type")[0].getValue().contains("boundary=test"));
    }

    @Test
    public void executeHttpRequest_withHistory_disabledParametersAndHeadersShouldNotBeDisplayed() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        HttpRequestEntitySaveRequest request = generateRandomHttpRequestEntitySaveRequest();
        request.setRequestHeaders(
                Arrays.asList(
                        new HttpHeaderSaveRequest("h_key1", "h_value1", "", false),
                        new HttpHeaderSaveRequest("h_key2", "h_value2", "", true)
                ));
        request.setRequestParams(
                Arrays.asList(
                        new HttpParamSaveRequest("q_key1", "q_value1", "", false),
                        new HttpParamSaveRequest("q_key2", "q_value2", "", true)
                ));

        // when
        mockTimer(metricService.get());
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        requestService.get().executeRequest(request, "", "", sseId, Optional.empty(), request.getEnvironmentId(),
                null);

        // then
        ArgumentCaptor<HttpRequestEntitySaveRequest> requestForHistoryCaptor =
                ArgumentCaptor.forClass(HttpRequestEntitySaveRequest.class);
        verify(executionHistoryService.get()).logRequestExecution(any(String.class), any(UUID.class),
                requestForHistoryCaptor.capture(), any(RequestExecutionResponse.class), any(), any());

        HttpRequestEntitySaveRequest requestForHistory = requestForHistoryCaptor.getValue();
        assertEquals(1, requestForHistory.getRequestHeaders().size());
        assertFalse(requestForHistory.getRequestHeaders().get(0).isDisabled());
        assertEquals(1, requestForHistory.getRequestParams().size());
        assertFalse(requestForHistory.getRequestParams().get(0).isDisabled());
    }

    @Test
    public void executeRequest_withGlobalContextVariablesSpecified_shouldSuccessfullyExecuted() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        String bodyString = "{\"id\":\"123\"}";

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        BasicStatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK");
        BasicHeader basicHeader = new BasicHeader("Content-Type", "application/json");
        Header[] headers = new Header[]{ basicHeader };
        BasicHttpEntity entity = new BasicHttpEntity();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bodyString.getBytes());
        entity.setContent(inputStream);
        RequestExecutionHeaderResponse headerResponse = new RequestExecutionHeaderResponse("Content-Type", "application/json");
        HttpRequestEntitySaveRequest httpSaveRequest = generateRandomHttpRequestEntitySaveRequest();
        ContextVariable contextVariable = new ContextVariable("key_1", "value_1", ContextVariableType.GLOBAL);
        httpSaveRequest.setContextVariables(singletonList(contextVariable));

        // when
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getAllHeaders()).thenReturn(headers);
        when(response.getEntity()).thenReturn(entity);
        mockTimer(metricService.get());
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        mockTimer(metricService.get());
        List<ContextVariable> expectedGlobalVariables = new ArrayList<>();
        expectedGlobalVariables.add(new ContextVariable("key_1", "value_1", ContextVariableType.GLOBAL));
        RequestExecutionResponse actualResponse = requestService.get().executeRequest(httpSaveRequest, "", "", sseId,
                Optional.empty(), httpSaveRequest.getEnvironmentId(), null);
        RequestExecutionResponse expectedResponse = RequestExecutionResponse.builder()
                .id(httpSaveRequest.getId())
                .responseHeaders(Collections.singletonList(headerResponse))
                .body(bodyString)
                .bodyType(RequestBodyType.JSON)
                .statusCode("200")
                .statusText("OK")
                .duration(actualResponse.getDuration())
                .startedWhen(actualResponse.getStartedWhen())
                .executedWhen(actualResponse.getExecutedWhen())
                .contextVariables(expectedGlobalVariables)
                .executionId(new UUID(0,0))
                .cookies(actualResponse.getCookies())
                .testsPassed(true)
                .build();
        // then
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void executeRequest_withAllContextVariablesTypesSpecified_allVariablesShouldBeInResponse() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        String bodyString = "{\"id\":\"123\"}";

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        BasicStatusLine basicStatusLine = new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK");
        BasicHeader basicHeader = new BasicHeader("Content-Type", "application/json");
        Header[] headers = new Header[]{ basicHeader };
        BasicHttpEntity entity = new BasicHttpEntity();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bodyString.getBytes());
        entity.setContent(inputStream);
        ContextVariable contextVariable1 = new ContextVariable("key_1", "value_1", ContextVariableType.GLOBAL);
        ContextVariable contextVariable2 = new ContextVariable("key_2", "value_2", ContextVariableType.COLLECTION);
        ContextVariable contextVariable3 = new ContextVariable("key_3", "value_3", ContextVariableType.DATA);
        ContextVariable contextVariable4 = new ContextVariable("key_4", "value_4", ContextVariableType.LOCAL);
        ContextVariable contextVariable5 = new ContextVariable("key_5", "value_5",
                ContextVariableType.ENVIRONMENT);
        List<ContextVariable> contextVariables = Arrays.asList(contextVariable1, contextVariable2, contextVariable3,
                contextVariable4, contextVariable5);
        HttpRequestEntitySaveRequest httpSaveRequest = generateRandomHttpRequestEntitySaveRequest();
        httpSaveRequest.setContextVariables(contextVariables);

        // when
        when(response.getStatusLine()).thenReturn(basicStatusLine);
        when(response.getAllHeaders()).thenReturn(headers);
        when(response.getEntity()).thenReturn(entity);
        mockTimer(metricService.get());
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        mockTimer(metricService.get());
        RequestExecutionResponse actualResponse = requestService.get().executeRequest(httpSaveRequest, "", "", sseId,
                Optional.empty(), httpSaveRequest.getEnvironmentId(), null);
        // then
        assertTrue(contextVariables.containsAll(actualResponse.getContextVariables()));
    }

    @Test
    public void testExecuteHttpRequest_WithVariables_ChangeVarsInFileByPriority() throws Exception {
        File file = new File("src/test/resources/tests/38812ea8-e0b2-4acd-ab1e-34be7ad9b502/testBinaryFile.txt");
        MultipartFile testFile = new MockMultipartFile("testBinaryFile", "testBinaryFile.txt",
                ContentType.APPLICATION_JSON.getMimeType(), Files.readAllBytes(file.toPath()));
        HttpRequestEntitySaveRequest httpSaveRequest = generateRandomHttpRequestEntitySaveRequest();

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[]{
                new BasicHeader("Content-Type", "application/json")
        });
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(response);

        requestService.get().executeHttpRequest(UUID.randomUUID(), httpSaveRequest, null, Optional.of(testFile),
                RequestTestUtils.generateContext(), null, new RequestRuntimeOptions());

        verify(fileService.get()).resolveParametersInMultipartFile(any(), any());
    }

    @Test
    public void testExecuteHttpRequest_responseContentTypeTextXML_shouldSendResponseWithXMLBodyType() throws Exception {
        // given
        CloseableHttpResponse responseWithCharset = mock(CloseableHttpResponse.class);
        when(responseWithCharset.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        when(responseWithCharset.getAllHeaders()).thenReturn(new Header[]{
                new BasicHeader("Content-Type", "text/xml;charset=utf-8")
        });
        CloseableHttpResponse responseWithoutCharset = mock(CloseableHttpResponse.class);
        when(responseWithoutCharset.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        when(responseWithoutCharset.getAllHeaders()).thenReturn(new Header[]{
                new BasicHeader("Content-Type", "text/xml")
        });
        HttpRequestEntitySaveRequest httpSaveRequest = generateRandomHttpRequestEntitySaveRequest();

        // when
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(responseWithCharset).thenReturn(responseWithoutCharset);

        RequestExecutionResponse actualResponseWithCharset = requestService.get().executeHttpRequest(UUID.randomUUID(),
                httpSaveRequest,
                null, Optional.empty(), RequestTestUtils.generateContext(), null, new RequestRuntimeOptions());
        RequestExecutionResponse actualResponseWithoutCharset = requestService.get().executeHttpRequest(UUID.randomUUID(),
                httpSaveRequest,
                null, Optional.empty(), RequestTestUtils.generateContext(), null, new RequestRuntimeOptions());

        // then
        assertEquals(RequestBodyType.XML, actualResponseWithCharset.getBodyType());
        assertEquals(RequestBodyType.XML, actualResponseWithoutCharset.getBodyType());
    }

    @Test
    public void executeHttpRequestTest_disabledAutoEncoding_shouldExecuteRequestWithoutEncoding() throws Exception {
        // given
        String keyWithEncodedCharacters = "key%20key";
        String valueWithEncodedCharacters = "value%24%25value%25";
        HttpRequestEntitySaveRequest httpSaveRequest = generateRandomHttpRequestEntitySaveRequest();
        httpSaveRequest.setRequestParams(singletonList(
                new HttpParamSaveRequest(keyWithEncodedCharacters, valueWithEncodedCharacters, "name")));
        RequestRuntimeOptions requestRuntimeOptions = new RequestRuntimeOptions();
        requestRuntimeOptions.setDisableAutoEncoding(true);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        // when
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any(), any(), any(), any())).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(response);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[]{
                new BasicHeader("Content-Type", "application/json")
        });
        requestService.get().executeHttpRequest(UUID.randomUUID(), httpSaveRequest, null, Optional.empty(),
                RequestTestUtils.generateContext(), null, requestRuntimeOptions);

        // then
        ArgumentCaptor<HttpEntityEnclosingRequestBase> requestCaptor =
                ArgumentCaptor.forClass(HttpEntityEnclosingRequestBase.class);
        verify(httpClient, times(1)).execute(requestCaptor.capture());
        HttpEntityEnclosingRequestBase request = requestCaptor.getValue();

        assertEquals(httpSaveRequest.getUrl() + "?" + keyWithEncodedCharacters + "=" + valueWithEncodedCharacters,
                request.getRequestLine().getUri());
    }

    @Test
    public void executeHttpRequestTest_disabledAutoEncodingAndQueryVariablesHaveIllegalCharacters_shouldThrowIllegalArgumentException() throws Exception {
        // given
        String keyWithIllegalCharacters = "key key";
        String valueWithIllegalCharacters = "value$%value%";
        HttpRequestEntitySaveRequest httpSaveRequest = generateRandomHttpRequestEntitySaveRequest();
        httpSaveRequest.setRequestParams(singletonList(
                new HttpParamSaveRequest(keyWithIllegalCharacters, valueWithIllegalCharacters, "name")));
        RequestRuntimeOptions requestRuntimeOptions = new RequestRuntimeOptions();
        requestRuntimeOptions.setDisableAutoEncoding(true);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        // when
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClientService.get().getHttpClient(any())).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(response);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
        when(response.getAllHeaders()).thenReturn(new Header[]{
                new BasicHeader("Content-Type", "application/json")
        });
        // then
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> requestService.get().executeHttpRequest(UUID.randomUUID(),
                        httpSaveRequest, null, Optional.empty(),
                        RequestTestUtils.generateContext(), null, requestRuntimeOptions));
        String expectedErrorMessage = "Illegal character in query at index 20: " + httpSaveRequest.getUrl() + "?"
                + keyWithIllegalCharacters + "=" + valueWithIllegalCharacters;
        assertEquals(expectedErrorMessage, exception.getTargetException().getMessage());
    }

    @Test
    public void executeCollectionWithNextRequest_setNextRequestToNull_shouldSkipAllNextRequests() {
        // given
        UUID testRunId = UUID.randomUUID();
        ExecutionCollectionRequestExecuteRequest executeCollectionRequest = new ExecutionCollectionRequestExecuteRequest();
        executeCollectionRequest.setTestRunId(testRunId);
        HttpRequest a = generateHttpRequest("a", UUID.randomUUID());
        HttpRequest b = generateHttpRequest("b", UUID.randomUUID());

        // when
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(true).nextRequest(null));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(nextRequestService.get().hasNextRequest(eq(testRunId)))
                .thenReturn(false)
                .thenReturn(true)
                .thenReturn(true);
        when(nextRequestService.get().getNextRequest(eq(testRunId)))
                .thenReturn(null);

        // then
        ExecuteStepResponse response = requestService.get().executeRequestWithRamAdapterLogging(executeCollectionRequest, a, null);
        Assertions.assertEquals(TestingStatuses.FAILED, response.getTestingStatus());

        response = requestService.get().executeRequestWithRamAdapterLogging(executeCollectionRequest, b, null);
        Assertions.assertEquals(TestingStatuses.SKIPPED, response.getTestingStatus());

        verify(ramService.get(), times(0)).closeCurrentSection(any() ,any());
        verify(ramService.get(), times(0)).openNewExecuteRequestSection(eq("a"), any());
        verify(nextRequestService.get(), times(0)).deleteNextRequest(eq(testRunId));
        Assertions.assertNotNull(response);
    }

    @Test
    public void executeCollectionWithNextRequest_repeatOneRequest() {
        // given
        UUID testRunId = UUID.randomUUID();
        ExecutionCollectionRequestExecuteRequest executeCollectionRequest = new ExecutionCollectionRequestExecuteRequest();
        executeCollectionRequest.setTestRunId(testRunId);
        executeCollectionRequest.setSection(new AtpCompaund());
        HttpRequest a = generateHttpRequest("a", UUID.randomUUID());
        CollectionRunRequest aCollRun = EntitiesGenerator.createCollRunRequest(testRunId, a.getId(), a.getName(), 1);
        CollectionRunStackRequest aInStack = EntitiesGenerator.createCollRunStackRequest(testRunId, a.getId(), a.getName(), 1);

        // when
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(true).nextRequest("a"));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(nextRequestService.get().hasNextRequest(eq(testRunId)))
                // execute "a" request
                .thenReturn(false).thenReturn(true)
                // rerun "a" request
                .thenReturn(false).thenReturn(false);
        when(nextRequestService.get().isSubCollectionExists(eq(testRunId))).thenReturn(true);
        when(nextRequestService.get().pop(eq(testRunId)))
                .thenReturn(aInStack)
                .thenReturn(null);
        when(nextRequestService.get().findInCollectionOrderNextRequest(eq(testRunId))).thenReturn(aCollRun);
        when(repository.get().findById(eq(a.getId()))).thenReturn(Optional.of(a));

        ExecuteStepResponse response = requestService.get().executeRequestWithRamAdapterLogging(executeCollectionRequest, a, null);

        verify(ramService.get(), times(1)).closeCurrentSection(any(), any());
        verify(ramService.get(), times(1)).openNewExecuteRequestSection(eq("a"), any());
        verify(nextRequestService.get(), times(1)).deleteNextRequest(eq(testRunId));
        Assertions.assertNotNull(response);
    }

    @Test
    public void executeCollectionWithNextRequest_setPreviouslyExecutedRequest() {
        // given
        UUID testRunId = UUID.randomUUID();
        ExecutionCollectionRequestExecuteRequest executeCollectionRequest = new ExecutionCollectionRequestExecuteRequest();
        executeCollectionRequest.setTestRunId(testRunId);
        executeCollectionRequest.setSection(new AtpCompaund());
        HttpRequest a = generateHttpRequest("a", UUID.randomUUID());
        HttpRequest b = generateHttpRequest("b", UUID.randomUUID());
        HttpRequest c = generateHttpRequest("c", UUID.randomUUID());

        CollectionRunRequest aCollRun = EntitiesGenerator.createCollRunRequest(testRunId, a.getId(), a.getName(), 1);

        CollectionRunStackRequest aInStack = EntitiesGenerator.createCollRunStackRequest(testRunId, a.getId(), a.getName(), 1);
        CollectionRunStackRequest bInStack = EntitiesGenerator.createCollRunStackRequest(testRunId, b.getId(), b.getName(), 2);
        CollectionRunStackRequest cInStack = EntitiesGenerator.createCollRunStackRequest(testRunId, c.getId(), c.getName(), 3);

        // when
        when(scriptService.get().evaluateRequestPreScript(any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(scriptService.get().evaluateRequestPostScript(any(), any(), any()))
                .thenReturn(new PostmanExecuteScriptResponseDto().hasNextRequest(false));
        when(nextRequestService.get().hasNextRequest(eq(testRunId)))
                // mock for execute a request
                .thenReturn(false).thenReturn(false)
                // mock for execute b request
                .thenReturn(false).thenReturn(false)
                // mock for execute c request
                .thenReturn(false).thenReturn(true)
                // repeat mock for execute a request
                .thenReturn(false).thenReturn(false)
                // repeat mock for execute b request
                .thenReturn(false).thenReturn(false)
                // repeat mock for execute c request
                .thenReturn(false).thenReturn(false);
        // should called only once
        when(nextRequestService.get().findInCollectionOrderNextRequest(eq(testRunId))).thenReturn(aCollRun);
        when(nextRequestService.get().isSubCollectionExists(eq(testRunId)))
                // mock for execute a request
                .thenReturn(false)
                // mock for execute b request
                .thenReturn(false)
                // mock for execute c request
                .thenReturn(true);
        when(nextRequestService.get().pop(eq(testRunId)))
                .thenReturn(aInStack)
                .thenReturn(bInStack)
                .thenReturn(cInStack).
                thenReturn(null);
        when(repository.get().findById(eq(a.getId()))).thenReturn(Optional.of(a));
        when(repository.get().findById(eq(b.getId()))).thenReturn(Optional.of(b));
        when(repository.get().findById(eq(c.getId()))).thenReturn(Optional.of(c));

        // then
        ExecuteStepResponse response = requestService.get().executeRequestWithRamAdapterLogging(executeCollectionRequest, a, null);
        Assertions.assertEquals(TestingStatuses.FAILED, response.getTestingStatus());
        response = requestService.get().executeRequestWithRamAdapterLogging(executeCollectionRequest, b, null);
        Assertions.assertEquals(TestingStatuses.FAILED, response.getTestingStatus());
        response = requestService.get().executeRequestWithRamAdapterLogging(executeCollectionRequest, c, null);
        Assertions.assertEquals(TestingStatuses.FAILED, response.getTestingStatus());

        verify(nextRequestService.get(), times(1)).deleteNextRequest(eq(testRunId));
        verify(nextRequestService.get(), times(1)).createNewSubCollection(eq(testRunId), any());
        verify(ramService.get(), times(3)).closeCurrentSection(any(), any());
        verify(ramService.get(), times(1)).openNewExecuteRequestSection(eq("a"), any());
        verify(ramService.get(), times(1)).openNewExecuteRequestSection(eq("b"), any());
        verify(ramService.get(), times(1)).openNewExecuteRequestSection(eq("c"), any());
        Assertions.assertNotNull(response);
    }

    @Test
    public void getResponseAsFileTest_responseAsJsonString_shouldReturnJsonFile() throws MimeTypeException, IOException {
        // given
        MockHttpServletResponse responseMock = new MockHttpServletResponse();
        responseMock.setOutputStreamAccessAllowed(true);
        RequestExecutionResponse response = EntitiesGenerator.generateRequestExecutionResponse();
        response.setResponseHeaders(Collections.singletonList(new RequestExecutionHeaderResponse("Content-Type", "application/json")));
        HttpRequestEntitySaveRequest request = generateRandomHttpRequestEntitySaveRequest();
        request.setName("Request \r\n\r\n with \r\r\r spaces \n\n\n  ");
        RequestExecution requestExecution = new RequestExecution();
        requestExecution.setName(request.getName());
        long timeStamp = 1707413388860L;
        requestExecution.setExecutedWhen(new Date(timeStamp));
        HttpRequestExecutionDetails details = new HttpRequestExecutionDetails(
                requestExecution, request, response, null, null);
        String expectedHeaderValue = String.format("attachment; filename=\"%s\"", "Request_with_spaces_" + timeStamp + ".json");
        // when
        when(detailsRepository.get().findByRequestExecutionByExecutionId(any())).thenReturn(Optional.of(details));
        requestService.get().writeResponseAsFile(UUID.randomUUID(), UUID.randomUUID(), responseMock);

        // then
        assertEquals(response.getBody(), responseMock.getContentAsString());
        assertTrue(responseMock.containsHeader(CONTENT_DISPOSITION));
        String headerValue = responseMock.getHeader(CONTENT_DISPOSITION);
        assertEquals(expectedHeaderValue, headerValue);
    }

    @Test
    public void getResponseAsFileTest_responseAsBinaryFile_shouldReturnBinaryFile() throws MimeTypeException, IOException {
        // given
        MockHttpServletResponse responseMock = new MockHttpServletResponse();
        responseMock.setOutputStreamAccessAllowed(true);
        RequestExecutionResponse response = EntitiesGenerator.generateRequestExecutionResponse();
        response.setResponseHeaders(Arrays.asList(
                new RequestExecutionHeaderResponse("Content-Disposition", "attachment; filename=\"my_file.xlsx\""),
                new RequestExecutionHeaderResponse("Content-Type", "application/octet-stream")));
        HttpRequestEntitySaveRequest request = generateRandomHttpRequestEntitySaveRequest();
        request.setName("Request \r\n\r\n with \r\r\r spaces \n\n\n  ");
        RequestExecution requestExecution = new RequestExecution();
        requestExecution.setName(request.getName());
        long timeStamp = 1707413388860L;
        requestExecution.setExecutedWhen(new Date(timeStamp));
        HttpRequestExecutionDetails details = new HttpRequestExecutionDetails(
                requestExecution, request, response, null, null);
        String expectedHeaderValue = String.format("attachment; filename=\"%s\"", "Request_with_spaces_" + timeStamp + ".xlsx");
        // when
        when(detailsRepository.get().findByRequestExecutionByExecutionId(any())).thenReturn(Optional.of(details));
        requestService.get().writeResponseAsFile(UUID.randomUUID(), UUID.randomUUID(), responseMock);

        // then
        assertArrayEquals(response.getBody().getBytes(StandardCharsets.UTF_8),
                responseMock.getContentAsByteArray());
        assertTrue(responseMock.containsHeader(CONTENT_DISPOSITION));
        String headerValue = responseMock.getHeader(CONTENT_DISPOSITION);
        assertEquals(expectedHeaderValue, headerValue);
    }
}
