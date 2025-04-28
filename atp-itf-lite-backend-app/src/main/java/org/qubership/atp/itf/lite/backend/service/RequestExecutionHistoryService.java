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

import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.CustomRequestExecutionRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestExecutionDetailsRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestExecutionRepository;
import org.qubership.atp.itf.lite.backend.enums.TestingStatus;
import org.qubership.atp.itf.lite.backend.enums.ValueType;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.exceptions.jsengine.ItfLiteScriptEnginePostScriptExecutionException;
import org.qubership.atp.itf.lite.backend.exceptions.jsengine.ItfLiteScriptEnginePreScriptExecutionException;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanExecuteScriptResponseDto;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.JsExecutionResult;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionResponse;
import org.qubership.atp.itf.lite.backend.model.entities.FileBody;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.qubership.atp.itf.lite.backend.model.entities.history.HistoryRequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.history.HistoryRequestDetailsResponse;
import org.qubership.atp.itf.lite.backend.model.entities.history.HistorySearchRequest;
import org.qubership.atp.itf.lite.backend.model.entities.history.HttpRequestExecutionDetails;
import org.qubership.atp.itf.lite.backend.model.entities.history.PaginatedResponse;
import org.qubership.atp.itf.lite.backend.model.entities.history.RequestExecution;
import org.qubership.atp.itf.lite.backend.model.entities.history.RequestExecutionDetails;
import org.qubership.atp.itf.lite.backend.model.entities.history.TestStatus;
import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.qubership.atp.itf.lite.backend.utils.RequestUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestExecutionHistoryService extends CrudService<RequestExecution> {

    private final ModelMapper modelMapper = new ModelMapper();
    private final UserService userService;
    private final RequestExecutionDetailsRepository detailsRepository;
    private final CustomRequestExecutionRepository customRequestExecutionRepository;
    private final RequestExecutionRepository requestExecutionRepository;
    private final GridFsService gridFsService;

    @Override
    protected JpaRepository<RequestExecution, UUID> repository() {
        return requestExecutionRepository;
    }

    /**
     * Log request execution.
     *
     * @param token         token
     * @param sseId         sseId
     * @param request       request
     * @param response      response
     * @param errorMessage  exception
     * @param formDataFiles formDataFiles
     */
    @Transactional
    public void logRequestExecution(String token, UUID sseId, RequestEntitySaveRequest request,
                                    RequestExecutionResponse response, Exception errorMessage,
                                    List<FileData> formDataFiles) {
        if (request instanceof HttpRequestEntitySaveRequest) {
            logHttpRequestExecution(token, sseId, (HttpRequestEntitySaveRequest) request, response, errorMessage,
                    formDataFiles);
        }
    }

    /**
     * Function for log request. Get execution details, update and save.
     */
    @Transactional
    public JsExecutionResult logRequestJsExecution(String token, UUID sseId, RequestEntitySaveRequest request,
                                                   PostmanExecuteScriptResponseDto jsExecutionResults,
                                                   boolean isPreScript) {
        Optional<RequestExecutionDetails> detailsOptional = detailsRepository.findByRequestExecutionSseId(sseId);
        RequestExecutionDetails details = detailsOptional
                .orElseGet(() -> generateAndConfigureRequestExecutionDetails(request, token, sseId));
        if (request instanceof HttpRequestEntitySaveRequest) {
            if (((HttpRequestEntitySaveRequest) request).getBody() != null
                    && RequestBodyType.GraphQL.equals(((HttpRequestEntitySaveRequest) request).getBody().getType())) {
                HistoryRequestBody requestBody = new HistoryRequestBody();
                requestBody.setContent(((HttpRequestEntitySaveRequest) request).getBody().computeAndGetContent());
                requestBody.setType(RequestBodyType.GraphQL);
                details.setRequestBody(requestBody);
            }
        }
        JsExecutionResult returnValue = new JsExecutionResult(true, null);
        if (Objects.nonNull(jsExecutionResults)) {
            log.info("Write JS script execution into DB");
            if (jsExecutionResults.getTestResults() != null) {
                List<TestStatus> testsStatuses = new ArrayList<>();
                jsExecutionResults.getTestResults().forEach(testResult -> {
                    log.debug("Logging JS script execution into DB: {}", testResult);
                    boolean isFailed = !testResult.getPassed() || testResult.getError() != null;
                    if (isFailed) {
                        returnValue.setPassed(false);
                    }
                    Boolean skipped = testResult.getSkipped();
                    if (Objects.isNull(skipped) || !skipped) {
                        testsStatuses.add(new TestStatus(
                                testResult.getName(),
                                isFailed ? TestingStatus.FAILED : TestingStatus.PASSED,
                                isFailed ? testResult.getError().getMessage() : ""));
                    }
                });
                List<TestStatus> tests = details.getRequestTests();
                if (Objects.nonNull(tests)) {
                    tests.addAll(testsStatuses);
                } else {
                    tests = testsStatuses;
                }
                details.setRequestTests(tests);
                details.getRequestExecution().setTestingStatus(
                        returnValue.isPassed() ? TestingStatus.PASSED : TestingStatus.FAILED);
            }
            if (jsExecutionResults.getConsoleLogs() != null) {
                jsExecutionResults.getConsoleLogs().forEach(details::addConsoleLog);
                returnValue.setConsoleLogs(jsExecutionResults.getConsoleLogs());
            }
            if (!returnValue.isPassed()) {
                if (isPreScript) {
                    details.setErrorMessage(RequestUtils.getErrorResponse(
                            new ItfLiteScriptEnginePreScriptExecutionException()));
                } else {
                    details.setErrorMessage(RequestUtils.getErrorResponse(
                            new ItfLiteScriptEnginePostScriptExecutionException()));
                }
            }
            detailsRepository.save(details);
        }
        return returnValue;
    }

    /**
     * Generate and initial configure request execution details. Add execution into details.
     * @param request request
     * @param token token
     * @param sseId sseId
     * @return initially configured request execution details
     */
    public RequestExecutionDetails generateAndConfigureRequestExecutionDetails(
            RequestEntitySaveRequest request, String token, UUID sseId) {
        RequestExecutionDetails details = new HttpRequestExecutionDetails();
        String executor = getUserInformation(token);
        RequestExecution execution = new RequestExecution(executor, sseId, request, null, null);
        details.setRequestExecution(execution);
        return details;
    }

    /**
     * Log http request execution.
     *
     * @param token    user token
     * @param request  request
     * @param response response
     */
    @Transactional
    public void logHttpRequestExecution(String token, UUID sseId, HttpRequestEntitySaveRequest request,
                                        RequestExecutionResponse response, Exception errorMessage,
                                        List<FileData> formDataFiles) {
        Optional<RequestExecutionDetails> detailsOptional = detailsRepository.findByRequestExecutionSseId(sseId);
        HistoryRequestBody requestBody = null;
        boolean bodyExists = nonNull(request.getBody());
        if (request.getFile() != null && request.getFile().getContent() != null) {
            FileData fileData = request.getFile();
            UUID fileId = gridFsService.saveHistoryBinary(LocalDateTime.now().toString(),
                    new ByteArrayInputStream(fileData.getContent()), fileData.getFileName());
            requestBody = new HistoryRequestBody();
            requestBody.setBinaryBody(new FileBody(fileData.getFileName(), fileId));
            requestBody.setType(bodyExists ? request.getBody().getType() : RequestBodyType.Binary);
        } else if (bodyExists) {
            requestBody = new HistoryRequestBody();
            requestBody.setType(request.getBody().getType());
            requestBody.setContent(request.getBody().getContent());
            if (Objects.nonNull(request.getBody().getFormDataBody())) {
                request.getBody().getFormDataBody()
                        .stream()
                        .filter(fdb -> ValueType.FILE.equals(fdb.getType()) && Objects.nonNull(fdb.getFileId()))
                        .forEach(fdb -> fdb.setFileId(gridFsService.copyFileById(fdb.getFileId(), sseId)));
                fillFormDataParts(sseId, request.getBody().getFormDataBody(), formDataFiles);
                requestBody.setFormDataBody(request.getBody().getFormDataBody());
            }
        }
        RequestExecutionDetails details;
        if (detailsOptional.isPresent()) {
            details = detailsOptional.get();
            RequestExecution execution = details.getRequestExecution();
            execution.update(request, response);
            ((HttpRequestExecutionDetails) details).update(execution, request, response, errorMessage, requestBody);
        } else {
            String executor = getUserInformation(token);
            RequestExecution execution = new RequestExecution(executor, sseId, request, response, null);
            details = new HttpRequestExecutionDetails(execution, request, response, errorMessage, requestBody);
        }
        detailsRepository.save(details);
    }

    private void fillFormDataParts(UUID sseId, List<FormDataPart> formDataParts,
                                   List<FileData> files) {
        if (!CollectionUtils.isEmpty(formDataParts) && !CollectionUtils.isEmpty(files)) {
            // Creates fileId for new added files
            // set it to fileId field
            // and save file with fileId in metadata
            int i = 0;
            for (FormDataPart fdp : formDataParts) {
                if (ValueType.FILE.equals(fdp.getType()) && Objects.isNull(fdp.getFileId()) && i < files.size()) {
                    FileData f = files.get(i);
                    if (Objects.nonNull(f)) {
                        UUID fileId = UUID.randomUUID();
                        fdp.setFileId(fileId);
                        fdp.setFileSize(f.getContent().length);
                        gridFsService.saveFileByRequestId(LocalDateTime.now().toString(), sseId,
                                new ByteArrayInputStream(f.getContent()), f.getFileName(), fileId);
                    }
                    i++;
                }
            }
        }
    }

    /**
     * Get user information.
     *
     * @param token token
     * @return user first and second names details
     */
    public String getUserInformation(String token) {
        UserInfo userInfo = userService.getUserInfoByToken(token);
        String executor;
        if (Objects.isNull(userInfo)) {
            executor = "Unknown User";
        } else {
            executor = userInfo.getFirstName() + " " + userInfo.getLastName();
        }
        return executor;
    }

    /**
     * Get execution requests high level information.
     *
     * @param request request entity
     * @return execution requests information
     */
    public PaginatedResponse<RequestExecution> getExecutionHistory(HistorySearchRequest request) {
        return customRequestExecutionRepository.findAllRequestExecutions(request);
    }

    /**
     * Get execution request low level information by history item id.
     *
     * @param historyItemId history request entity id
     * @return execution request information
     */
    public HistoryRequestDetailsResponse getExecutionHistoryDetailsByHistoryItemId(UUID historyItemId) {
        RequestExecution requestExecution = get(historyItemId);
        RequestExecutionDetails requestExecutionDetails = detailsRepository.findByRequestExecution(requestExecution);

        HistoryRequestDetailsResponse historyRequestDetailsResponse = modelMapper
                .map(requestExecutionDetails, HistoryRequestDetailsResponse.class);
        if (requestExecutionDetails.getErrorMessage() != null) {
            historyRequestDetailsResponse.setErrorMessage(requestExecutionDetails.getErrorMessage().getMessage());
        }
        if (Objects.isNull(historyRequestDetailsResponse.getResponseBody())
                && Objects.nonNull(requestExecutionDetails.getResponseBodyByte())) {
            historyRequestDetailsResponse.setResponseBody(new String(
                    requestExecutionDetails.getResponseBodyByte(), StandardCharsets.UTF_8));
        }
        return historyRequestDetailsResponse;
    }

    /**
     * Get binary file from history by metadata.fileId.
     *
     * @param fileId   file id.
     * @param response http servlet for send response.
     */
    public void getBinaryFileHistory(UUID fileId, HttpServletResponse response) throws IOException {
        log.debug("Start download file by id {}", fileId);
        Optional<FileData> requestBinaryFile = gridFsService.downloadFileByFileId(fileId);
        if (!requestBinaryFile.isPresent()) {
            log.warn("File with id {} not found", fileId);
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        ServletOutputStream responseOutputStream = response.getOutputStream();
        responseOutputStream.write(requestBinaryFile.get().getContent());
        response.setHeader(CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"",
                requestBinaryFile.get().getFileName()));
        response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
        response.flushBuffer();
    }

    /**
     * Get execution request low level information by sse id.
     *
     * @param sseId sse id
     * @return execution request information
     */
    public RequestExecutionDetails getExecutionHistoryDetailsBySseId(UUID sseId) {
        return detailsRepository.findByRequestExecutionSseId(sseId).orElse(null);
    }

    /**
     * CleanUp overdue request execution history.
     *
     * @param shift number of days for history retention
     */
    @Transactional
    public int cleanUpRequestExecutionHistory(int shift) {
        Calendar calendar = Calendar.getInstance();
        int minusDays = shift > 0 ? shift * -1 : shift;
        calendar.add(Calendar.DAY_OF_MONTH, minusDays);
        return requestExecutionRepository.deleteByExecutedWhenBefore(new Timestamp(calendar.getTimeInMillis()));
    }

    /**
     * Find all executors in execution history by projectId.
     *
     * @param projectId project identifier
     * @return list of executors
     */
    public List<String> getExecutorsInRequestExecutionHistory(UUID projectId) {
        return requestExecutionRepository.findByProjectId(projectId);
    }
}
