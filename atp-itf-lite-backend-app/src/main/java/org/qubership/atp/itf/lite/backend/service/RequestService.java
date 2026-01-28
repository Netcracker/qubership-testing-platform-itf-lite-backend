/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.itf.lite.backend.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityNotFoundException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.util.function.TriConsumer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpException;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.itf.lite.backend.configuration.FeignClientsProperties;
import org.qubership.atp.itf.lite.backend.configuration.RequestResponseSizeProperties;
import org.qubership.atp.itf.lite.backend.converters.CurlFormatToRequestConverter;
import org.qubership.atp.itf.lite.backend.converters.RequestToCurlFormatConverter;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestExecutionDetailsRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestRepository;
import org.qubership.atp.itf.lite.backend.enums.ContextScope;
import org.qubership.atp.itf.lite.backend.enums.ScriptEngineExceptionType;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.enums.ValueType;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.exceptions.ItfLiteException;
import org.qubership.atp.itf.lite.backend.exceptions.file.ItfLiteMaxFileException;
import org.qubership.atp.itf.lite.backend.exceptions.jsengine.ItfLiteScriptEngineAtpDecryptException;
import org.qubership.atp.itf.lite.backend.exceptions.jsengine.ItfLiteScriptEngineAtpEncryptException;
import org.qubership.atp.itf.lite.backend.exceptions.jsengine.ItfLiteScriptEnginePostScriptExecutionException;
import org.qubership.atp.itf.lite.backend.exceptions.jsengine.ItfLiteScriptEnginePostmanSandboxContextException;
import org.qubership.atp.itf.lite.backend.exceptions.jsengine.ItfLiteScriptEnginePreScriptExecutionException;
import org.qubership.atp.itf.lite.backend.exceptions.jsengine.ItfLiteScriptEngineUnavailableException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteExportRequestException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteHttpRequestExecuteException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteHttpRequestIllegalMethodValueException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteRequestIllegalUrlEmptyValueException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteRequestQueryParamEncodingException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteRequestSizeLimitException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteResponseSizeLimitException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteUploadFileException;
import org.qubership.atp.itf.lite.backend.feign.clients.ItfPlainFeignClient;
import org.qubership.atp.itf.lite.backend.feign.dto.ConsoleLogDto;
import org.qubership.atp.itf.lite.backend.feign.dto.HttpResponseExceptionTypeEnum;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanExecuteScriptResponseDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanExecuteScriptResponseTestResultsInnerErrorDto;
import org.qubership.atp.itf.lite.backend.feign.dto.UIVelocityRequestBodyDto;
import org.qubership.atp.itf.lite.backend.feign.service.ItfFeignService;
import org.qubership.atp.itf.lite.backend.feign.service.JsScriptEngineService;
import org.qubership.atp.itf.lite.backend.feign.service.RamService;
import org.qubership.atp.itf.lite.backend.mdc.ItfLiteMdcField;
import org.qubership.atp.itf.lite.backend.model.RequestRuntimeOptions;
import org.qubership.atp.itf.lite.backend.model.api.dto.ResponseCookie;
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
import org.qubership.atp.itf.lite.backend.model.api.response.JsExecutionResult;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionHeaderResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestPreExecuteResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.collections.ContextEntity;
import org.qubership.atp.itf.lite.backend.model.api.response.collections.ExecuteStepResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.environments.System;
import org.qubership.atp.itf.lite.backend.model.api.response.itf.ItfParametersResolveResponse;
import org.qubership.atp.itf.lite.backend.model.context.SaveRequestResolvingContext;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;
import org.qubership.atp.itf.lite.backend.model.entities.FileBody;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.collection.run.CollectionRunRequest;
import org.qubership.atp.itf.lite.backend.model.entities.collection.run.CollectionRunStackRequest;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.qubership.atp.itf.lite.backend.model.entities.history.HttpRequestExecutionDetails;
import org.qubership.atp.itf.lite.backend.model.entities.history.RequestExecutionDetails;
import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;
import org.qubership.atp.itf.lite.backend.service.context.ExecutorContextEnricher;
import org.qubership.atp.itf.lite.backend.service.history.iface.DeleteHistoryService;
import org.qubership.atp.itf.lite.backend.service.history.iface.EntityHistoryService;
import org.qubership.atp.itf.lite.backend.service.rest.HttpClientService;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.qubership.atp.itf.lite.backend.utils.CookieUtils;
import org.qubership.atp.itf.lite.backend.utils.FileUtils;
import org.qubership.atp.itf.lite.backend.utils.RequestUtils;
import org.qubership.atp.itf.lite.backend.utils.StreamUtils;
import org.qubership.atp.itf.lite.backend.utils.UrlParsingUtils;
import org.qubership.atp.macros.core.processor.Evaluator;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.SerializationUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Striped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService extends CrudService<Request> implements EntityHistoryService<Request> {

    static final String ITF_URL_CONTEXT_SEPARATOR = "#/context/";
    static final String ITF_URL_PROJECT_SEPARATOR = "/project/";
    static final String ITF_CONFIGURATOR = "configurator";
    static final String ITF_EXECUTOR = "executor";
    static final String SESSION_ID = "Session-Id";
    static final String ORIGIN_HOST = "Origin-Host";
    static final String ORIGIN_HOST_VALUE = "originHostValue";
    static final String ORIGIN_REALM = "Origin-Realm";
    static final String ORIGIN_REALM_VALUE = "originRealmValue";
    static final String DPA_FORMAT = "<DPA>"
            + "<Origin-Host>${" + ORIGIN_HOST_VALUE + "}</Origin-Host>"
            + "<Origin-Realm>${" + ORIGIN_REALM_VALUE + "}</Origin-Realm>"
            + "<Result-Code>2001</Result-Code>"
            + "</DPA>";
    static final String RESULT_CODE_TAG = "Result-Code";
    static final String MESSAGE_TEXT_TAG = "Error-Message";
    static final String TAG_DELIMITER = ";";
    static final String XML_START_SYMBOL = "<";
    static final String XML_END_SYMBOL = ">";
    private static final int STRIPES = 100;
    private static final Striped<Lock> LOCK_STRIPED = Striped.lazyWeakLock(STRIPES);

    private final ExecutorContextEnricher executorContextEnricher;
    private final RequestRepository requestRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final CurlFormatToRequestConverter curlFormatToRequestConverter;
    private final RequestToCurlFormatConverter requestToCurlFormatConverter;
    private final ItfFeignService itfFeignService;
    private final ItfPlainFeignClient itfPlainFeignClient;
    private final FeignClientsProperties feignClientsProperties;
    private final GridFsService gridFsService;
    private final RequestExecutionHistoryService executionHistoryService;
    private final FolderService folderService;
    private final RequestAuthorizationService requestAuthorizationService;
    private final EnvironmentVariableService envParamService;
    private final RequestSpecificationService requestSpecificationService;
    private final MetricService metricService;
    private final HttpClientService httpClientService;
    private final MacrosService macrosService;
    private final JsScriptEngineService scriptService;
    private final ItfLiteFileService itfLiteFileService;
    private final TemplateResolverService templateResolverService;
    private final RamService ramService;
    private final DynamicVariablesService dynamicVariablesService;
    private final WritePermissionsService writePermissionsService;
    private final CookieService cookieService;
    private final RequestExecutionDetailsRepository detailsRepository;
    private final NextRequestService nextRequestService;
    private final RequestResponseSizeProperties requestResponseSizeProperties;
    private final DeleteHistoryService deleteHistoryService;
    @Value("${atp.itf.lite.max-size-file:10485760}")
    private long maxFileSize;

    @Override
    protected JpaRepository<Request, UUID> repository() {
        return requestRepository;
    }

    @Override
    public Request restore(Request entity) {
        return save(entity);
    }

    /**
     * Save request (and update folder children).
     *
     * @param request request to save
     * @return save request
     */
    @Override
    public Request save(Request request) {
        Request savedRequest = super.save(request);
        folderService.updateFolderChildren(savedRequest.getFolderId());
        return savedRequest;
    }

    /**
     * Get request by specified identifier.
     *
     * @param requestId request identifier
     * @return request
     */
    public Request getRequest(UUID requestId) {
        return getRequest(requestId, null);
    }

    /**
     * Get request by specified identifier.
     *
     * @param requestId request identifier
     * @param projectId project ID
     * @return request
     */
    public Request getRequest(UUID requestId, UUID projectId) {
        log.info("Find request by id {} and projectId {}", requestId, projectId);
        Request request = isNull(projectId) ? get(requestId)
                : getByProjectIdAndId(projectId, requestId);

        if (!request.isAutoCookieDisabled() && request instanceof HttpRequest) {
            List<Cookie> cookies = cookieService.getNotExpiredCookiesByUserIdAndProjectId(request.getProjectId());
            if (!CollectionUtils.isEmpty(cookies)) {
                HttpRequest httpRequest = (HttpRequest) request;
                if (StringUtils.isNotEmpty(httpRequest.getUrl())) {
                    try {
                        URI uri = new URI(httpRequest.getUrl());
                        HttpHeaderSaveRequest cookieHeader = cookieService.cookieListToRequestHeader(uri, cookies);
                        if (StringUtils.isNotEmpty(cookieHeader.getValue())) {
                            log.info("Get cookies header for request with id {}", request.getId());
                            httpRequest.setCookieHeader(cookieHeader);
                        }
                        // ignore the error in the url, because it is most likely caused by the fact
                        // that the url contains env variables, macros or context variables.
                    } catch (URISyntaxException ignore) {
                        log.warn("Syntax exception", ignore);
                    }
                }
            }
        }
        request.setHasWritePermissions(writePermissionsService
                .hasWritePermissions(request.getPermissionFolderId(), request.getProjectId()));
        request.setParentAuth(folderService.getParentAuth(request.getFolderId()));
        return request;
    }

    /**
     * Generate Authorization header by authorization type.
     *
     * @param authorization authorization
     * @return authorization header
     */
    @Nullable
    public RequestHeader generateAuthorizationHeader(@Nullable RequestAuthorization authorization) {
        if (nonNull(authorization)) {
            return requestAuthorizationService.generateAuthorizationHeader(authorization);
        }
        return null;
    }

    /**
     * Generate Authorization params by authorization type.
     *
     * @param authorization authorization
     * @return authorization params
     */
    @Nullable
    public List<RequestParam> generateAuthorizationParams(RequestAuthorization authorization) {
        if (nonNull(authorization)) {
            return requestAuthorizationService.generateAuthorizationParams(authorization);
        }
        return null;
    }

    /**
     * Try to get request by ID and project ID.
     */
    private Request getByProjectIdAndId(UUID projectId, UUID requestId) {
        Optional<Request> requestOptional = requestRepository.findByProjectIdAndId(projectId, requestId);
        if (requestOptional.isPresent()) {
            return requestOptional.get();
        } else {
            log.error("Unable find request by id = {} and projectId = {}", requestId, projectId);
            throw new AtpEntityNotFoundException("Request", requestId);
        }
    }

    /**
     * Get request settings by specified identifier.
     *
     * @param requestId request identifier
     * @return settings
     */
    public Settings getSettings(UUID requestId) {
        log.debug("Get request settings by requestId: {}", requestId);
        return modelMapper.map(getRequest(requestId), Settings.class);
    }

    /**
     * Collects set of permission ids from requests.
     * Need for PreAuthorize checks
     *
     * @param requestIds set of request ids
     * @return set of permission folder ids
     */
    public Set<UUID> getPermissionFolderIdsByRequestIds(Set<UUID> requestIds) {
        return requestRepository.findAllByIdIn(requestIds)
                .stream().filter(request -> request.getPermissionFolderId() != null)
                .map(Request::getPermissionFolderId)
                .collect(Collectors.toSet());
    }

    /**
     * Collects set of permission ids from requests.
     * Need for PreAuthorize checks
     *
     * @param requestIds set of request ids
     * @return set of permission folder ids
     */
    public Set<UUID> getPermissionFolderIdsByIdsWithModifiedWhen(Set<IdWithModifiedWhen> requestIds) {
        Set<UUID> ids = requestIds.stream().map(IdWithModifiedWhen::getId).collect(Collectors.toSet());
        return requestRepository.findAllByIdIn(ids)
                .stream().filter(request -> request.getPermissionFolderId() != null)
                .map(Request::getPermissionFolderId)
                .collect(Collectors.toSet());
    }

    /**
     * Check that request exists.
     *
     * @param requestId request identifier
     * @return true or exception
     */
    public boolean isRequestExists(UUID requestId) {
        log.debug("Check request existence by id {}", requestId);
        if (!isEntityExists(requestId)) {
            log.error("Failed to found {} entity with id: {}", RequestService.class.getName(), requestId);
            throw new AtpEntityNotFoundException(RequestService.class.getName(), requestId);
        }
        return true;
    }

    /**
     * Get binary file from cache or gridfs by request id.
     *
     * @param requestId request id.
     * @param response  http servlet for send response.
     */
    public void getRequestBinaryFile(UUID requestId, HttpServletResponse response) throws IOException {
        log.info("Get binary file for request with id {}", requestId);
        Request request = getRequest(requestId);
        FileData requestBinaryFile = itfLiteFileService.getRequestFileData(requestId, request.getModifiedWhen(),
                Constants.DEFAULT_BINARY_FILES_FOLDER);
        if (requestBinaryFile == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        ServletOutputStream responseOutputStream = response.getOutputStream();
        responseOutputStream.write(requestBinaryFile.getContent());
        response.setHeader(CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"",
                requestBinaryFile.getFileName()));
        response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
        response.setHeader(CONTENT_TYPE, requestBinaryFile.getContentType());
        log.debug("File with name {} and content type {} was downloaded.",
                requestBinaryFile.getFileName(), requestBinaryFile.getContentType());
        response.flushBuffer();
    }

    /**
     * Get file from cache or gridfs by file id.
     *
     * @param requestId request id.
     * @param fileId    file id.
     * @param response  http servlet for send response.
     */
    public void getFile(UUID requestId, UUID fileId, HttpServletResponse response) throws IOException {
        log.info("Get file by id {}", fileId);
        Request request = getRequest(requestId);
        FileData file = itfLiteFileService.getFileDataById(fileId, request.getModifiedWhen(),
                Constants.DEFAULT_FORM_DATA_FOLDER);
        if (file == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        ServletOutputStream responseOutputStream = response.getOutputStream();
        responseOutputStream.write(file.getContent());
        response.setHeader(CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"",
                file.getFileName()));
        response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
        response.flushBuffer();
    }

    /**
     * Get response as file.
     *
     * @param requestId request id.
     * @param executionId execution id.
     * @param response  http servlet for send response.
     */
    public void writeResponseAsFile(UUID requestId, UUID executionId, HttpServletResponse response)
            throws IOException {
        log.info("Get response as file for requestId = {} by executionId = {}", requestId, executionId);

        Optional<RequestExecutionDetails> detailsOptional =
                detailsRepository.findByRequestExecutionByExecutionId(executionId);
        if (!detailsOptional.isPresent()) {
            log.warn("Details information for request id {} not found by EXECUTION ID {}.", requestId, executionId);
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        RequestExecutionDetails details = detailsOptional.get();
        HttpRequestExecutionDetails httpRequestExecutionDetails = (HttpRequestExecutionDetails) details;

        Map<String, List<String>> responseHeaders = httpRequestExecutionDetails.getResponseHeaders();
        String extension = getExtensionByContentTypeHeader(responseHeaders);
        if (StringUtils.isEmpty(extension)) {
            String binaryFileName = getResponseFilenameFromResponseHeaders(responseHeaders);
            extension = FilenameUtils.getExtension(binaryFileName);
            if (!StringUtils.isEmpty(extension)) {
                extension = "." + extension;
            }
        }

        if (StringUtils.isEmpty(extension)) {
            extension = ".txt";
        }

        String rawFileName = details.getRequestExecution().getName().trim().replaceAll("\\s+", "_")
                + "_" + details.getRequestExecution().getExecutedWhen().getTime();
        String safeFileName = rawFileName.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"",
                safeFileName + extension));
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setHeader("X-Content-Type-Options", "nosniff");

        ServletOutputStream responseOutputStream = response.getOutputStream();
        byte[] responseContentAsByteArray = details.getResponseBodyByte();
        responseOutputStream.write(responseContentAsByteArray);
        response.flushBuffer();
    }

    /**
     * Upload binary file to cache and gridfs by request id.
     *
     * @param requestId request id.
     * @param file      file for upload.
     */
    public void uploadBinaryFile(UUID requestId, MultipartFile file) {
        log.info("Upload binary file {} for request with id {}", file.getOriginalFilename(), requestId);
        try {
            Request request = getRequest(requestId);
            FileBody fileBody = itfLiteFileService
                    .uploadFileForRequest(requestId, Constants.DEFAULT_BINARY_FILES_FOLDER, file);
            ((HttpRequest) request).getBody().setBinaryBody(fileBody);
            save(request);
        } catch (IOException e) {
            log.error("Failed to load the file with name {} for request with id {}",
                    file.getOriginalFilename(), requestId.toString(), e);
            throw new ItfLiteUploadFileException(file.getOriginalFilename(), requestId.toString());
        }
    }

    /**
     * Get request by specified identifier and project id.
     *
     * @param requestId request identifier
     * @return request
     */
    public Request getRequestByProjectIdAndRequestId(UUID projectId, UUID requestId) {
        log.info("Find request by projectId {} and request id {}", projectId, requestId);
        return requestRepository.findByProjectIdAndId(projectId, requestId).orElseThrow(() -> {
            log.error("Failed to find request by projectId = {} and id = {}", projectId, requestId);
            return new EntityNotFoundException(String.format("Failed to find request by projectId = %s and id = %s",
                    projectId, requestId));
        });
    }

    /**
     * Get all requests specified by project.
     *
     * @param projectId project identifier
     * @return collection of requests
     */
    public Collection<Request> getAllRequests(UUID projectId, UUID folderId) {
        log.info("Find all requests, filters: [projectId: {}, folderId: {}]", projectId, folderId);
        Specification<Request> requestSpecification = Specification
                .where(requestSpecificationService.generateSpecificationToFilterRequestsByProjectIdFolderIdsRequestIds(
                        projectId,
                        folderId == null ? null : Collections.singleton(folderId),
                        null));
        return requestRepository.findAll(requestSpecification);
    }

    /**
     * Gets collection of requests by projectId, folderIds and requestIds.
     *
     * @param projectId  project id
     * @param folderIds  folder ids
     * @param requestIds request ids
     * @return collection of requests
     */
    public List<Request> getAllRequestsByProjectIdFolderIdsRequestIds(UUID projectId, Set<UUID> folderIds,
                                                                      Set<UUID> requestIds) {
        log.info("Find all requests, filters: [projectId: {}, folderIds: {}, requestIds: {}]",
                projectId, folderIds, requestIds);
        Specification<Request> requestSpecification = Specification
                .where(requestSpecificationService.generateSpecificationToFilterRequestsByProjectIdFolderIdsRequestIds(
                        projectId, folderIds, requestIds));
        return requestRepository.findAll(requestSpecification);
    }

    /**
     * Create request.
     *
     * @param requestCreationRequest creation request
     * @return created request
     */
    public Request createRequest(RequestEntityCreateRequest requestCreationRequest) {
        Request request = modelMapper.map(requestCreationRequest, HttpRequest.class);
        setOrder(request);
        folderService.updateFolderChildren(request.getFolderId());
        return save(request);
    }

    /**
     * Create request.
     */
    public Request createRequest(Request request) {
        setOrder(request);
        return save(request);
    }

    /**
     * Create requests.
     */
    public List<Request> createRequests(List<Request> requests) {
        requests.forEach(this::setOrder);

        return saveAll(requests);
    }

    /**
     * Save request.
     *
     * @param requestId                request id
     * @param requestEntitySaveRequest request data
     * @param fileInfo                 info about file
     * @return updated request
     */
    public Request saveRequest(UUID requestId, RequestEntitySaveRequest requestEntitySaveRequest,
                               List<MultipartFile> files, Optional<FileBody> fileInfo) {
        checkFilesSize(files);
        log.debug("Check if request with requestId {} exists in project {}",
                requestId, requestEntitySaveRequest.getProjectId());
        AuthorizationSaveRequest authorization = requestEntitySaveRequest.getAuthorization();
        if (nonNull(authorization)) {
            requestAuthorizationService.encryptAuthorizationParameters(authorization);
        }
        requestEntitySaveRequest.normalize();
        // EntityNotFoundException will be thrown if not found
        Request request = get(requestId);
        HttpRequest httpRequest = (HttpRequest) request;
        HttpRequestEntitySaveRequest saveRequest = (HttpRequestEntitySaveRequest) requestEntitySaveRequest;
        retainFormData(httpRequest.getBody(), saveRequest.getBody(), httpRequest.getId(), false);

        modelMapper.map(requestEntitySaveRequest, request);
        removeAutoGeneratedHeadersExcept(httpRequest, CONTENT_TYPE);
        removeAutoGeneratedParamsExcept(httpRequest);
        updateHeadersFields(httpRequest.getRequestHeaders());
        updateParametersFields(httpRequest.getRequestParams());
        dynamicVariablesService.enrichPreScriptsByDynamicVariables(httpRequest);
        prepareFormDataRequest(httpRequest.getBody(), files, httpRequest.getId(), false);
        if (fileInfo.isPresent()) {
            if (Objects.isNull(httpRequest.getBody())) {
                httpRequest.setBody(new RequestBody(fileInfo.get(), RequestBodyType.Binary));
            } else {
                httpRequest.getBody().setBinaryBody(fileInfo.get());
            }
        }

        folderService.updateAuthorizationFolderId(request);
        return save(request);
    }

    /**
     * Removes all auto generated headers except provided.
     */
    private void removeAutoGeneratedHeadersExcept(HttpRequest httpRequest, String... except) {
        final Set<String> exceptions = Sets.newHashSet(except);
        httpRequest.getRequestHeaders()
                .removeIf(header -> !exceptions.contains(header.getKey()) && header.isGenerated());
    }

    /**
     * Removes all auto generated params except provided.
     */
    private void removeAutoGeneratedParamsExcept(HttpRequest httpRequest, String... except) {
        final Set<String> exceptions = Sets.newHashSet(except);
        httpRequest.getRequestParams()
                .removeIf(param -> !exceptions.contains(param.getKey()) && param.isGenerated());
    }

    /**
     * Removes files what not used in request now.
     */
    public void retainFormData(RequestBody body,
                               RequestBody bodyToSave,
                               UUID id,
                               boolean isSnapshot) {
        RequestBodyType savedRequestBodyType = Objects.isNull(body) ? null :
                body.getType();
        RequestBodyType requestToSaveBodyType = Objects.isNull(bodyToSave) ? null :
                bodyToSave.getType();
        if (RequestBodyType.FORM_DATA.equals(requestToSaveBodyType)) {
            if (RequestBodyType.FORM_DATA.equals(savedRequestBodyType)) {
                // compare old and new files in form-data
                // and remove old not used files
                List<FormDataPart> savedFormDataParts = body.getFormDataBody();
                List<FormDataPart> formDataPartsToSave = bodyToSave.getFormDataBody();
                if (!CollectionUtils.isEmpty(savedFormDataParts)) {
                    if (CollectionUtils.isEmpty(formDataPartsToSave)) {
                        // all old form-data parts removed
                        // remove all files by requestId
                        log.debug("RequestToSave not contain files - remove old form-data files");
                        if (isSnapshot) {
                            gridFsService.removeFileBySessionId(id);
                        } else {
                            gridFsService.removeFileByRequestId(id);
                        }

                    } else {
                        // form-data changed
                        // get fileId what not used now and remove it
                        List<UUID> savedFileIds = getUniqueFileIds(body.getFormDataBody());
                        savedFileIds.removeAll(getUniqueFileIds(bodyToSave.getFormDataBody()));
                        log.info("Remove files with id in {}", savedFileIds);
                        savedFileIds.forEach(gridFsService::removeFileByFileId);
                    }
                }
            }
        } else {
            if (RequestBodyType.FORM_DATA.equals(savedRequestBodyType)) {
                // request body type changed from FORM_DATA to another type
                // removed all used files
                log.debug("Request to save change body type - remove old request files (requestId {})", id);
                if (isSnapshot) {
                    gridFsService.removeFileBySessionId(id);
                } else {
                    gridFsService.removeFileByRequestId(id);
                }
                body.setFormDataBody(null);
            }
        }

    }

    private List<UUID> getUniqueFileIds(List<FormDataPart> formData) {
        return formData.stream()
                .filter(fd -> ValueType.FILE.equals(fd.getType()))
                .map(FormDataPart::getFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Check files size.
     * @throws ItfLiteMaxFileException for file with size > maxFileSize.
     */
    public void checkFilesSize(List<MultipartFile> files) {
        if (!CollectionUtils.isEmpty(files)) {
            files.forEach(file -> {
                if (file.getSize() > maxFileSize) {
                    throw new ItfLiteMaxFileException(file.getName());
                }
            });
        }
    }

    /**
     * Remove old formDta files, add new files and set file UUIDs.
     */
    public List<UUID> prepareFormDataRequest(RequestBody requestBody,
                                             List<MultipartFile> files,
                                             UUID id,
                                             boolean isSnapshotFile) {
        List<UUID> fileIds = new ArrayList<>();
        if (Objects.nonNull(requestBody)) {
            List<FormDataPart> formDataParts = requestBody.getFormDataBody();
            if (!CollectionUtils.isEmpty(formDataParts) && !CollectionUtils.isEmpty(files)) {
                // Creates fileId for new added files
                // set it to fileId field
                // and save file with fileId in metadata
                int i = 0;
                for (FormDataPart fdp : formDataParts) {
                    if (ValueType.FILE.equals(fdp.getType()) && Objects.isNull(fdp.getFileId()) && i < files.size()) {
                        MultipartFile f = files.get(i);
                        if (Objects.nonNull(f)) {
                            UUID fileId = UUID.randomUUID();
                            fileIds.add(fileId);
                            fdp.setFileId(fileId);
                            fdp.setFileSize(f.getSize());
                            try {
                                if (isSnapshotFile) {
                                    gridFsService.saveFileBySessionId(LocalDateTime.now().toString(), id,
                                            f.getInputStream(), f.getOriginalFilename(), fileId);
                                } else {
                                    gridFsService.saveFileByRequestId(LocalDateTime.now().toString(), id,
                                            f.getInputStream(), f.getOriginalFilename(), fileId);
                                }

                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        i++;
                    }
                }
            }
        }
        return fileIds;
    }

    private <S, O> List<S> retainEntities(List<O> opposite, Function<O, S> mapper) {
        return opposite.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    /**
     * Edit request.
     * Request name will be updated.
     *
     * @param requestId                  request id
     * @param requestEntityCreateRequest request data
     * @return edited request
     */
    public Request editRequest(UUID requestId, RequestEntityEditRequest requestEntityCreateRequest) {
        log.debug("Check if request with requestId {} exists in project {}",
                requestId, requestEntityCreateRequest.getProjectId());
        // EntityNotFoundException will be thrown if not found
        Request request = get(requestId);
        modelMapper.map(requestEntityCreateRequest, request);

        return save(request);
    }

    /**
     * Copy request to different folder.
     *
     * @param requestEntityCopyRequest request copy data
     * @return new request copied from request with provided id
     */
    public Request copyRequest(UUID requestId, RequestEntityCopyRequest requestEntityCopyRequest) {
        log.debug("Check if request with requestId {} exists in project {}", requestId,
                requestEntityCopyRequest.getProjectId());
        Request request = get(requestId);

        Request copyRequest = RequestUtils.copyRequestFromRequest(request);
        UUID targetFolderId = requestEntityCopyRequest.getFolderId();
        Folder targetFolder = targetFolderId == null
                ? null
                : folderService.getFolder(targetFolderId);
        setFolderFields(copyRequest, targetFolder);
        setOrder(copyRequest);

        log.debug("Get requests by folder id {}", targetFolderId);
        List<Request> folderRequests = requestRepository
                .findAllByProjectIdAndFolderId(requestEntityCopyRequest.getProjectId(),
                        requestEntityCopyRequest.getFolderId());

        addPostfixIfNameIsTaken(folderRequests, copyRequest);

        folderService.updateAuthorizationFolderId(copyRequest);
        Request savedRequest = save(copyRequest);
        HttpRequest savedHttpRequest = (HttpRequest) savedRequest;
        if (nonNull(savedHttpRequest.getBody())) {
            if (RequestBodyType.Binary.equals(savedHttpRequest.getBody().getType())) {
                try {
                    Optional<FileBody> fileInfo = itfLiteFileService
                            .copyFileForCopiedRequest(requestId, request.getModifiedWhen(),
                                    savedRequest.getId(), Constants.DEFAULT_BINARY_FILES_FOLDER);
                    if (fileInfo.isPresent()) {
                        savedHttpRequest.getBody().setBinaryBody(fileInfo.get());
                        savedRequest = save(savedHttpRequest);
                    }
                } catch (IOException e) {
                    log.error("File wasn't copied for request {}", savedRequest.getId(), e);
                }
            } else if (RequestBodyType.FORM_DATA.equals(savedHttpRequest.getBody().getType())) {
                if (!CollectionUtils.isEmpty(savedHttpRequest.getBody().getFormDataBody())) {
                    copyFormDataFiles(savedHttpRequest.getBody().getFormDataBody(), savedHttpRequest.getId());
                    savedRequest = save(savedHttpRequest);
                }
            }
        }

        return savedRequest;
    }

    private void copyFormDataFiles(List<FormDataPart> formData, UUID newRequestId) {
        formData.stream()
                .filter(fdp -> ValueType.FILE.equals(fdp.getType()) && nonNull(fdp.getFileId()))
                .forEach(fdp -> fdp.setFileId(gridFsService.copyFileById(fdp.getFileId(), newRequestId)));
    }

    /**
     * Copy set of requests to different folder.
     *
     * @param requestEntitiesCopyRequest request copy data
     */
    @Transactional
    public void copyRequests(RequestEntitiesCopyRequest requestEntitiesCopyRequest) {
        UUID projectId = requestEntitiesCopyRequest.getProjectId();

        Set<UUID> requestIds = requestEntitiesCopyRequest.getRequestIds();
        log.debug("Check if requests with requestIds {} exist in project {}", requestIds, projectId);
        List<Request> requests = requestRepository.findAllByProjectIdAndIdIn(projectId, requestIds);

        log.debug("Get requests by folder id {}", requestEntitiesCopyRequest.getFolderId());
        UUID targetFolderId = requestEntitiesCopyRequest.getFolderId();
        List<Request> folderRequests = requestRepository
                .findAllByProjectIdAndFolderId(requestEntitiesCopyRequest.getProjectId(),
                        targetFolderId);

        Folder targetFolder = targetFolderId == null ? null : folderService.getFolder(targetFolderId);

        // Generate ids for copied requests
        Set<UUID> foundRequestIds = StreamUtils.extractIds(requests);
        List<Request> copyRequests = new ArrayList<>();
        for (Request request : requests) {
            MdcUtils.put(ItfLiteMdcField.REQUEST_ID.toString(), request.getId());
            Request copiedRequest = RequestUtils.copyRequestFromRequest(request);
            setFolderFields(copiedRequest, targetFolder);
            addPostfixIfNameIsTaken(folderRequests, copiedRequest);
            if (TransportType.REST.equals(request.getTransportType())
                    && ((HttpRequest) request).getBody() != null) {
                HttpRequest httpRequest = (HttpRequest) request;
                HttpRequest httpCopiedRequest = (HttpRequest) copiedRequest;
                if (RequestBodyType.Binary.equals(httpRequest.getBody().getType())) {
                    setOrder(httpCopiedRequest);
                    HttpRequest savedRequest = (HttpRequest) save(httpCopiedRequest);
                    try {
                        log.info("Coping request with binary file {} to folder {}",
                                request.getId(), requestEntitiesCopyRequest.getFolderId());
                        Optional<FileBody> fileInfo = itfLiteFileService
                                .copyFileForCopiedRequest(request.getId(), request.getModifiedWhen(),
                                        savedRequest.getId(), Constants.DEFAULT_BINARY_FILES_FOLDER);
                        if (fileInfo.isPresent()) {
                            savedRequest.getBody().setBinaryBody(fileInfo.get());
                            save(savedRequest);
                        }
                    } catch (IOException e) {
                        log.error("File wasn't copied for request {}", savedRequest.getId(), e);
                    }
                } else if (RequestBodyType.FORM_DATA.equals(httpCopiedRequest.getBody().getType())
                        && !CollectionUtils.isEmpty(httpCopiedRequest.getBody().getFormDataBody())) {
                    setOrder(httpCopiedRequest);
                    HttpRequest savedRequest = (HttpRequest) save(httpCopiedRequest);
                    copyFormDataFiles(savedRequest.getBody().getFormDataBody(), savedRequest.getId());
                    save(savedRequest);
                } else {
                    copyRequests.add(copiedRequest);
                }
            }
            MDC.remove(ItfLiteMdcField.REQUEST_ID.toString());
        }
        copyRequests.forEach(req -> {
            setOrder(req);
            folderService.updateAuthorizationFolderId(req);
        });

        log.info("Coping requests [{}] to folder {}", foundRequestIds, requestEntitiesCopyRequest.getFolderId());
        saveAll(copyRequests);
    }

    /**
     * Copying diameter request dictionary if exists.
     *
     * @param fromRequestId source request id
     * @param toRequestId   target request id
     */
    public UUID copyDictionary(UUID fromRequestId, UUID toRequestId) {
        Optional<FileData> dictionary = gridFsService.downloadFile(fromRequestId);
        if (dictionary.isPresent()) {
            InputStream dictionaryInputStream = new ByteArrayInputStream(dictionary.get().getContent());
            return gridFsService.saveDictionaryByRequestId(LocalDateTime.now().toString(), toRequestId,
                    dictionaryInputStream, dictionary.get().getFileName());
        }
        return null;
    }

    /**
     * Move request to different folder.
     *
     * @param requestEntityMoveRequest request copy data
     * @return updated request
     */
    public Request moveRequest(UUID requestId, RequestEntityMoveRequest requestEntityMoveRequest) {
        log.debug("Check if request with requestId {} exists in project {}", requestId,
                requestEntityMoveRequest.getProjectId());
        Request request = get(requestId);
        UUID targetFolderId = requestEntityMoveRequest.getFolderId();
        Folder targetFolder = targetFolderId == null ? null : folderService.getFolder(targetFolderId);
        setFolderFields(request, targetFolder);
        setOrder(request);
        folderService.updateAuthorizationFolderId(request);
        return save(request);
    }

    /**
     * Move requests to different folder.
     *
     * @param requestEntityMoveRequest request copy data
     */
    @Transactional
    public void moveRequests(RequestEntitiesMoveRequest requestEntityMoveRequest) {
        UUID projectId = requestEntityMoveRequest.getProjectId();

        Set<UUID> requestIds = StreamUtils.extractIds(requestEntityMoveRequest.getRequestIds(),
                IdWithModifiedWhen::getId);
        log.debug("Check if requests with requestIds {} exist in project {}", requestIds, projectId);
        List<Request> requests = requestRepository.findAllByProjectIdAndIdIn(projectId, requestIds);
        UUID targetFolderId = requestEntityMoveRequest.getFolderId();
        Folder targetFolder = targetFolderId == null ? null : folderService.getFolder(targetFolderId);

        requests.forEach(request -> {
            setFolderFields(request, targetFolder);
            setOrder(request);
            folderService.updateAuthorizationFolderId(request);
        });

        Set<UUID> foundRequestIds = StreamUtils.extractIds(requests);
        log.info("Moving requests [{}] to folder {}", foundRequestIds, requestEntityMoveRequest.getFolderId());
        saveAll(requests);
    }

    private void setFolderFields(Request request, Folder folder) {
        request.setFolderId(folder == null ? null : folder.getId());
        request.setPermissionFolderId(folder == null ? null : folder.getPermissionFolderId());
    }

    /**
     * Delete request.
     *
     * @param requestId request id
     */
    @Transactional
    public void deleteRequest(UUID requestId) {
        log.debug("Check that request with requestId {} exists", requestId);
        // EntityNotFoundException will be thrown if not found
        Request request = get(requestId);
        delete(request);
        log.debug("Delete files from gridFs by request id {}", requestId);
        gridFsService.removeAllFilesByRequestId(requestId);
        log.debug("Delete javers history snapshots by request id = {}", requestId);
        deleteHistoryService.deleteSnapshotsByEntityIds(Sets.newHashSet(requestId));
    }

    /**
     * Delete requests.
     *
     * @param requestEntitiesBulkDelete request with ids
     */
    @Transactional
    public void bulkDeleteRequests(RequestEntitiesBulkDelete requestEntitiesBulkDelete) {
        Set<UUID> requestsAndFoldersUuids = requestEntitiesBulkDelete.getRequestIds();
        log.debug("Check if requests with requestIds {} exist in project", requestsAndFoldersUuids);
        List<Request> requestsToDelete = requestRepository.findAllById(requestsAndFoldersUuids);
        List<Folder> foldersToDelete = folderService.getFoldersByIds(requestsAndFoldersUuids);

        Set<UUID> foundRequestIds = StreamUtils.extractIds(requestsToDelete);
        log.info("Delete requests with ids: {}", foundRequestIds);
        deleteByEntities(requestsToDelete);

        Set<UUID> folderIdsToDelete = StreamUtils.extractIds(foldersToDelete);
        log.info("Remove folder with ids: {}", folderIdsToDelete);
        FolderDeleteRequest folderDeleteRequest =
                new FolderDeleteRequest(folderIdsToDelete, requestEntitiesBulkDelete.getProjectId());
        folderService.deleteFolders(folderDeleteRequest);

        foundRequestIds.forEach(id -> {
            log.debug("Delete files from gridFs by request id {}", id);
            gridFsService.removeAllFilesByRequestId(id);
        });

        log.debug("Delete javers history snapshots by request ids: {}", foundRequestIds);
        deleteHistoryService.deleteSnapshotsByEntityIds(foundRequestIds);
    }

    /**
     * Add postfix "Copy" if request with the same name already exists in folder.
     *
     * @param folderRequests list of requests under folder
     * @param request        request
     */
    public void addPostfixIfNameIsTaken(List<Request> folderRequests, Request request) {
        while (folderRequests.stream().anyMatch(folderRequest -> folderRequest.getName().equals(request.getName()))) {
            request.setName(request.getName() + Constants.COPY_POSTFIX);
        }
    }

    /**
     * Export request to cURL format.
     *
     * @param requestId request id
     * @return StringBuilder with cURL format request
     */
    public String exportRequest(UUID requestId, UUID environmentId, String context,
                                List<ContextVariable> contextVariables) throws URISyntaxException {
        log.debug("Check if request with requestId {} exists", requestId);
        // EntityNotFoundException will be thrown if not found
        Request request = get(requestId);

        try {
            HttpRequestEntitySaveRequest httpRequest = modelMapper.map(request, HttpRequestEntitySaveRequest.class);
            UUID projectId = request.getProjectId();
            Evaluator evaluator = macrosService.createMacrosEvaluator(projectId);
            SaveRequestResolvingContext resolvingContext = SaveRequestResolvingContext.builder().build();
            if (Objects.nonNull(environmentId)) {
                Map<String, Object> environmentVariables = SaveRequestResolvingContext.parseSystems(
                        envParamService.getEnvironmentSystemsById(environmentId));
                resolvingContext.setEnvironmentVariables(environmentVariables);
                resolvingContext.getEnvironment().putAll(environmentVariables);
            }
            resolvingContext.parseAndClassifyContextVariables(contextVariables);
            templateResolverService.resolveTemplatesWithOrder(httpRequest, resolvingContext, evaluator);
            templateResolverService.processEncryptedValues(httpRequest, true);
            RequestBody body = httpRequest.getBody();
            if (body != null && RequestBodyType.Velocity.equals(body.getType())) {
                String response = processVelocity(httpRequest.getProjectId(), body.getContent(), context);
                httpRequest.getBody().setContent(response);
            }

            AuthorizationSaveRequest authorization = httpRequest.getAuthorization();
            if (nonNull(authorization)) {
                requestAuthorizationService.processRequestAuthorization(projectId, httpRequest, null, environmentId,
                        evaluator, resolvingContext);
            }

            return requestToCurlFormatConverter.convertRequestToCurlStringBuilder(httpRequest);
        } catch (AtpException ex) {
            log.error("Failed to export request to cURL format", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to export request to cURL format", ex);
            throw new ItfLiteExportRequestException();
        }
    }

    /**
     * Import cURL string to Request.
     *
     * @param importRequest import request with requestId and curl string
     * @return Request
     */
    public Request importRequest(CurlStringImportRequest importRequest) {
        log.debug("Check if request with requestId {} exists", importRequest.getRequestId());
        // EntityNotFoundException will be thrown if not found
        Request request = get(importRequest.getRequestId());
        HttpRequest httpRequest = (HttpRequest) request;
        return curlFormatToRequestConverter.convertCurlStringToRequest(httpRequest, importRequest.getRequestString());
    }

    /**
     * Execute request with RAM Adapter logging.
     *
     * @param request request
     */
    public ExecuteStepResponse executeRequestWithRamAdapterLogging(
            ExecutionCollectionRequestExecuteRequest requestExecuteRequest,
            Request request, UUID environmentId) {

        Map<String, Object> context = requestExecuteRequest.getContext();
        UUID testRunId = requestExecuteRequest.getTestRunId();
        if (nextRequestService.hasNextRequest(testRunId)) {
            if (isRequestMatchesNextRequest(testRunId, request.getId(), request.getName())) {
                nextRequestService.deleteNextRequest(testRunId);
            } else {
                log.info("Current request not matched next request - skip execution. (TestRunId: {}, requestId: {})",
                        testRunId, request.getId());
                ramService.writeMessage("Request execution was skipped because the nextRequest is specified, "
                        + "which does not match the current request", TestingStatuses.SKIPPED);
                return createSkippedExecuteStepResponse(context);
            }

        }

        SaveRequestResolvingContext resolvingContext = SaveRequestResolvingContext.builder()
                .globals(SaveRequestResolvingContext.parseScope(context, ContextScope.GLOBALS))
                .collectionVariables(SaveRequestResolvingContext.parseScope(context, ContextScope.COLLECTION))
                .environment(SaveRequestResolvingContext.parseScope(context, ContextScope.ENVIRONMENT))
                .iterationData(SaveRequestResolvingContext.parseScope(context, ContextScope.DATA))
                .variables(SaveRequestResolvingContext.parseScope(context, ContextScope.LOCAL_VARIABLES))
                .build();

        if (nonNull(context)) {
            Object envId = context.get(Constants.ENV_ID_KEY);
            if (nonNull(envId)) {
                List<System> systems = envParamService.getEnvironmentSystemsById(UUID.fromString(envId.toString()));
                Map<String, Object> environmentVariables = SaveRequestResolvingContext.parseSystems(systems);
                resolvingContext.setEnvironmentVariables(environmentVariables);
                resolvingContext.getEnvironment().putAll(environmentVariables);
            }
        }

        ExecuteStepResponse response = executeRequestWithRamAdapterLogging(requestExecuteRequest, request,
                environmentId, resolvingContext);

        if (nextRequestService.isSubCollectionExists(testRunId)) {
            // update current execution status and start new sub collection
            ramService.updateExecutionStatus(requestExecuteRequest, ExecutionStatuses.FINISHED);
            List<ExecuteStepResponse> responses =
                    runSubCollection(requestExecuteRequest, environmentId, resolvingContext);
            //update final response from sub collection
            if (!responses.isEmpty()) {
                //update context. Get from latest request in sub collection
                response.setContext(responses.get(responses.size() - 1).getContext());
                //update testing status. If any failed then root also failed.
                if (responses.stream().anyMatch(r -> TestingStatuses.FAILED.equals(r.getTestingStatus()))) {
                    response.setTestingStatus(TestingStatuses.FAILED);
                }
            }
        }
        return response;
    }

    /**
     * Execute request with RAM Adapter logging.
     *
     * @param requestExecuteRequest execution request
     * @param request request to execute
     * @param environmentId environment id
     * @param resolvingContext context variables
     * @return result of request execution
     */
    private ExecuteStepResponse executeRequestWithRamAdapterLogging(
            ExecutionCollectionRequestExecuteRequest requestExecuteRequest,
            Request request, UUID environmentId, SaveRequestResolvingContext resolvingContext) {

        boolean statusPassed;
        final UUID testRunId = requestExecuteRequest.getTestRunId();
        if (nextRequestService.isExecutionLimitExceeded(testRunId, request.getId())) {
            log.info("Execution count limit was exceeded. Skip collection execution (requestId: {})", request.getId());
            nextRequestService.setNextRequest(testRunId, null);
            ramService.writeMessage("Request execution limit has been exceeded. Collection execution skipped",
                    TestingStatuses.SKIPPED);
            return createSkippedExecuteStepResponse(resolvingContext.mergeWithScopePrefixes());
        }

        try {
            final UUID transportLogRecordId = UUID.randomUUID();
            RequestEntitySaveRequest saveRequest = objectMapper.readValue(
                    objectMapper.writeValueAsString(request), RequestEntitySaveRequest.class);

            if (nonNull(requestExecuteRequest.getContext())) {
                Object envId = requestExecuteRequest.getContext().get(Constants.ENV_ID_KEY);
                if (nonNull(envId)) {
                    saveRequest.setEnvironmentId(UUID.fromString(envId.toString()));
                }
            }

            Optional<MultipartFile> file;
            HttpRequestEntitySaveRequest httpSaveRequest = (HttpRequestEntitySaveRequest) saveRequest;
            if (nonNull(httpSaveRequest.getBody())
                    && RequestBodyType.Binary.equals(httpSaveRequest.getBody().getType())) {
                file = itfLiteFileService.getFileAsMultipartFileByRequestId(
                        saveRequest.getId(), request.getModifiedWhen(),
                        Constants.DEFAULT_BINARY_FILES_FOLDER);
            } else {
                file = Optional.empty();
            }
            if (!httpSaveRequest.isAutoCookieDisabled()) {
                httpSaveRequest.setCookies(cookieService.getAllByExecutionRequestIdAndTestRunId(
                        requestExecuteRequest.getExecutionRequestId(), requestExecuteRequest.getTestRunId()));
            }
            TriConsumer<RequestEntitySaveRequest, RequestExecutionResponse, Exception> afterRestExecution =
                    (req, resp, error) -> {
                        TestingStatuses status = error == null
                                ? TestingStatuses.PASSED : TestingStatuses.FAILED;
                        ramService.writeRequestExecutionResult(transportLogRecordId, req, resp, error, status);
                    };
            BiFunction<PostmanExecuteScriptResponseDto, Boolean, JsExecutionResult> scriptExecution =
                    ramService::writeTestsResults;
            BiConsumer<RequestEntitySaveRequest, List<ConsoleLogDto>> logConsoleLogs =
                    (req, consoleLogs) -> {
                        String prescript = req == null || req.getPreScripts() == null ? "" : req.getPreScripts();
                        String postscript = req == null || req.getPostScripts() == null ? "" : req.getPostScripts();
                        ramService.writeConsoleLogs(transportLogRecordId, prescript, postscript, consoleLogs);
                    };
            Function<RequestExecutionResponse, UUID> setExecutionId = (resp) -> transportLogRecordId;
            RequestExecutionResponse response = executeRequest(saveRequest, null, file,
                    resolvingContext, afterRestExecution, scriptExecution,
                    environmentId, null, setExecutionId, logConsoleLogs, getRuntimeOptions(request),
                    getUpdateNextRequestFunc(testRunId));
            statusPassed = response.isTestsPassed();

            if (!request.isAutoCookieDisabled() && saveRequest instanceof HttpRequestEntitySaveRequest) {
                List<Cookie> cookies = saveRequest.getCookies();
                if (!CollectionUtils.isEmpty(cookies)) {
                    cookieService.fillCookieInfoWithExecutionRequestInfo(cookies,
                            requestExecuteRequest.getExecutionRequestId(), requestExecuteRequest.getTestRunId());
                    cookieService.deleteByExecutionRequestIdAndTestRunId(requestExecuteRequest.getExecutionRequestId(),
                            requestExecuteRequest.getTestRunId());
                    cookieService.save(cookies);
                }
            }
        } catch (Exception e) {
            statusPassed = false;
            log.error("Request execution failed: (RequestId: {})", request.getId(), e);
        }

        nextRequestPostProcess(testRunId, request.getId(), request.getName());
        // if this condition is true we need to iterate by requests manually
        if (nextRequestService.hasNextRequest(testRunId)) {
            CollectionRunRequest collReq = nextRequestService.findInCollectionOrderNextRequest(testRunId);
            if (Objects.nonNull(collReq)) {
                log.info("Next request is found among already executed requests. (NextRequest: {})", collReq);
                nextRequestService.deleteNextRequest(testRunId);
                nextRequestService.createNewSubCollection(testRunId, collReq);
            }
        }

        return createExecuteStepResponse(requestExecuteRequest, resolvingContext, statusPassed);
    }

    private boolean isRequestMatchesNextRequest(UUID testRunId, UUID currentRequestId, String currentRequestName) {
        String nextRequest = nextRequestService.getNextRequest(testRunId);
        if (Objects.isNull(nextRequest)) {
            return false;
        }
        return currentRequestName.equals(nextRequest) || currentRequestId.toString().equals(nextRequest);
    }

    private void nextRequestPostProcess(UUID testRunId, UUID requestId, String requestName) {
        nextRequestService.addRequestToCollectionOrder(testRunId, requestId, requestName);
        nextRequestService.incrementExecutionCount(testRunId, requestId);
    }

    private List<ExecuteStepResponse> runSubCollection(ExecutionCollectionRequestExecuteRequest requestExecuteRequest,
                                                       UUID environmentId, SaveRequestResolvingContext resolvingContext) {
        UUID testRunId = requestExecuteRequest.getTestRunId();
        log.debug("Execute sub collection from stack");
        List<ExecuteStepResponse> responses = new ArrayList<>();
        for (CollectionRunStackRequest requestFromStack = nextRequestService.pop(testRunId);
             Objects.nonNull(requestFromStack);
             requestFromStack = nextRequestService.pop(testRunId)) {
            Long createdDateStamp = requestExecuteRequest.getSection().getCreatedDateStamp();
            Long newCreatedDateStamp = (Objects.nonNull(createdDateStamp) ? createdDateStamp : 0) + 1;
            requestExecuteRequest.getSection().setCreatedDateStamp(newCreatedDateStamp);
            Request nextRequest = get(requestFromStack.getRequestId());
            ramService.openNewExecuteRequestSection(nextRequest.getName(), newCreatedDateStamp);
            requestExecuteRequest.setContext(resolvingContext.mergeWithScopePrefixes());
            responses.add(executeRequestWithRamAdapterLogging(requestExecuteRequest, nextRequest,
                    environmentId, resolvingContext));
            ramService.closeCurrentSection(requestExecuteRequest.getContext(),
                    resolvingContext.mergeWithScopePrefixes());
        }
        return responses;
    }

    private RequestRuntimeOptions getRuntimeOptions(Request request) {
        return new RequestRuntimeOptions(
                request.isDisableSslCertificateVerification(),
                request.isDisableSslClientCertificate(),
                request.isDisableFollowingRedirect(),
                request.isDisableAutoEncoding());
    }

    /**
     * Create execute step response.
     *
     * @param requestExecuteRequest ExecutionCollectionRequestExecuteRequest
     * @return configured execute step response
     */
    private ExecuteStepResponse createExecuteStepResponse(
            ExecutionCollectionRequestExecuteRequest requestExecuteRequest,
            SaveRequestResolvingContext resolvingContext,
            boolean isPassed) {
        ExecuteStepResponse executeStepResponse = new ExecuteStepResponse();
        executeStepResponse.setStatus(ExecutionStatuses.FINISHED);
        executeStepResponse.setTestingStatus(isPassed ? TestingStatuses.PASSED : TestingStatuses.FAILED);
        if (nonNull(requestExecuteRequest.getContext()) && nonNull(resolvingContext)) {
            ContextEntity context = new ContextEntity();
            try {
                context.setJsonString(objectMapper.writeValueAsString(resolvingContext.mergeWithScopePrefixes()));
                executeStepResponse.setContext(context);
            } catch (JsonProcessingException e) {
                log.error("Failed parse context (RequestId: {})", requestExecuteRequest.getExecutionRequestId(), e);
            }
        }
        return executeStepResponse;
    }

    private ExecuteStepResponse createSkippedExecuteStepResponse(Map<String, Object> contextVariables) {
        ExecuteStepResponse executeStepResponse = new ExecuteStepResponse();
        executeStepResponse.setStatus(ExecutionStatuses.FINISHED);
        executeStepResponse.setTestingStatus(TestingStatuses.SKIPPED);
        ContextEntity context = new ContextEntity();
        try {
            context.setJsonString(objectMapper.writeValueAsString(contextVariables));
            executeStepResponse.setContext(context);
        } catch (JsonProcessingException e) {
            log.error("Failed parse context", e);
        }
        return executeStepResponse;
    }

    /**
     * Executing a request with saving the execution history.
     *
     * @param request request
     * @return request execution response
     */
    public RequestExecutionResponse executeRequest(RequestEntitySaveRequest request, String context, String token,
                                                   UUID sseId, Optional<MultipartFile> file, UUID environmentId,
                                                   List<FileData> fileDataList) throws Exception {
        return executeRequest(request, context, token, sseId, file, environmentId, fileDataList,
                new RequestRuntimeOptions());
    }

    /**
     * Executing a request with saving the execution history.
     *
     * @param request request
     * @return request execution response
     */
    public RequestExecutionResponse executeRequest(RequestEntitySaveRequest request, String context, String token,
                                                   UUID sseId, Optional<MultipartFile> file, UUID environmentId,
                                                   List<FileData> fileDataList,
                                                   RequestRuntimeOptions runtimeOptions) throws Exception {
        SaveRequestResolvingContext resolvingContext = SaveRequestResolvingContext.builder().build();
        resolvingContext.parseAndClassifyContextVariables(request.getContextVariables());

        if (nonNull(environmentId)) {
            List<System> systems = envParamService.getEnvironmentSystemsById(environmentId);
            Map<String, Object> environmentVariables = SaveRequestResolvingContext.parseSystems(systems);
            resolvingContext.setEnvironmentVariables(environmentVariables);
            resolvingContext.getEnvironment().putAll(environmentVariables);
        }

        // Add executor identity to LOCAL scope so macros can read it via contextMap.get(...)
        executorContextEnricher.enrich(resolvingContext.getVariables(), token, request.getName());

        UUID projectId = request.getProjectId();
        if (request instanceof HttpRequestEntitySaveRequest) {
            HttpRequestEntitySaveRequest httpRequest = (HttpRequestEntitySaveRequest) request;
            if (!request.isAutoCookieDisabled()) {
                httpRequest.setCookies(cookieService.getNotExpiredCookiesByUserIdAndProjectId(projectId));
            }

            // remove generated auth header because it will be generated in request execution if needed
            httpRequest.getRequestHeaders().removeIf(
                    header -> header.isGenerated() && "Authorization".equals(header.getKey()));
        }

        TriConsumer<RequestEntitySaveRequest, RequestExecutionResponse, Exception> afterExecution =
                (req, resp, error) ->
                        executionHistoryService.logRequestExecution(token, sseId, req, resp, error, fileDataList);
        BiFunction<PostmanExecuteScriptResponseDto, Boolean, JsExecutionResult> scriptExecution =
                (scriptResults, isPreScript) -> executionHistoryService.logRequestJsExecution(token, sseId, request,
                        scriptResults, isPreScript);
        BiConsumer<RequestEntitySaveRequest, List<ConsoleLogDto>> logConsoleLogs = (req, consoleLogs) -> {
            Optional<RequestExecutionDetails> detailsOptional = detailsRepository.findByRequestExecutionSseId(sseId);
            RequestExecutionDetails details;
            if (detailsOptional.isPresent()) {
                details = detailsOptional.get();
            } else {
                log.warn("Details information for request id {} not found by SSE ID {}. Generate new details.",
                        req.getId(), sseId);
                details = executionHistoryService.generateAndConfigureRequestExecutionDetails(request, token, sseId);
            }
            details.setConsoleLogs(consoleLogs);
            detailsRepository.save(details);
        };
        Function<RequestExecutionResponse, UUID> setExecutionId = (resp) -> {
            Optional<RequestExecutionDetails> detailsOptional = detailsRepository.findByRequestExecutionSseId(sseId);
            RequestExecutionDetails details;
            if (detailsOptional.isPresent()) {
                details = detailsOptional.get();
            } else {
                log.warn("Details information for request id {} not found by SSE ID {}. Generate new details.",
                        request.getId(), sseId);
                details = executionHistoryService.generateAndConfigureRequestExecutionDetails(request, token, sseId);
                detailsRepository.save(details);
            }
            return details.getRequestExecution().getId();
        };
        RequestExecutionResponse response = executeRequest(request, context, file, resolvingContext,
                afterExecution, scriptExecution, environmentId, fileDataList, setExecutionId, logConsoleLogs,
                runtimeOptions, (ignore) -> { });
        response.parseAndSetContextVariables(resolvingContext);

        RequestExecutionDetails details = detailsRepository.findByRequestExecutionSseId(sseId)
                .orElseGet(() -> {
                    log.warn("Details information for request id {} not found by SSE ID {}. Generate new details.",
                            request.getId(), sseId);
                    return executionHistoryService.generateAndConfigureRequestExecutionDetails(request, token, sseId);
                });
        details.setContextVariables(response.getContextVariables());

        if (request instanceof HttpRequestEntitySaveRequest) {
            HttpRequestEntitySaveRequest httpRequest = (HttpRequestEntitySaveRequest) request;
            List<Cookie> cookies = request.getCookies();
            if (!CollectionUtils.isEmpty(cookies)) {
                if (!request.isAutoCookieDisabled()) {
                    cookieService.fillCookieInfo(cookies, projectId);
                    cookieService.deleteByUserIdAndProjectId(projectId);
                    List<Cookie> savedCookies = cookieService.save(cookies);
                    try {
                        URI uri = new URI(httpRequest.getUrl());
                        HttpHeaderSaveRequest cookieHeader = cookieService.cookieListToRequestHeader(
                                uri, savedCookies);
                        if (StringUtils.isNotEmpty(cookieHeader.getValue())) {
                            response.setCookieHeader(cookieHeader);
                            details.setCookieHeader(cookieHeader);
                        }
                    } catch (URISyntaxException ignore) {
                        log.debug("Syntax exception", ignore);
                    }
                }
                response.setCookies(CookieUtils.convertCookieListToResponseCookieList(
                        cookieService.filterCookie(httpRequest.getUrl(), cookies)));
                details.setCookies(response.getCookies());
            }

        }

        detailsRepository.save(details);
        log.debug("Response to return: {}, and final details saved {}", response, details);
        return response;
    }

    /**
     * Execute request and runs afterExecution function.
     *
     * @param request        request
     * @param context        context
     * @param file           file for diameter request, can bu null
     * @param afterExecution afterExecution function, Can be used for additional actions with the request and
     *                       the response
     * @param fileDataList   request files
     * @param setExecutionId Supplier how to get execution ID
     * @param logConsoleLogs TriConsumer how log ConsoleLogs
     * @return request execution response
     */
    private RequestExecutionResponse executeRequest(RequestEntitySaveRequest request, String context,
                                                    Optional<MultipartFile> file,
                                                    SaveRequestResolvingContext resolvingContext,
                                                    TriConsumer<RequestEntitySaveRequest,
                                                            RequestExecutionResponse, Exception> afterExecution,
                                                    BiFunction<PostmanExecuteScriptResponseDto, Boolean,
                                                            JsExecutionResult> scriptExecution,
                                                    UUID environmentId, List<FileData> fileDataList,
                                                    Function<RequestExecutionResponse, UUID> setExecutionId,
                                                    BiConsumer<RequestEntitySaveRequest, List<ConsoleLogDto>>
                                                            logConsoleLogs) {
        return executeRequest(request, context, file, resolvingContext, afterExecution, scriptExecution,
                environmentId, fileDataList, setExecutionId, logConsoleLogs, new RequestRuntimeOptions(),
                (ignore) -> { });
    }

    /**
     * Execute request and runs afterExecution function.
     *
     * @param request        request
     * @param context        context
     * @param file           file for diameter request, can bu null
     * @param afterExecution afterExecution function,
     *                       Can be used for additional actions with the request and the response
     * @param fileDataList   request files
     * @param runtimeOptions Options set for Request execution
     * @return request execution response
     */
    private RequestExecutionResponse executeRequest(RequestEntitySaveRequest request, String context,
                                                    Optional<MultipartFile> file,
                                                    SaveRequestResolvingContext resolvingContext,
                                                    TriConsumer<RequestEntitySaveRequest,
                                                            RequestExecutionResponse, Exception> afterExecution,
                                                    BiFunction<PostmanExecuteScriptResponseDto, Boolean,
                                                            JsExecutionResult> scriptExecution,
                                                    UUID environmentId, List<FileData> fileDataList,
                                                    Function<RequestExecutionResponse, UUID> setExecutionId,
                                                    BiConsumer<RequestEntitySaveRequest, List<ConsoleLogDto>>
                                                            logConsoleLogs,
                                                    RequestRuntimeOptions runtimeOptions,
                                                    Consumer<PostmanExecuteScriptResponseDto> updateNextRequest) {
        RequestExecutionResponse response = new RequestExecutionResponse();
        // set current date into startedWhen, startedWhen will be rewritten right before actual request execution
        response.setStartedWhen(new Date());
        final UUID projectId = request.getProjectId();
        Stopwatch timer = Stopwatch.createStarted();
        TransportType transportType = request.getTransportType();

        file.ifPresent(multipartFile -> ((HttpRequestEntitySaveRequest) request)
                .setFile(new FileData(null, multipartFile.getOriginalFilename())));
        // Add saved files if any to request execution files
        if (CollectionUtils.isEmpty(fileDataList)) {
            fileDataList = gridFsService.getFilesDataByRequestId(request.getId());
        } else {
            List<FileData> finalFileDataListForFiltering = fileDataList;
            List<FileData> gridFsFileDatas = gridFsService.getFilesDataByRequestId(
                            request.getId()).stream().filter(fileData ->
                            finalFileDataListForFiltering.stream().noneMatch(receivedFileData ->
                                    receivedFileData.getFileName().equals(fileData.getFileName())))
                    .collect(Collectors.toList());
            fileDataList.addAll(gridFsFileDatas);
        }

        validateRequestSizeLimits(request, transportType, file, fileDataList);

        Evaluator evaluator = macrosService.createMacrosEvaluator(request.getProjectId());
        Exception errorMessage = null;
        RequestEntitySaveRequest requestForHistory = request;
        List<ConsoleLogDto> consoleLogs = null;

        try {
            JsExecutionResult jsResult = null;
            try {
                request.normalize();
                // Execute pre-script and saving test results
                PostmanExecuteScriptResponseDto scriptResponseDto =
                        scriptService.evaluateRequestPreScript(request, resolvingContext);
                jsResult = scriptExecution.apply(scriptResponseDto, true);
                updateNextRequest.accept(scriptResponseDto);
                // if test result contains errors
                // then throws exception to interrupt request execution
                if (!jsResult.isPassed()) {
                    throw getExceptionIfScriptEngineScriptResultIsNotPassed(scriptResponseDto, true);
                }
                templateResolverService.resolveTemplatesWithOrder(request, resolvingContext, evaluator);
                requestForHistory = generateRequestForHistory(request);
                templateResolverService.processEncryptedValues(request, false);
                templateResolverService.processEncryptedValues(requestForHistory, true);
                consoleLogs = jsResult.getConsoleLogs();
                RequestPreExecuteResponse requestPreExecuteResponse = preExecuteProcessing(projectId,
                        request,
                        requestForHistory,
                        environmentId,
                        resolvingContext,
                        evaluator);
                addCookieHeader(request);
                response = executeRequest(request, context, resolvingContext, file, fileDataList, runtimeOptions);
                response.setTestsPassed(true);
            } catch (AtpException ex) {
                errorMessage = ex;
                throw ex;
            } finally {
                updateRequestForHistory(request, requestForHistory);
                afterExecution.accept(requestForHistory, response, errorMessage);
                response.setExecutionId(setExecutionId.apply(response));
            }

            validateResponseAndPostScriptSizeLimits(response, request.getPostScripts());

            // if request executed without exceptions
            // then execute post script
            PostmanExecuteScriptResponseDto scriptResponseDto =
                    scriptService.evaluateRequestPostScript(request, response, resolvingContext);
            jsResult = scriptExecution.apply(scriptResponseDto, false);
            updateNextRequest.accept(scriptResponseDto);
            response.setTestsPassed(jsResult.isPassed());
            if (!jsResult.isPassed()) {
                ItfLiteException postScriptException =
                        getExceptionIfScriptEngineScriptResultIsNotPassed(scriptResponseDto, false);
                response.setError(RequestUtils.getErrorResponse(postScriptException));
            }

            if (jsResult.getConsoleLogs() != null) {
                if (consoleLogs == null) {
                    consoleLogs = jsResult.getConsoleLogs();
                } else {
                    consoleLogs.addAll(jsResult.getConsoleLogs());
                }
            }
        } finally {
            if (consoleLogs == null) {
                consoleLogs = new ArrayList<>();
            }
            logConsoleLogs.accept(requestForHistory, consoleLogs);
            addTimeMetric(transportType, projectId, timer);
        }
        return response;
    }

    private RequestExecutionResponse executeRequest(RequestEntitySaveRequest request, String context,
                                                    SaveRequestResolvingContext resolvingContext,
                                                    Optional<MultipartFile> file,
                                                    List<FileData> fileDataList,
                                                    RequestRuntimeOptions runtimeOptions) {
        TransportType transportType = request.getTransportType();
        try {
            return prepareAndExecuteHttpRequest((HttpRequestEntitySaveRequest) request, context, resolvingContext,
                    file, fileDataList, runtimeOptions);
        } catch (AtpException ex) {
            log.error("Exception during request execution", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Exception during request execution", ex);
            throw new ItfLiteException(ex.getMessage());
        }
    }

    /**
     * Checks if request size without post-script meets limitations.
     * @param request request
     * @param transportType transport type
     */
    private void validateRequestSizeLimits(RequestEntitySaveRequest request, TransportType transportType,
                                           Optional<MultipartFile> file, List<FileData> fileDataList) {
        HttpRequestEntitySaveRequest httpRequestEntitySaveRequest = (HttpRequestEntitySaveRequest) request;
        byte[] serializedHttpRequestEntitySaveRequest = SerializationUtils.serialize(httpRequestEntitySaveRequest);
        long requestWithoutPostScriptSize = 0;
        if (nonNull(serializedHttpRequestEntitySaveRequest)) {
            requestWithoutPostScriptSize = serializedHttpRequestEntitySaveRequest.length;
        } else {
            log.warn("Can't serialize http request entity save for request {}", request.getId());
        }
        if (nonNull(httpRequestEntitySaveRequest.getPostScripts())) {
            byte[] serializedPostScripts = SerializationUtils.serialize(
                    httpRequestEntitySaveRequest.getPostScripts());
            long postScriptsSizeInBytes = 0;
            if (nonNull(serializedPostScripts)) {
                postScriptsSizeInBytes = serializedPostScripts.length;
            } else {
                log.warn("Can't serialize post scripts for request {}", request.getId());
            }
            requestWithoutPostScriptSize = requestWithoutPostScriptSize - postScriptsSizeInBytes;
        }
        if (!CollectionUtils.isEmpty(fileDataList)) {
            byte[] serializedFileDataList = SerializationUtils.serialize(fileDataList);
            if (nonNull(serializedFileDataList)) {
                requestWithoutPostScriptSize += serializedFileDataList.length;
            } else {
                log.warn("Can't serialize file data list for request {}", request.getId());
            }
        }
        if (file != null && file.isPresent()) {
            try {
                requestWithoutPostScriptSize += file.get().getBytes().length;
            } catch (IOException exception) {
                log.warn("Can't get bytes for binary file for request {}", request.getId(), exception);
            }
        }
        if (isNotEntityContentMeetLimit(requestWithoutPostScriptSize, true)) {
            ItfLiteRequestSizeLimitException exception = new ItfLiteRequestSizeLimitException();
            log.error("Request and pre-script are bigger than ITF-Lite's configured limit = {} Mb",
                    requestResponseSizeProperties.getRequestSizeLimitInMb(), exception);
            throw exception;
        }
    }

    /**
     * Checks if response and post-script size meet limitations.
     * @param response response
     * @param postScripts post scripts
     */
    private void validateResponseAndPostScriptSizeLimits(RequestExecutionResponse response, String postScripts) {
        byte[] serializedResponse = SerializationUtils.serialize(response);
        long responseAndPostScriptSize = 0;
        if (nonNull(serializedResponse)) {
            responseAndPostScriptSize = serializedResponse.length;
        } else {
            log.warn("Can't serialize response with id = {}", response.getId());
        }
        if (StringUtils.isNotEmpty(postScripts)) {
            byte[] serializedPostScripts = SerializationUtils.serialize(postScripts);
            long postScriptsSize = 0;
            if (nonNull(serializedPostScripts)) {
                postScriptsSize = serializedPostScripts.length;
            } else {
                log.warn("Can't serialize post scripts with response id = {}", response.getId());
            }
            responseAndPostScriptSize += postScriptsSize;
        }
        if (isNotEntityContentMeetLimit(responseAndPostScriptSize, false)) {
            ItfLiteResponseSizeLimitException exception = new ItfLiteResponseSizeLimitException();
            log.error("Response and post-script are bigger than ITF-Lite's configured limit = {} Mb",
                    requestResponseSizeProperties.getResponseSizeLimitInMb(), exception);
            throw exception;
        }
    }

    private Consumer<PostmanExecuteScriptResponseDto> getUpdateNextRequestFunc(UUID testRunId) {
        return scriptExecResp -> {
            if (Objects.nonNull(scriptExecResp) && scriptExecResp.getHasNextRequest()) {
                nextRequestService.setNextRequest(testRunId, scriptExecResp.getNextRequest());
            }
        };
    }

    private ItfLiteException getExceptionIfScriptEngineScriptResultIsNotPassed(
            PostmanExecuteScriptResponseDto scriptResponseDto, boolean isPreScript) {
        ItfLiteException exception = null;
        if (isPreScript) {
            exception = new ItfLiteScriptEnginePreScriptExecutionException();
        } else {
            exception = new ItfLiteScriptEnginePostScriptExecutionException();
        }
        if (!CollectionUtils.isEmpty(scriptResponseDto.getTestResults())) {
            PostmanExecuteScriptResponseTestResultsInnerErrorDto scriptResponseErrorDto =
                    scriptResponseDto.getTestResults().get(0).getError();

            if (isPreScript) {
                exception = new ItfLiteScriptEnginePreScriptExecutionException(scriptResponseErrorDto.getMessage());
            } else {
                // if the tests are failed in the post script, don't need exception
                exception = null;
            }

            HttpResponseExceptionTypeEnum exceptionTypeDto =
                    scriptResponseErrorDto.getHttpResponseExceptionType();
            if (Objects.isNull(exceptionTypeDto)) {
                log.warn("Exception type not specified");
                return exception;
            }
            ScriptEngineExceptionType exceptionType = ScriptEngineExceptionType.valueOf(exceptionTypeDto.getValue());
            if (ScriptEngineExceptionType.DECRYPT_EXCEPTION.equals(exceptionType)) {
                exception = new ItfLiteScriptEngineAtpDecryptException(scriptResponseErrorDto.getMessage());
            } else if (ScriptEngineExceptionType.ENCRYPT_EXCEPTION.equals(exceptionType)) {
                exception = new ItfLiteScriptEngineAtpEncryptException(scriptResponseErrorDto.getMessage());
            } else if (ScriptEngineExceptionType.UNAVAILABLE_EXCEPTION.equals(exceptionType)) {
                exception = new ItfLiteScriptEngineUnavailableException();
            } else if (ScriptEngineExceptionType.POSTMAN_SANDBOX_CONTEXT_EXCEPTION.equals(exceptionType)) {
                exception = new ItfLiteScriptEnginePostmanSandboxContextException();
            }
        }
        return exception;
    }

    private RequestPreExecuteResponse preExecuteProcessing(UUID projectId,
                                                           RequestEntitySaveRequest request,
                                                           RequestEntitySaveRequest historyRequest,
                                                           UUID environmentId,
                                                           SaveRequestResolvingContext resolvingContext,
                                                           Evaluator evaluator) {
        if (request instanceof HttpRequestEntitySaveRequest && historyRequest instanceof HttpRequestEntitySaveRequest) {
            return httpRequestPreExecuteProcessing(projectId,
                    (HttpRequestEntitySaveRequest) request,
                    (HttpRequestEntitySaveRequest) historyRequest,
                    environmentId,
                    resolvingContext,
                    evaluator);
        }
        return RequestPreExecuteResponse.builder().build();
    }

    private RequestPreExecuteResponse httpRequestPreExecuteProcessing(UUID projectId,
                                                                      HttpRequestEntitySaveRequest request,
                                                                      HttpRequestEntitySaveRequest historyRequest,
                                                                      UUID environmentId,
                                                                      SaveRequestResolvingContext resolvingContext,
                                                                      Evaluator evaluator) {
        String authToken = null;
        try {
            authToken = requestAuthorizationService.processRequestAuthorization(projectId,
                    request, historyRequest, environmentId, evaluator, resolvingContext);
        } catch (AtpException ex) {
            log.error("Exception during request authorization processing", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Exception during request authorization processing", ex);
            throw new ItfLiteException(ex.getMessage());
        }
        return RequestPreExecuteResponse.builder().authToken(authToken).build();
    }

    private void addCookieHeader(RequestEntitySaveRequest request) {
        if (request instanceof HttpRequestEntitySaveRequest && !request.isAutoCookieDisabled()) {
            HttpRequestEntitySaveRequest httpRequest = (HttpRequestEntitySaveRequest) request;
            if (!CollectionUtils.isEmpty(httpRequest.getCookies())) {
                try {
                    URI uri = new URI(httpRequest.getUrl());
                    HttpHeaderSaveRequest cookieHeader = cookieService.cookieListToRequestHeader(uri,
                            httpRequest.getCookies());
                    if (cookieHeader != null && !StringUtils.isEmpty(cookieHeader.getValue())) {
                        httpRequest.getRequestHeaders().add(0, cookieHeader);
                    }
                } catch (URISyntaxException ignore) {
                    log.debug("Syntax exception", ignore);
                }
            }
        }
    }

    /**
     * Resolve request properties and execute http request.
     *
     * @param request        request
     * @param context        context
     * @param fileDataList   request files
     * @param runtimeOptions runtime options
     * @return response
     */
    private RequestExecutionResponse prepareAndExecuteHttpRequest(HttpRequestEntitySaveRequest request,
                                                                  String context,
                                                                  SaveRequestResolvingContext resolvingContext,
                                                                  Optional<MultipartFile> binaryFile,
                                                                  List<FileData> fileDataList,
                                                                  RequestRuntimeOptions runtimeOptions) {
        updateSaveRequestHeadersFields(request.getRequestHeaders());
        updateSaveRequestParametersFields(request.getRequestParams());
        if (request.getBody() != null && RequestBodyType.GraphQL.equals(request.getBody().getType())) {
            request.getBody().computeAndSetContent();
        }
        UUID projectId = request.getProjectId();

        try {
            RequestExecutionResponse response = executeHttpRequest(projectId, request, context, binaryFile,
                    resolvingContext, fileDataList, runtimeOptions);
            if (isNull(request.getCookies())) {
                request.setCookies(new ArrayList<>());
            }
            request.setCookies(CookieUtils.addResponseCookie(request.getCookies(), response.getCookies()));
            return response;
        } catch (AtpException exception) {
            log.error("Failed to execute http request '{}'", request.getId(), exception);
            throw exception;
        } catch (HttpClientErrorException httpClientErrorException) {
            log.error("Failed to execute http request '{}'", request.getId(), httpClientErrorException);
            throw new ItfLiteHttpRequestExecuteException(httpClientErrorException);
        } catch (InvocationTargetException exception) {
            log.error("Failed to execute http request '{}'", request.getId(), exception.getTargetException());
            throw new ItfLiteHttpRequestExecuteException(exception.getTargetException());
        } catch (Exception ex) {
            log.error("Failed to execute http request '{}'", request.getId(), ex);
            throw new ItfLiteHttpRequestExecuteException(ex);
        }
    }

    private void addTimeMetric(TransportType type, UUID projectId, Stopwatch timer) {
        metricService.timer(Constants.REQUEST_METRICS_NAME, Constants.PROJECT_ID_LABEL_NAME, projectId.toString(),
                Constants.REQUEST_TYPE_LABEL_NAME, type.name().toLowerCase(Locale.US)).record(timer.elapsed());
        timer.stop();
    }

    RequestEntitySaveRequest generateRequestForHistory(RequestEntitySaveRequest request) {
        try {
            RequestEntitySaveRequest requestForHistoryDeepCopy = modelMapper.map(
                    request,
                    request.getClass());
            if (request instanceof HttpRequestEntitySaveRequest) {
                HttpRequestEntitySaveRequest httpRequestEntitySaveRequest
                        = (HttpRequestEntitySaveRequest) requestForHistoryDeepCopy;
                httpRequestEntitySaveRequest.setRequestParams(httpRequestEntitySaveRequest.getRequestParams()
                        .stream()
                        .filter(q -> !q.isDisabled())
                        .collect(Collectors.toList()));
                httpRequestEntitySaveRequest.setRequestHeaders(httpRequestEntitySaveRequest.getRequestHeaders()
                        .stream()
                        .filter(h -> !h.isDisabled())
                        .collect(Collectors.toList()));
                if (Objects.nonNull(httpRequestEntitySaveRequest.getBody())) {
                    if (RequestBodyType.FORM_DATA.equals(httpRequestEntitySaveRequest.getBody().getType())) {
                        if (!CollectionUtils.isEmpty(httpRequestEntitySaveRequest.getBody().getFormDataBody())) {
                            httpRequestEntitySaveRequest.getBody().setFormDataBody(
                                    httpRequestEntitySaveRequest.getBody().getFormDataBody()
                                            .stream()
                                            .filter(fdp -> !fdp.isDisabled())
                                            .collect(Collectors.toList()));
                        }
                    } else if (RequestBodyType.GraphQL.equals(httpRequestEntitySaveRequest.getBody().getType())) {
                        httpRequestEntitySaveRequest.getBody().computeAndSetContent();
                    }
                }
                addCookieHeader(httpRequestEntitySaveRequest);
            }
            return requestForHistoryDeepCopy;
        } catch (AtpException ex) {
            log.error("Failed to create request for history", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to create request for history", ex);
            throw new ItfLiteException("Failed to create request for history");
        }
    }

    private void updateRequestForHistory(RequestEntitySaveRequest request, RequestEntitySaveRequest historyRequest) {
        if (request instanceof HttpRequestEntitySaveRequest) {
            if (historyRequest != null) {
                ((HttpRequestEntitySaveRequest) historyRequest)
                        .setFile(((HttpRequestEntitySaveRequest) request)
                                .getFile());
            }
            log.debug("Copy file data for history request {}", historyRequest);
        }
    }

    /**
     * Execute http request.
     *
     * @param httpRequest httpRequest
     * @return request execution response
     */
    public RequestExecutionResponse executeHttpRequest(UUID projectId,
                                                       HttpRequestEntitySaveRequest httpRequest,
                                                       String context, Optional<MultipartFile> binaryFile,
                                                       SaveRequestResolvingContext resolvingContext,
                                                       List<FileData> fileNameStreamMap,
                                                       RequestRuntimeOptions runtimeOptions) throws Exception {
        HttpEntity entity = obtainHttpEntity(httpRequest, context, binaryFile, resolvingContext, fileNameStreamMap);
        Header[] headers = obtainHttpHeaders(httpRequest);
        HttpMethod method = obtainHttpMethod(httpRequest);
        String urlWithParameters = createUrlWithParameters(httpRequest, runtimeOptions);
        Date beforeExecutionDate = new Date();
        CookieStore httpCookieStore = new BasicCookieStore();
        try (CloseableHttpClient client =
                     httpClientService.getHttpClient(projectId, runtimeOptions, urlWithParameters, httpCookieStore)) {
            HttpEntityEnclosingRequestBase request =
                    (HttpEntityEnclosingRequestBase) method.getHttpRequest(urlWithParameters);
            request.setEntity(entity);
            request.setHeaders(headers);

            try (CloseableHttpResponse response = client.execute(request)) {
                calculateAndRegisterRequestSize(entity, headers, projectId, httpRequest.getTransportType());
                return createResponse(httpRequest, response, beforeExecutionDate, new Date(),
                        httpCookieStore, projectId);
            }
        }
    }

    private void calculateAndRegisterRequestSize(HttpEntity entity,
                                                 Header[] headers,
                                                 UUID projectId,
                                                 TransportType transportType) throws Exception {
        double requestSize = 0.0;
        requestSize += RequestUtils.calculateHeadersSize(headers);
        if (nonNull(entity)) {
            // Used outputStream to avoid `org.apache.http.ContentTooLongException: Content length is too long`
            // for MultipartFormEntity http entities
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            entity.writeTo(outputStream);
            outputStream.flush();
            requestSize += outputStream.toByteArray().length;
        }
        metricService.incrementRequestSizePerProject(requestSize, projectId,
                transportType);
    }

    private boolean isNotEntityContentMeetLimit(long sizeInBytes, boolean isRequestEntity) {
        int bodySizeLimitInMb = requestResponseSizeProperties.getRequestSizeLimitInMb();
        if (!isRequestEntity) {
            bodySizeLimitInMb = requestResponseSizeProperties.getResponseSizeLimitInMb();
        }
        // bytes to megabytes before comparison
        return sizeInBytes * 1.0 / 1024 / 1024 >= bodySizeLimitInMb;
    }

    private Header[] obtainHttpHeaders(HttpRequestEntitySaveRequest httpRequest) {
        if (isNull(httpRequest.getRequestHeaders())) {
            return null;
        }
        return httpRequest.getRequestHeaders()
                .stream()
                .filter(h -> !h.isDisabled())
                .map(h -> new BasicHeader(h.getKey(), h.getValue()))
                .toArray(Header[]::new);
    }

    private HttpMethod obtainHttpMethod(HttpRequestEntitySaveRequest httpRequest) {
        String requestUrl = httpRequest.getUrl();
        UUID requestId = httpRequest.getId();
        HttpMethod requestHttpMethod = httpRequest.getHttpMethod();

        if (StringUtils.isEmpty(requestUrl)) {
            log.error("Provided request '{}' URL is empty", requestId);
            throw new ItfLiteRequestIllegalUrlEmptyValueException();
        }

        if (isNull(requestHttpMethod)) {
            log.error("Failed to obtain http method from value: {}", httpRequest.getHttpMethod());
            throw new ItfLiteHttpRequestIllegalMethodValueException(String.valueOf(httpRequest.getHttpMethod()));
        }

        return requestHttpMethod;
    }

    @Nullable
    private HttpEntity obtainHttpEntity(HttpRequestEntitySaveRequest httpRequest, String context,
                                        Optional<MultipartFile> binaryFileOpt,
                                        SaveRequestResolvingContext resolvingContext,
                                        List<FileData> fileDataList) throws URISyntaxException, IOException {
        final RequestBody body = httpRequest.getBody();

        if (isNull(body) && !binaryFileOpt.isPresent()) {
            return null;
        } else if (binaryFileOpt.isPresent()) {
            MultipartFile binaryFile = binaryFileOpt.get();
            final String contentType = binaryFile.getContentType();
            final byte[] processedBinaryFile = itfLiteFileService
                    .resolveParametersInMultipartFile(binaryFile, resolvingContext);
            if (processedBinaryFile != null) {
                httpRequest.setFile(new FileData(processedBinaryFile, binaryFile.getOriginalFilename()));
                httpRequest.setBody(new RequestBody((String) null, RequestBodyType.Binary));

                return EntityBuilder.create()
                        .setBinary(processedBinaryFile)
                        .setContentType(
                                contentType == null ? ContentType.DEFAULT_BINARY : ContentType.parse(contentType))
                        .build();
            } else {
                return null;
            }
        } else {
            final RequestBodyType bodyType = body.getType();
            final String content = body.getContent();
            final UUID projectId = httpRequest.getProjectId();

            if (RequestBodyType.Velocity.equals(bodyType)) {
                final String processedBody = processVelocity(projectId, content, context);
                return new StringEntity(processedBody);
            }
            if (RequestBodyType.FORM_DATA.equals(bodyType)) {
                String boundary = getFormDataBoundary(httpRequest.getRequestHeaders());
                HttpEntity entity = prepareFormDataPart(body, fileDataList, boundary);
                updateGeneratedHeaderContentTypeValue(httpRequest.getRequestHeaders(),
                        entity.getContentType().getValue());
                return entity;
            } else {
                if (nonNull(body.getContent())) {
                    return new StringEntity(content, StandardCharsets.UTF_8);
                } else {
                    return null;
                }
            }
        }
    }

    @Nullable
    private String getFormDataBoundary(List<HttpHeaderSaveRequest> headers) {
        String contentType = null;
        for (HttpHeaderSaveRequest header: headers) {
            if ("Content-Type".equals(header.getKey())) {
                contentType = header.getValue();
            }
        }
        if (StringUtils.isNotEmpty(contentType)) {
            String[] contentTypeParts = contentType.split(";");
            for (String contentTypePart : contentTypeParts) {
                if (contentTypePart.contains("boundary")) {
                    int startValueIdx = contentTypePart.indexOf("=");
                    if (startValueIdx != -1) {
                        String boundaryValue = contentTypePart.substring(startValueIdx + 1).trim();
                        if (StringUtils.isEmpty(boundaryValue)) {
                            return null;
                        }
                        return boundaryValue;
                    }
                }
            }
        }
        return null;
    }

    private void updateGeneratedHeaderContentTypeValue(List<HttpHeaderSaveRequest> headers, String contentType) {
        headers
                .stream()
                .filter(h -> h.isGenerated() && "Content-Type".equals(h.getKey()))
                .forEach(h -> h.setValue(contentType));
    }

    private HttpEntity prepareFormDataPart(RequestBody body, List<FileData> fileDataList, String boundary) {
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create().setBoundary(boundary);
        Map<String, FileData> map = Objects.isNull(fileDataList) ? new HashMap<>() :
                StreamUtils.toEntityMapWithMergeFunction(fileDataList, FileData::getFileName, (file1, file2) -> file1);
        body.getFormDataBody().forEach(requestFormDataBody -> {
            ValueType type = requestFormDataBody.getType();
            String key = requestFormDataBody.getKey();
            if (ValueType.TEXT.equals(type)) {
                ContentType contentType = StringUtils.isEmpty(requestFormDataBody.getContentType())
                        ? ContentType.APPLICATION_OCTET_STREAM
                        : ContentType.parse(requestFormDataBody.getContentType());
                multipartEntityBuilder.addPart(key,
                        new StringBody(requestFormDataBody.getValue(), contentType));
            } else if (ValueType.FILE.equals(type)) {
                String fileName = requestFormDataBody.getValue();
                FileData fileData = map.get(fileName);
                if (fileData != null) {
                    ContentType contentType = StringUtils.isEmpty(requestFormDataBody.getContentType())
                            ? FileUtils.guessContentTypeFromName(fileName)
                            : ContentType.parse(requestFormDataBody.getContentType());
                    multipartEntityBuilder.addPart(key,
                            new ByteArrayBody(fileData.getContent(), contentType, fileName));
                }
            }
        });
        return multipartEntityBuilder.build();
    }

    /**
     * Saves multipart file to file system and grid fs.
     *
     * @param requestId diameter request id
     * @param file      multipart file dictionary or binary
     * @throws IOException could be during file system operations
     */
    public Optional<FileBody> saveFileToFileSystemAndGridFs(UUID requestId, MultipartFile file,
                                                            TransportType transportType) throws IOException {
        FileUtils.saveMultipartFileDictionaryToFileSystem(Constants.DEFAULT_BINARY_FILES_FOLDER, requestId,
                file);
        gridFsService.removeFileByRequestId(requestId);
        FileBody fileInfo = gridFsService.saveBinaryByRequestId(LocalDateTime.now().toString(), requestId,
                file.getInputStream(), file.getOriginalFilename(), file.getContentType());
        log.debug("File for request {} was saved with parameters {}", requestId, fileInfo);
        return Optional.of(fileInfo);
    }

    /**
     * Substitutes parameters values from ITF if ITF feign client is enabled.
     *
     * @param content string with itf variables
     * @param context context
     * @return Processed string with variables values
     */
    public String processVelocity(UUID projectId, String content, String context) throws URISyntaxException {
        if (feignClientsProperties.getIsFeignAtpItfEnabled()) {
            String[] contextArray = {};
            if (!StringUtils.isEmpty(context)) {
                contextArray = context.split(ITF_URL_CONTEXT_SEPARATOR);
            }
            String contextId;
            URI itfUri;
            String itfRoute;
            if (contextArray.length > 1) {
                // contextArray[0] --- itf url, contextArray[1] --- contextId
                itfUri = getItfUri(contextArray[0]);
                itfRoute = "";
                contextId = contextArray[1];
                Properties properties = generatePropertiesForItfRequest(content, contextId);
                ItfParametersResolveResponse response = itfPlainFeignClient.processVelocity(
                        itfUri, itfRoute, projectId, properties);
                return response.getResponse();
            } else {
                contextId = context;

                UIVelocityRequestBodyDto requestBody = generateRequestBodyForItf(content, contextId);
                ItfParametersResolveResponse response = itfFeignService.processVelocity(projectId, requestBody);
                return response.getResponse();
            }
        }
        return content;
    }

    /**
     * Generate properties for itf /velocity endpoint.
     *
     * @param requestBody request body
     * @return properties
     */
    private Properties generatePropertiesForItfRequest(String requestBody, String contextId) {
        Properties properties = new Properties();
        if (!StringUtils.isEmpty(contextId)) {
            properties.setProperty("context", contextId);
        } else {
            properties.setProperty("context", "");
        }
        properties.setProperty("xpath", "");
        properties.setProperty("message", requestBody);
        return properties;
    }

    /**
     * Generate request body for itf '/velocity' endpoint.
     *
     * @param requestBody request body
     * @return requestBodyDto
     */
    private UIVelocityRequestBodyDto generateRequestBodyForItf(String requestBody, String contextId) {
        UIVelocityRequestBodyDto requestBodyDto = new UIVelocityRequestBodyDto();
        if (!StringUtils.isEmpty(contextId)) {
            requestBodyDto.setContext(contextId);
        } else {
            requestBodyDto.setContext("");
        }
        requestBodyDto.setMessage(requestBody);
        return requestBodyDto;
    }

    /**
     * Create execution response entity and fill with parameters from HttpClient response.
     *
     * @param response            HttpResponse
     * @param beforeExecutionDate before execution date
     * @param afterExecutionDate  after execution date
     * @return RequestExecutionResponse
     */
    private RequestExecutionResponse createResponse(HttpRequestEntitySaveRequest httpRequest, HttpResponse response,
                                                    Date beforeExecutionDate, Date afterExecutionDate,
                                                    CookieStore cookieStore, UUID projectId)
            throws IOException {
        List<RequestExecutionHeaderResponse> headers = new ArrayList<>();
        Header[] respHeaders = response.getAllHeaders();
        double responseSize = 0.0;
        if (nonNull(respHeaders)) {
            for (Header header : respHeaders) {
                responseSize += RequestUtils.calculateHeaderSize(header);
                headers.add(new RequestExecutionHeaderResponse(header.getName(), header.getValue()));
            }
        }

        String requestDomain = UrlParsingUtils.getDomain(httpRequest.getUrl());
        List<ResponseCookie> cookies = CookieUtils.parseResponseCookie(requestDomain, cookieStore.getCookies());

        HttpEntity entity = response.getEntity();
        String body = "";
        if (nonNull(entity)) {
            body = EntityUtils.toString(entity,StandardCharsets.UTF_8);
            ContentType contentType = ContentType.get(entity);
            Charset charset = contentType == null ? null : ContentType.get(entity).getCharset();
            responseSize += charset == null ? body.getBytes().length : body.getBytes(charset).length;
        }
        metricService.incrementResponseSizePerProject(responseSize, projectId,
                httpRequest.getTransportType());

        RequestBodyType bodyType = getResponseBodyType(respHeaders);
        String statusCode = String.valueOf(response.getStatusLine().getStatusCode());
        String statusText = response.getStatusLine().getReasonPhrase();
        BigInteger duration = BigInteger.valueOf(afterExecutionDate.getTime() - beforeExecutionDate.getTime());
        return RequestExecutionResponse.builder()
                .id(httpRequest.getId())
                .responseHeaders(headers)
                .body(body)
                .bodyType(bodyType)
                .statusCode(statusCode)
                .statusText(statusText)
                .duration(duration)
                .startedWhen(beforeExecutionDate)
                .executedWhen(afterExecutionDate)
                .cookies(cookies)
                .build();
    }

    RequestBodyType getResponseBodyType(Map<String, List<String>> headersMap) {
        BasicHeader[] responseHeaders = headersMap.entrySet()
                .stream()
                .flatMap(entry -> {
                    String headerName = entry.getKey();
                    List<String> headerValues = entry.getValue();

                    return headerValues.stream().map(value -> new BasicHeader(headerName, value));
                })
                .toArray(BasicHeader[]::new);

        return getResponseBodyType(responseHeaders);
    }

    RequestBodyType getResponseBodyType(Header[] responseHeaders) {
        RequestBodyType bodyType = RequestBodyType.JSON;

        if (responseHeaders != null) {
            Optional<Header> responseContentTypeHeader = Arrays.stream(responseHeaders)
                    .filter(header -> CONTENT_TYPE.equals(header.getName()))
                    .findFirst();
            Optional<Header> contentDisposition = Arrays.stream(responseHeaders)
                    .filter(h -> CONTENT_DISPOSITION.equals(h.getName()))
                    .findFirst();

            bodyType = RequestBodyType.valueOfContentType(
                    responseContentTypeHeader.map(NameValuePair::getValue).orElse(null),
                    contentDisposition.map(NameValuePair::getValue).orElse(null));

            if (isNull(bodyType)) {
                bodyType = RequestBodyType.JSON;
            }
        }

        return bodyType;
    }

    /**
     * Create url with parameters from request.
     *
     * @return url string with parameters
     */
    private String createUrlWithParameters(HttpRequestEntitySaveRequest httpRequest,
                                           RequestRuntimeOptions runtimeOptions) {
        final List<HttpParamSaveRequest> requestParams = httpRequest.getRequestParams();
        final String url = httpRequest.getUrl();

        if (isEmpty(requestParams)) {
            return url;
        }

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(url);
        requestParams.forEach(parameter -> {
            if (!parameter.isDisabled()) {
                final String key = parameter.getKey();
                final String value = parameter.getValue();
                try {
                    if (!runtimeOptions.isDisableAutoEncoding()) {
                        uriComponentsBuilder.queryParam(URLEncoder.encode(key, "UTF-8").replace("+", "%20"),
                                URLEncoder.encode(value, "UTF-8").replace("+", "%20"));
                    } else {
                        uriComponentsBuilder.queryParam(key, value);
                    }
                } catch (UnsupportedEncodingException e) {
                    log.error("Failed to encode query parameter '{}' with value: '{}'", key, value, e);
                    throw new ItfLiteRequestQueryParamEncodingException(key, value);
                }
            }
        });

        return uriComponentsBuilder.build().toUriString();
    }

    /**
     * Get ITF context by itf feign client.
     *
     * @param context request string
     * @return context
     */
    public String getContext(UUID projectId, String context) throws URISyntaxException {
        if (feignClientsProperties.getIsFeignAtpItfEnabled()) {
            log.debug("Getting context: {}", context);
            if (StringUtils.isEmpty(context)) {
                log.debug("Context is empty");
                return "";
            }
            String[] contextArray = context.split(ITF_URL_CONTEXT_SEPARATOR);
            if (contextArray.length > 1) {
                // contextArray[0] --- itf url, contextArray[1] --- contextId
                URI itfUri = getItfUri(contextArray[0]);
                String itfRoute = "";
                return itfPlainFeignClient.getContext(itfUri, itfRoute, projectId, contextArray[1]);
            } else {
                return itfFeignService.getContext(context, projectId);
            }
        }
        return "";
    }

    /**
     * Create itf uri from itf string.
     * If itfString is multi project itf url then it is parsed to replace "configurator" to "executor".
     * And projectId will be also removed
     *
     * @param itfString itf string
     * @return itf uri
     * @throws URISyntaxException produces if can't create URI from string
     */
    private URI getItfUri(String itfString) throws URISyntaxException {
        if (itfString.contains(ITF_CONFIGURATOR)) {
            // split by "project" to remove "/project/{projectId}" string
            // itfArray[0] --- itf url, itfArray[1] --- projectId
            String[] itfArray = itfString.split(ITF_URL_PROJECT_SEPARATOR);
            // change configurator to executor
            String itfExecutor = itfArray[0].replace(ITF_CONFIGURATOR, ITF_EXECUTOR);
            return new URI(itfExecutor);
        }
        return new URI(itfString);
    }

    /**
     * Resolve variables in http request.
     *
     * @param httpRequest http request
     * @param context     itf context
     * @return http request with resolved variables
     */
    public HttpRequest resolveAllVariables(HttpRequest httpRequest, String context,
                                           Boolean isVelocityResolveRequired,
                                           UUID environmentId)
            throws URISyntaxException, AtpDecryptException {
        HttpRequestEntitySaveRequest httpRequestEntitySaveRequest =
                modelMapper.map(httpRequest, HttpRequestEntitySaveRequest.class);
        envParamService.resolveEnvironmentParameters(httpRequestEntitySaveRequest, false, environmentId);
        resolveVelocityVariables(httpRequest, context, isVelocityResolveRequired, httpRequestEntitySaveRequest);

        return modelMapper.map(httpRequestEntitySaveRequest, HttpRequest.class);
    }

    /**
     * Resolve Velocity variables.
     *
     * @param httpRequest               http request
     * @param context                   execution context
     * @param isVelocityResolveRequired velocity resolve flag
     * @param saveRequest               save request
     * @throws URISyntaxException possible exception
     */
    public void resolveVelocityVariables(HttpRequest httpRequest, String context, Boolean isVelocityResolveRequired,
                                         HttpRequestEntitySaveRequest saveRequest) throws URISyntaxException {
        if (isVelocityResolveRequired) {
            final RequestBody body = saveRequest.getBody();
            if (body != null) {
                final RequestBodyType bodyType = body.getType();
                final String content = body.getContent();
                final UUID projectId = httpRequest.getProjectId();
                if (RequestBodyType.Velocity.equals(bodyType)) {
                    final String processedBody = processVelocity(projectId, content, context);
                    body.setContent(processedBody);
                }
            }
        }
    }

    /**
     * Remove special symbols from headers entities names.
     *
     * @param entities list of entities
     */
    public void updateHeadersFields(List<RequestHeader> entities) {
        entities.forEach(entity -> {
            entity.setKey(entity.getKey().trim().replaceAll("[\n\r\t\b]", StringUtils.EMPTY));
            entity.setValue(entity.getValue().trim());
        });
    }

    /**
     * Remove special symbols from parameter entities names.
     *
     * @param entities list of entities
     */
    public void updateParametersFields(List<RequestParam> entities) {
        entities.forEach(entity -> {
            entity.setKey(entity.getKey().trim().replaceAll("[\n\r\t\b]", StringUtils.EMPTY));
            entity.setValue(entity.getValue().trim());
        });
    }

    /**
     * Calculate and set order for the request.
     */
    public void setOrder(Request request) {
        final UUID projectId = request.getProjectId();
        final UUID folderId = request.getFolderId();

        Integer maxOrder = isNull(folderId) ? requestRepository.findMaxOrder(projectId) :
                requestRepository.findMaxOrder(projectId, folderId);

        final Integer calcOrder = nonNull(maxOrder) ? ++maxOrder : 0;
        log.debug("Request order: {}", calcOrder);

        request.setOrder(calcOrder);
    }

    /**
     * Change request order.
     */
    public void order(UUID requestId, RequestOrderChangeRequest request) {
        log.debug("Change order for the request with id '{}', request params: {}", requestId, request);
        final UUID projectId = request.getProjectId();
        final UUID folderId = request.getFolderId();
        final int order = request.getOrder();

        List<Request> requests = requestRepository.findAllByProjectIdAndFolderId(projectId, folderId);
        requests.sort(Comparator.comparing(Request::getOrder, Comparator.nullsLast(Comparator.naturalOrder())));

        Request changedRequest = StreamUtils.find(requests, req -> req.getId().equals(requestId));
        requests.remove(changedRequest);
        requests.add(order, changedRequest);

        int count = 0;
        for (Request requestEntity : requests) {
            requestEntity.setOrder(count++);
        }
        requestRepository.saveAll(requests);
    }

    /**
     * Remove special symbols from headers entities names.
     *
     * @param entities list of entities
     */
    public void updateSaveRequestHeadersFields(List<HttpHeaderSaveRequest> entities) {
        if (!isEmpty(entities)) {
            entities.forEach(entity -> {
                entity.setKey(entity.getKey().trim().replaceAll("[\n\r\t\b]", StringUtils.EMPTY));
                entity.setValue(entity.getValue().trim());
            });
            processContentTypeHeaders(entities);
        }
    }

    private void processContentTypeHeaders(List<HttpHeaderSaveRequest> entities) {
        Predicate<HttpHeaderSaveRequest> isContentTypeHeader = header -> header.getKey().equalsIgnoreCase(CONTENT_TYPE);
        List<HttpHeaderSaveRequest> enabledUserContentTypeHeaders = entities.stream()
                .filter(header -> isContentTypeHeader.test(header) && !header.isGenerated() && !header.isDisabled())
                .collect(Collectors.toList());

        if (!isEmpty(enabledUserContentTypeHeaders)) {
            log.debug("Enabled user content type headers: {}", enabledUserContentTypeHeaders);
            // we have at least one enabled Content-Type header created by User
            // disable all other autogenerated Content-Type headers
            entities.stream()
                    .filter(header -> isContentTypeHeader.test(header) && header.isGenerated() && !header.isDisabled())
                    .forEach(header -> header.setDisabled(true));
        }
    }

    /**
     * Remove special symbols from parameter entities names.
     *
     * @param entities list of entities
     */
    public void updateSaveRequestParametersFields(List<HttpParamSaveRequest> entities) {
        entities.forEach(entity -> {
            entity.setKey(entity.getKey().trim().replaceAll("[\n\r\t\b]", StringUtils.EMPTY));
            entity.setValue(entity.getValue().trim());
        });
    }

    public Request getByProjectIdAndSourceId(UUID projectId, UUID sourceId) {
        return requestRepository.getByProjectIdAndSourceId(projectId, sourceId);
    }

    /**
     * Encode request parameters except environments.
     *
     * @param parameters request parameters that must be encoded
     */
    public void encodeRequestParametersExceptEnv(List<RequestParam> parameters) {
        parameters.forEach(parameter -> {
            final String key = parameter.getKey();
            final String value = parameter.getValue();

            try {
                parameter.setKey(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
                parameter.setValue(envParamService.encodeParameterExceptEnv(value));
            } catch (UnsupportedEncodingException e) {
                log.error("Failed to encode query parameter '{}' with value: '{}'", key, value, e);
                throw new ItfLiteRequestQueryParamEncodingException(key, value);
            }
        });
    }

    /**
     * Retrieve options by requestId.
     *
     * @param requestId request id
     * @return request options
     */
    public RequestRuntimeOptions retrieveRuntimeOptions(UUID requestId) {
        return requestRepository.getRequestRuntimeOptionsById(requestId)
                .orElseThrow(() -> {
                    log.error("Failed to found Request with id: {}", requestId);
                    return new AtpEntityNotFoundException("Request", requestId);
                });
    }

    /**
     * Parses request path.
     * Pay attention that folder names are unique if they are under the same parent folder.
     *
     * @param requestPath request path
     * @return request id
     */
    private UUID parseRequestPath(String requestPath) {
        String splitChar = "/";
        List<String> requestParts = Arrays.stream(requestPath.split(splitChar))
                .map(String::trim).collect(Collectors.toList());
        if (isEmpty(requestParts)) {
            log.error("Request path is empty. Can't get request id");
            return null;
        }

        if (requestParts.size() == 1) {
            List<Request> requests = requestRepository.findAllByName(requestParts.get(0));
            if (!isEmpty(requests)) {
                Request filteredRequest = requests.stream()
                        .filter(request -> request.getFolderId() == null).findAny().orElse(null);
                return filteredRequest != null ? filteredRequest.getId() : null;
            }
        }

        List<Request> requests = requestRepository.findAllByName(requestParts.get(requestParts.size() - 1));
        UUID requestId = null;
        for (Request request : requests) {
            requestId = request.getId();
            UUID currentFolderId = request.getFolderId();
            int i = requestParts.size() - 2;
            while (i > 0) {
                if (currentFolderId == null) {
                    i = 0;
                    requestId = null;
                } else {
                    String currentFolderName = requestParts.get(i);
                    List<Folder> foundFolders = folderService.getFolderByIdAndName(currentFolderId, currentFolderName);
                    if (isEmpty(foundFolders)) {
                        i = 0;
                        requestId = null;
                    } else {
                        i--;
                        // folder by id and name is unique, that's why foundFolders contain only one element
                        currentFolderId = foundFolders.get(0).getParentId();
                    }
                }
            }
        }

        return requestId;
    }

    private String getResponseFilenameFromResponseHeaders(Map<String, List<String>> headersMap) {
        Optional<Map. Entry<String, List<String>>> contentDispositionMapEntryOptional = headersMap.entrySet().stream()
                .filter(entry -> CONTENT_DISPOSITION.equalsIgnoreCase(entry.getKey())).findFirst();
        if (!contentDispositionMapEntryOptional.isPresent()) {
            return null;
        }

        List<String> headerValues = contentDispositionMapEntryOptional.get().getValue();
        if (CollectionUtils.isEmpty(headerValues)) {
            return null;
        }

        BasicHeader basicHeader = new BasicHeader(CONTENT_DISPOSITION, headerValues.get(0));
        HeaderElement[] headerElements = basicHeader.getElements();
        if (headerElements.length > 0) {
            HeaderElement headerElement = headerElements[0];
            if (headerElement.getName().equalsIgnoreCase("attachment")) {
                NameValuePair nameValuePair = headerElement.getParameterByName("filename");
                if (nameValuePair != null) {
                    return nameValuePair.getValue();
                }
            }
        }
        return null;
    }

    private String getExtensionByContentTypeHeader(Map<String, List<String>> headersMap) {
        Optional<Map. Entry<String, List<String>>> contentDispositionMapEntryOptional = headersMap.entrySet().stream()
                .filter(entry -> CONTENT_DISPOSITION.equalsIgnoreCase(entry.getKey())).findFirst();
        if (contentDispositionMapEntryOptional.isPresent()) {
            return null;
        }

        Optional<Map. Entry<String, List<String>>> contentTypeMapEntryOptional = headersMap.entrySet().stream()
                .filter(entry -> CONTENT_TYPE.equalsIgnoreCase(entry.getKey())).findFirst();
        if (!contentTypeMapEntryOptional.isPresent()) {
            return null;
        }

        List<String> headerValues = contentTypeMapEntryOptional.get().getValue();
        BasicHeader basicHeader = new BasicHeader(CONTENT_TYPE, headerValues.get(0));
        HeaderElement[] headerElements = basicHeader.getElements();
        if (headerElements.length > 0) {
            HeaderElement headerElement = headerElements[0];
            String contentType = headerElement.getName();
            MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
            try {
                return allTypes.forName(contentType).getExtension();
            } catch (MimeTypeException e) {
                log.warn("Can't get extension by contentType = {}", contentType);
            }
        }
        return null;
    }
}
