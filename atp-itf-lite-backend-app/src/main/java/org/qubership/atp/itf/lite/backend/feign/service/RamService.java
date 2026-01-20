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

package org.qubership.atp.itf.lite.backend.feign.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.RAM_DOWNLOAD_FILE_PATH;

import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.adapter.common.adapters.AtpKafkaRamAdapter;
import org.qubership.atp.adapter.common.adapters.providers.RamAdapterProvider;
import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.context.TestRunContextHolder;
import org.qubership.atp.adapter.common.entities.Message;
import org.qubership.atp.adapter.executor.executor.AtpRamWriter;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpException;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.enums.ValueType;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteIllegalTestRunsCountInExecutionRequestException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteIncorrectImportContextVariablesRequest;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteIncorrectImportRequest;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteTestRunsNotFoundException;
import org.qubership.atp.itf.lite.backend.feign.clients.RamExecutionRequestFeignClient;
import org.qubership.atp.itf.lite.backend.feign.clients.RamLogRecordFeignClient;
import org.qubership.atp.itf.lite.backend.feign.clients.RamTestPlansFeignClient;
import org.qubership.atp.itf.lite.backend.feign.clients.RamTestRunsFeignClient;
import org.qubership.atp.itf.lite.backend.feign.dto.ConsoleLogDto;
import org.qubership.atp.itf.lite.backend.feign.dto.ContextVariableDto;
import org.qubership.atp.itf.lite.backend.feign.dto.LogRecordDto;
import org.qubership.atp.itf.lite.backend.feign.dto.LogRecordFilteringRequestDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanExecuteScriptResponseDto;
import org.qubership.atp.itf.lite.backend.feign.dto.RequestDto;
import org.qubership.atp.itf.lite.backend.feign.dto.RequestHeaderDto;
import org.qubership.atp.itf.lite.backend.feign.dto.ResponseDto;
import org.qubership.atp.itf.lite.backend.feign.dto.RestLogRecordDto;
import org.qubership.atp.itf.lite.backend.feign.dto.TestPlanDto;
import org.qubership.atp.itf.lite.backend.feign.dto.TestPlansSearchRequestDto;
import org.qubership.atp.itf.lite.backend.feign.dto.TestRunDto;
import org.qubership.atp.itf.lite.backend.feign.dto.TypeActionDto;
import org.qubership.atp.itf.lite.backend.model.api.request.ExecutionCollectionRequestExecuteRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.ImportContextRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.ImportFromRamRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.ImportContextResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.JsExecutionResult;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.qubership.atp.itf.lite.backend.service.GridFsService;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.qubership.atp.itf.lite.backend.utils.CookieUtils;
import org.qubership.atp.itf.lite.backend.utils.StreamUtils;
import org.qubership.atp.itf.lite.backend.utils.UrlParsingUtils;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.ScriptConsoleLog;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.qubership.atp.ram.models.logrecords.parts.Request;
import org.qubership.atp.ram.models.logrecords.parts.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RamService {

    private final RamTestPlansFeignClient testPlansFeignClient;
    private final RamTestRunsFeignClient ramTestRunsFeignClient;
    private final RamLogRecordFeignClient ramLogRecordFeignClient;
    private final RamExecutionRequestFeignClient ramExecutionRequestFeignClient;

    private final GridFsService gridFsService;
    private final String fileDownloadLink;

    private static final String DEFAULT_ITF_LITE_RUN_COLLECTION_TEST_PLAN_NAME = "ITF Lite Default";

    /**
     * Find or create find default Itf Lite run collection test plan.
     */
    public UUID getDefaultCollectionRunTestPlanId(UUID projectId) {
        final List<TestPlanDto> defaultRunCollectionTestPlans = testPlansFeignClient.search(
                new TestPlansSearchRequestDto()
                        .name(DEFAULT_ITF_LITE_RUN_COLLECTION_TEST_PLAN_NAME)
                        .projectId(projectId)
        ).getBody();
        if (CollectionUtils.isEmpty(defaultRunCollectionTestPlans)) {
            log.warn("Failed to find default Itf Lite run collection test plan");
            final TestPlanDto newTestPlan = new TestPlanDto()
                    .name(DEFAULT_ITF_LITE_RUN_COLLECTION_TEST_PLAN_NAME)
                    .projectId(projectId);
            log.debug("Creating new default Itf Lite run collection test plan");
            // TODO: refactor to actual method
            final TestPlanDto createdTestPlan = testPlansFeignClient.createTestPlan(newTestPlan).getBody();
            if (nonNull(createdTestPlan)) {
                return createdTestPlan.getUuid();
            } else {
                throw new AtpException(
                        String.format("Default Test Plan with name \"%s\" not found and cannot be created",
                                DEFAULT_ITF_LITE_RUN_COLLECTION_TEST_PLAN_NAME));
            }
        } else {
            final TestPlanDto existedDefaultTestPlan = StreamUtils.getFirstElem(defaultRunCollectionTestPlans);
            log.debug("Found new default Itf Lite run collection test plan: {}", existedDefaultTestPlan.getUuid());
            return existedDefaultTestPlan.getUuid();
        }
    }

    /**
     * Updates action entity's log record name.
     * Replaces UUID by request name
     *
     * @param requestExecuteRequest request execute request
     * @param newName               action new name
     */
    public void updateExecutionLogRecordName(ExecutionCollectionRequestExecuteRequest requestExecuteRequest,
                                             String newName) {
        Timestamp startDate = Objects.nonNull(requestExecuteRequest.getSection().getStartDate())
                ? requestExecuteRequest.getSection().getStartDate() : new Timestamp(new Date().getTime());
        AtpRamWriter.getAtpRamWriter().getAdapter().updateExecutionStatus(
                requestExecuteRequest.getSection().getSectionId(),
                ExecutionStatuses.IN_PROGRESS.name(),
                startDate,
                newName);
    }

    /**
     * Create list of context variables with context value before and after execution.
     *
     * @param beforeContext context before execution step
     * @param afterContext  context after execution step
     */
    public List<ContextVariable> getContextVariables(Map<String, Object> beforeContext,
                                                     Map<String, Object> afterContext) {
        List<ContextVariable> contextVariables = new ArrayList<>();
        if (!CollectionUtils.isEmpty(beforeContext)) {
            beforeContext.forEach((k, v) ->
                    contextVariables.add(new ContextVariable(k,
                            Objects.isNull(v) ? null : v.toString(),
                            CollectionUtils.isEmpty(afterContext) || Objects.isNull(afterContext.get(k))
                                    ? null : afterContext.get(k).toString())));
        }
        if (!CollectionUtils.isEmpty(afterContext)) {
            afterContext.forEach((k, v) -> {
                if (CollectionUtils.isEmpty(beforeContext) || !beforeContext.containsKey(k)) {
                    contextVariables.add(new ContextVariable(k,
                            null,
                            Objects.isNull(v) ? null : v.toString()));
                }
            });
        }
        return contextVariables;
    }

    /**
     * Fill context RAM.
     * Should be called once per execution
     */
    public TestRunContext provideInfo(ExecutionCollectionRequestExecuteRequest requestExecuteRequest) {
        TestRunContext ram2Context = TestRunContextHolder.getContext(requestExecuteRequest.getTestRunId().toString());
        log.info("Test run ID from context = {}", ram2Context.getTestRunId());
        ram2Context.setAtpTestRunId(requestExecuteRequest.getTestRunId().toString());
        ram2Context.setAtpExecutionRequestId(requestExecuteRequest.getExecutionRequestId().toString());
        ram2Context.setAtpCompaund(updateCompoundStatuses(ram2Context.getAtpCompaund(),
                requestExecuteRequest.getSection()));
        ram2Context.setTestPlanId(requestExecuteRequest.getTestPlanId().toString());
        ram2Context.setProjectId(requestExecuteRequest.getProjectId().toString());
        ram2Context.getSections().clear();
        log.info("ER_INFO: {}", ram2Context.getAtpExecutionRequestId());
        log.info("PROJECT_TESTPLAN_INFO: {} {}", ram2Context.getProjectId(), ram2Context.getTestPlanId());
        log.info("Section ID [{}]", ram2Context.getCurrentSectionId());
        AtpRamWriter.getAtpRamWriter().createContext(requestExecuteRequest.getTestRunId().toString());
        return ram2Context;
    }

    private AtpCompaund updateCompoundStatuses(AtpCompaund contextCompaund, AtpCompaund requestCompaund) {
        try {
            HashMap<String, AtpCompaund> lastCompaundTable = compaundToHash(contextCompaund);
            AtpCompaund currentCompaund = requestCompaund;
            while (nonNull(currentCompaund)) {
                AtpCompaund currentRequestCompaund = lastCompaundTable.get(currentCompaund.getSectionId());
                if (currentRequestCompaund != null
                        && (currentCompaund.getTestingStatuses() == null
                        || TestingStatuses.UNKNOWN.equals(currentCompaund.getTestingStatuses()))) {
                    currentCompaund.setTestingStatuses(currentRequestCompaund.getTestingStatuses());
                    log.trace("Set compaund {} to status {}", currentCompaund.getSectionId(),
                            currentCompaund.getTestingStatuses());
                }
                currentCompaund = currentCompaund.getParentSection();
            }
        } catch (NullPointerException e) {
            log.error("Invalid compaund, unable to update status");
        }
        return requestCompaund;
    }

    private HashMap<String, AtpCompaund> compaundToHash(AtpCompaund contextCompaund) {
        HashMap<String, AtpCompaund> result = new HashMap<>();
        AtpCompaund currentCompaund = contextCompaund;
        while (nonNull(currentCompaund)) {
            result.put(currentCompaund.getSectionId(), currentCompaund);
            currentCompaund = currentCompaund.getParentSection();
        }
        return result;
    }

    public void updateMessageAndTestingStatus(String message, TestingStatuses status) {
        AtpRamWriter.getAtpRamWriter().updateMessageTestingStatusAndFiles(message, status, null);
    }

    /**
     * Write itf log record with request execution results.
     *
     * @param request                  request
     * @param requestExecutionResponse response
     */
    public void writeRequestExecutionResult(UUID transportLogRecordId,
                                            RequestEntitySaveRequest request,
                                            RequestExecutionResponse requestExecutionResponse,
                                            Exception errorMessage,
                                            TestingStatuses testingStatus) {
        log.debug("Write LogRecord into RAM adapter");
        org.qubership.atp.adapter.common.entities.Message resultMessage =
                new org.qubership.atp.adapter.common.entities.Message(transportLogRecordId.toString(),
                        null, request.getName(), "", testingStatus.toString(),
                        TypeAction.TRANSPORT.toString(), false);
        if (errorMessage != null) {
            resultMessage.setMessage(errorMessage.getMessage());
        }
        resultMessage.setRequest(createLogRecordPartsRequest(request, requestExecutionResponse,
                resultMessage.getUuid()));
        resultMessage.setResponse(createLogRecordPartsResponse(request, requestExecutionResponse));
        resultMessage.setProtocolType(request.getTransportType().toString());
        resultMessage.setItfLiteRequestId(request.getId());
        resultMessage.setIsPreScriptPresent(Strings.isNotBlank(request.getPreScripts()));
        resultMessage.setIsPostScriptPresent(Strings.isNotBlank(request.getPostScripts()));
        setTimestampInMessage(requestExecutionResponse, resultMessage);
        log.debug("Send rest message with id = {} into ram adapter", resultMessage.getUuid());
        AtpRamWriter atpRamWriter = AtpRamWriter.getAtpRamWriter();
        atpRamWriter.writeLogRecordWithParentSections(atpRamWriter.getAdapter()::restMessage, resultMessage);
    }

    private void setTimestampInMessage(RequestExecutionResponse requestExecutionResponse,
                                       org.qubership.atp.adapter.common.entities.Message resultMessage) {
        if (requestExecutionResponse != null) {
            resultMessage.setStartDate(requestExecutionResponse.getStartedWhen() != null
                    ? new Timestamp(requestExecutionResponse.getStartedWhen().getTime())
                    : null);
            resultMessage.setEndDate(requestExecutionResponse.getExecutedWhen() != null
                    ? new Timestamp(requestExecutionResponse.getExecutedWhen().getTime())
                    : null);
        }
    }

    /**
     * Write console log for record.
     *
     * @param transportRecordId transport log record ID
     * @param preScript         pre script value
     * @param postScript        post script value
     * @param consoleLogs       consoleLogs
     */
    public void writeConsoleLogs(UUID transportRecordId, String preScript, String postScript,
                                 List<ConsoleLogDto> consoleLogs) {
        if (preScript != null ||  postScript != null) {
            AtpKafkaRamAdapter atpKafkaRamAdapter = (AtpKafkaRamAdapter) RamAdapterProvider.getNewAdapter("kafka");
            atpKafkaRamAdapter.sendScriptConsoleLogs(
                    consoleLogs.stream().map(c -> new ScriptConsoleLog(c.getMessage(), c.getTimestamp(), c.getLevel()))
                            .collect(Collectors.toList()),
                    preScript,
                    postScript,
                    transportRecordId.toString());
        }
    }

    /**
     * Write itf log record with request execution results.
     *
     * @param jsExecutionResult List of {@link PostmanExecuteScriptResponseDto}
     */
    public JsExecutionResult writeTestsResults(PostmanExecuteScriptResponseDto jsExecutionResult, boolean isPreScript) {
        JsExecutionResult returnValue = new JsExecutionResult(true, null);
        if (Objects.nonNull(jsExecutionResult)) {
            returnValue.setConsoleLogs(jsExecutionResult.getConsoleLogs());
            if (Objects.nonNull(jsExecutionResult.getTestResults())) {
                log.info("Write JS script execution into RAM adapter");
                jsExecutionResult.getTestResults().forEach(testResult -> {
                    log.debug("Logging JS script execution into RAM Adapter: {}", testResult);
                    boolean isFailed = !testResult.getPassed() || testResult.getError() != null;
                    if (isFailed) {
                        returnValue.setPassed(false);
                    }
                    String testingStatus = isFailed ? "FAILED" : "PASSED";
                    String message = isFailed ? testResult.getError().getMessage() : "";
                    org.qubership.atp.adapter.common.entities.Message resultMessage =
                            new org.qubership.atp.adapter.common.entities.Message(
                                    null,
                                    null,
                                    testResult.getName(),
                                    message,
                                    testingStatus,
                                    TypeAction.TECHNICAL.toString(),
                                    false);
                    log.info("Send rest message with id = {} into ram adapter", resultMessage.getUuid());
                    AtpRamWriter atpRamWriter = AtpRamWriter.getAtpRamWriter();
                    atpRamWriter.writeLogRecordWithParentSections(atpRamWriter.getAdapter()::message, resultMessage);
                });
            }
        }
        return returnValue;
    }

    /**
     * Creates log records parts request from itf lite request.
     *
     * @param request                  itf lite request
     * @param requestExecutionResponse request execution response
     * @return log records parts request
     */
    private org.qubership.atp.ram.models.logrecords.parts.Request createLogRecordPartsRequest(
            RequestEntitySaveRequest request, RequestExecutionResponse requestExecutionResponse, String logRecordId) {
        org.qubership.atp.ram.models.logrecords.parts.Request logRecordPartsRequest =
                new org.qubership.atp.ram.models.logrecords.parts.Request();
        if (nonNull(requestExecutionResponse) && nonNull(requestExecutionResponse.getStartedWhen())) {
            logRecordPartsRequest.setTimestamp(new Timestamp(requestExecutionResponse.getStartedWhen().getTime()));
        }
        if (TransportType.REST.equals(request.getTransportType())
                || TransportType.SOAP.equals(request.getTransportType())) {
            HttpRequestEntitySaveRequest httpRequest = (HttpRequestEntitySaveRequest) request;
            if (nonNull(httpRequest.getBody())) {
                StringJoiner sj = new StringJoiner("\n");
                FileData fileData = httpRequest.getFile();
                prepareRequestBody(sj, logRecordId, httpRequest.getBody(), fileData, logRecordPartsRequest);
                logRecordPartsRequest.setBody(sj.toString());
            }
            if (nonNull(httpRequest.getUrl())) {
                logRecordPartsRequest.setEndpoint(httpRequest.getUrlWithQueryParameters());
            }
            if (nonNull(httpRequest.getHttpMethod())) {
                logRecordPartsRequest.setMethod(httpRequest.getHttpMethod().toString());
            }
            if (!CollectionUtils.isEmpty(httpRequest.getRequestHeaders())) {
                logRecordPartsRequest.setHeadersList(httpRequest.getRequestHeaders()
                        .stream()
                        .map(headerPair -> new org.qubership.atp.ram.models.logrecords.parts.RequestHeader(
                                headerPair.getKey(), headerPair.getValue(), ""))
                        .collect(Collectors.toList()));
            }
        }

        return logRecordPartsRequest;
    }

    private void prepareRequestBody(StringJoiner sj, String logRecordId, RequestBody body, FileData binary,
                                    Request logRecordPartsRequest) {
        if (body.getType() != null) {
            switch (body.getType()) {
                case Binary:
                    if (nonNull(binary)) {
                        sj.add(createLinkToDownloadFile(logRecordId, null, binary));
                        AtpRamWriter.getAtpRamWriter().uploadFileForLogRecord(logRecordId,
                                new ByteArrayInputStream(binary.getContent()), binary.getFileName());
                    }
                    logRecordPartsRequest.setHtmlBody(true);
                    return;
                case FORM_DATA:
                    List<FormDataPart> fdps = body.getFormDataBody();
                    if (!CollectionUtils.isEmpty(fdps)) {
                        fdps.forEach(fdp -> addFromDataPart(sj, logRecordId, fdp));
                    }
                    logRecordPartsRequest.setHtmlBody(true);
                    return;
                default:
            }
        }
        sj.add(body.getContent() == null ? "" : body.getContent());
    }

    private void addFromDataPart(StringJoiner sj, String logRecordId, FormDataPart fdp) {
        if (ValueType.TEXT.equals(fdp.getType())) {
            sj.add(String.format("%s: %s", fdp.getKey(), fdp.getValue()));
        } else {
            UUID fileId = fdp.getFileId();
            if (nonNull(fileId)) {
                Optional<FileData> optFile = gridFsService.downloadFileByFileId(fileId);
                if (optFile.isPresent()) {
                    FileData file = optFile.get();
                    sj.add(String.format("%s: %s", fdp.getKey(),
                            createLinkToDownloadFile(logRecordId, fdp.getFileId(), file)));
                    AtpRamWriter.getAtpRamWriter().uploadFileForLogRecord(logRecordId,
                            new ByteArrayInputStream(file.getContent()), fdp.getFileId().toString());
                }
            } else {
                // if value type is file, but file not uploaded
                sj.add(fdp.getKey() + ":");
            }
        }
    }

    private String createLinkToDownloadFile(String logRecordId, @Nullable UUID fileId, FileData file) {
        StringBuilder sb = new StringBuilder();
        sb.append("<a href=").append(fileDownloadLink).append(RAM_DOWNLOAD_FILE_PATH).append(logRecordId)
                .append("?fileName=");
        if (nonNull(fileId)) {
            sb.append(fileId);
        } else {
            sb.append(file.getFileName());
        }
        sb.append(" target=\"_blank\">").append(file.getFileName()).append("</a>");
        return sb.toString();
    }

    /**
     * Generate link to file and upload file to RAM.
     *
     * @param fileData              file
     * @param logRecordPartsRequest LR request
     * @param logRecordId           ID log record
     * @param httpRequest           executed request
     */
    private void loadFileForLogRecord(FileData fileData,
                                      org.qubership.atp.ram.models.logrecords.parts.Request logRecordPartsRequest,
                                      String logRecordId, HttpRequestEntitySaveRequest httpRequest) {
        if (fileData != null) {
            StringJoiner body = new StringJoiner(
                    httpRequest.getBody() == null ? "" : httpRequest.getBody().getContent() + "\n");
            body.add("<a href=").add(fileDownloadLink).add(RAM_DOWNLOAD_FILE_PATH).add(logRecordId).add(">")
                    .add(fileData.getFileName()).add("</a>");
            logRecordPartsRequest.setBody(body.toString());
            AtpRamWriter.getAtpRamWriter().uploadFileForLogRecord(logRecordId,
                    new ByteArrayInputStream(fileData.getContent()), fileData.getFileName());
        }
    }

    /**
     * Creates response from request and request execution response.
     *
     * @param request                  request
     * @param requestExecutionResponse request execution response
     * @return response for itf log record
     */
    private Response createLogRecordPartsResponse(RequestEntitySaveRequest request,
                                                  RequestExecutionResponse requestExecutionResponse) {
        Response response = new Response();
        if (nonNull(requestExecutionResponse) && nonNull(requestExecutionResponse.getExecutedWhen())) {
            response.setTimestamp(new Timestamp(requestExecutionResponse.getExecutedWhen().getTime()));
        } else {
            log.debug("requestExecutionResponse's executedWhen is null. Set current time in response");
            response.setTimestamp(new Timestamp(new Date().getTime()));
        }
        if (nonNull(requestExecutionResponse)) {
            response.setBody(requestExecutionResponse.getBody());
            response.setCode(requestExecutionResponse.getStatusCode());
            if (nonNull(requestExecutionResponse.getResponseHeaders())) {
                response.setHeadersList(requestExecutionResponse.getResponseHeaders().stream()
                        .map(headerPair -> new org.qubership.atp.ram.models.logrecords.parts.RequestHeader(
                                headerPair.getKey(), headerPair.getValue(), ""))
                        .collect(Collectors.toList()));
            }
        }
        if (TransportType.REST.equals(request.getTransportType())
                || TransportType.SOAP.equals(request.getTransportType())) {
            response.setEndpoint(((HttpRequestEntitySaveRequest) request).getUrlWithQueryParameters());
        }
        return response;
    }

    /**
     * Import context variables from ram.
     * @param request import request
     * @return imported context variables
     */
    public ImportContextResponse importContextVariables(ImportContextRequest request) {
        if (request == null || request.getImportEntityType() == null || request.getImportEntityId() == null) {
            throw new ItfLiteIncorrectImportContextVariablesRequest(new Gson().toJson(request));
        }
        ResponseEntity<List<ContextVariableDto>> contextVariableDtoResponse = null;
        if (request.getImportEntityType().equals(ImportContextRequest.ImportEntityType.EXECUTION_REQUEST)) {
            UUID executionRequestId = request.getImportEntityId();
            ResponseEntity<Map<String, List<TestRunDto>>> testRunsResponse = ramExecutionRequestFeignClient
                    .getAllTestRunsBySomeExecutionRequests(executionRequestId.toString());
            if (testRunsResponse.getBody() == null || CollectionUtils.isEmpty(testRunsResponse.getBody()
                    .get(executionRequestId.toString()))) {
                throw new ItfLiteTestRunsNotFoundException(executionRequestId);
            }
            List<TestRunDto> testRunDto = testRunsResponse.getBody().get(executionRequestId.toString());
            if (testRunDto.size() > 1) {
                throw new ItfLiteIllegalTestRunsCountInExecutionRequestException(executionRequestId);
            }
            contextVariableDtoResponse =
                    ramTestRunsFeignClient.getAllContextVariables(testRunDto.get(0).getUuid());
        }

        if (request.getImportEntityType().equals(ImportContextRequest.ImportEntityType.TEST_RUN)) {
            contextVariableDtoResponse =
                    ramTestRunsFeignClient.getAllContextVariables(request.getImportEntityId());
        }
        if (request.getImportEntityType().equals(ImportContextRequest.ImportEntityType.LOG_RECORD)) {
            contextVariableDtoResponse =
                    ramLogRecordFeignClient.getAllContextVariables(request.getImportEntityId());
        }
        return generateImportContextResponse(contextVariableDtoResponse == null
                ? null : contextVariableDtoResponse.getBody());
    }

    private ImportContextResponse generateImportContextResponse(List<ContextVariableDto> contextVariableDto) {
        List<org.qubership.atp.itf.lite.backend.model.api.request.ContextVariable> contextVariables =
                CollectionUtils.isEmpty(contextVariableDto)
                        ? Collections.emptyList()
                        : contextVariableDto.stream()
                        .map(org.qubership.atp.itf.lite.backend.model.api.request.ContextVariable::new)
                        .collect(Collectors.toList());
        return ImportContextResponse.builder().importedVariables(contextVariables).build();
    }

    /**
     * Import cookies from ram.
     * @param importRequest import request
     * @return list of imported cookies
     */
    public List<Cookie> importCookies(ImportFromRamRequest importRequest) {
        if (isNull(importRequest) || (isNull(importRequest.getExecutionRequestId())
                && isNull(importRequest.getTestRunId()) && isNull(importRequest.getLogRecordId()))) {
            log.error("Invalid import cookie request. All field are null");
            throw new ItfLiteIncorrectImportRequest("At least one of the fields must be specified");
        }

        if (nonNull(importRequest.getTestRunId())) {
            UUID testRunId = importRequest.getTestRunId();
            UUID logRecordId = importRequest.getLogRecordId();
            return parseCookieFromLogRecords(testRunId, logRecordId);
        } else if (nonNull(importRequest.getExecutionRequestId())) {
            UUID erId = importRequest.getExecutionRequestId();
            List<UUID> testRunIds = ramExecutionRequestFeignClient.getAllTestRunIds(erId).getBody();
            if (CollectionUtils.isEmpty(testRunIds)) {
                log.error("The execution request should contain test run, but it is not found");
                throw new ItfLiteTestRunsNotFoundException(erId);
            }
            if (testRunIds.size() > 1) {
                log.error("Execution Request must contain only one test run to import cookies");
                throw new ItfLiteIllegalTestRunsCountInExecutionRequestException(erId);
            }
            return parseCookieFromLogRecords(testRunIds.get(0), null);
        }
        return null;
    }

    /**
     * Method parses all cookies from the first logRecord to the logRecord with the specified id,
     * if id is specified, otherwise to the last logRecord.
     *
     * @param testRunId testRun id in which need to parse cooke headers
     * @param logRecordId logRecord id
     * @return list of cookies imported from log records
     */
    private List<Cookie> parseCookieFromLogRecords(UUID testRunId, @Nullable UUID logRecordId) {
        List<Cookie> importedCookies = new ArrayList<>();
        LogRecordFilteringRequestDto filter = new LogRecordFilteringRequestDto()
                .addTypesItem(TypeActionDto.TRANSPORT.getValue());

        // get transport log records for test run
        List<LogRecordDto> logRecords = ramTestRunsFeignClient.getAllFilteredLogRecords(testRunId, filter).getBody();
        if (!CollectionUtils.isEmpty(logRecords)) {
            for (LogRecordDto logRecord : logRecords) {
                if (logRecord instanceof RestLogRecordDto) {
                    RestLogRecordDto restLogRecord = ((RestLogRecordDto) logRecord);
                    importedCookies.addAll(parseRestLogRecordCookie(restLogRecord));
                }

                if (logRecord.getUuid().equals(logRecordId)) {
                    break;
                }
            }
        }
        return importedCookies;
    }

    private List<Cookie> parseRestLogRecordCookie(RestLogRecordDto logRecord) {
        List<Cookie> importedCookies = new ArrayList<>();
        RequestDto request = logRecord.getRequest();
        String domain = "";
        if (StringUtils.isNotEmpty(request.getEndpoint())) {
            domain = UrlParsingUtils.getDomain(request.getEndpoint());
        }
        if (!CollectionUtils.isEmpty(request.getHeadersList())) {
            for (RequestHeaderDto header : request.getHeadersList()) {
                String cookieHeaderValue = header.getValue();
                if (Constants.COOKIE_HEADER_KEY.equals(header.getName()) && StringUtils.isNotEmpty(cookieHeaderValue)) {
                    importedCookies.addAll(CookieUtils.parseCookieHeader(domain, cookieHeaderValue));
                }
            }
        }
        ResponseDto response = logRecord.getResponse();
        if (nonNull(response) && !CollectionUtils.isEmpty(response.getHeadersList())) {
            for (RequestHeaderDto header : response.getHeadersList()) {
                String cookieHeaderValue = header.getValue();
                if (Constants.COOKIE_RESP_HEADER_KEY.equals(header.getName())
                        && StringUtils.isNotEmpty(cookieHeaderValue)) {
                    importedCookies.addAll(CookieUtils.parseCookieHeader(domain, cookieHeaderValue));
                }
            }
        }
        return importedCookies;
    }

    /**
     * Updates context variables and closes current section.
     * @param oldContext context before request execution
     * @param newContext context after request execution
     */
    public void closeCurrentSection(Map<String, Object> oldContext, Map<String, Object> newContext) {
        AtpRamWriter writer = AtpRamWriter.getAtpRamWriter();
        List<ContextVariable> contextVariables = getContextVariables(oldContext, newContext);
        writer.getAdapter().updateContextVariables(writer.getContext().getCurrentSectionId(), contextVariables);
        writer.closeSection();
    }

    /**
     * Opens new section for request execution.
     * @param requestName request name
     */
    public void openNewExecuteRequestSection(String requestName, Long createdDateStamp) {
        Message msg = new Message();
        msg.setType(TypeAction.ITF.name());
        msg.setName(String.format("Execute request \"%s\"", requestName));
        msg.setExecutionStatus(ExecutionStatuses.IN_PROGRESS.name());
        msg.setCreatedDateStamp(createdDateStamp);
        AtpRamWriter writer = AtpRamWriter.getAtpRamWriter();
        writer.writeParentSections(writer.getContext().getAtpCompaund().getParentSection());
        writer.getAdapter().openItfSection(msg, null);
    }

    /**
     * Writes message into current open section with provided message text and testing status.
     * @param message message text
     * @param status testing status
     */
    public void writeMessage(String message, TestingStatuses status) {
        AtpRamWriter.getAtpRamWriter().updateMessageTestingStatusAndFiles(message, status, null);
    }

    /**
     * Update execution status for action.
     * @param requestExecuteRequest execute request action request
     * @param status execution status
     */
    public void updateExecutionStatus(ExecutionCollectionRequestExecuteRequest requestExecuteRequest,
                                      ExecutionStatuses status) {
        Timestamp startDate = Objects.nonNull(requestExecuteRequest.getSection().getStartDate())
                ? requestExecuteRequest.getSection().getStartDate() : new Timestamp(new Date().getTime());
        AtpRamWriter.getAtpRamWriter().getAdapter().updateExecutionStatus(
                requestExecuteRequest.getSection().getSectionId(), status.name(), startDate,
                new Date().getTime() - startDate.getTime());
    }

    public void updateTestingStatus(TestingStatuses testStatus) {
        AtpRamWriter writer = AtpRamWriter.getAtpRamWriter();
        writer.getAdapter().updateTestingStatus(writer.getContext().getCurrentSectionId(), testStatus.name());
    }

}
