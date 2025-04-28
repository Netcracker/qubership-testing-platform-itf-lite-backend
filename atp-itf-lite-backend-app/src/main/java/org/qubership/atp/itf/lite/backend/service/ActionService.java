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

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.qubership.atp.adapter.common.entities.Message;
import org.qubership.atp.adapter.executor.executor.AtpRamWriter;
import org.qubership.atp.itf.lite.backend.catalog.models.ActionEntity;
import org.qubership.atp.itf.lite.backend.catalog.models.ActionParameter;
import org.qubership.atp.itf.lite.backend.catalog.models.ComplexActionParameter;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.ActionRepository;
import org.qubership.atp.itf.lite.backend.enums.ActionName;
import org.qubership.atp.itf.lite.backend.enums.EntityType;
import org.qubership.atp.itf.lite.backend.exceptions.ItfLiteException;
import org.qubership.atp.itf.lite.backend.exceptions.action.ItfLiteActionNotFoundException;
import org.qubership.atp.itf.lite.backend.feign.service.RamService;
import org.qubership.atp.itf.lite.backend.model.api.request.ExecutionCollectionRequestExecuteRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.GroupResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.collections.ContextEntity;
import org.qubership.atp.itf.lite.backend.model.api.response.collections.ExecuteStepResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Action;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActionService {

    private final ActionRepository repository;
    private final FolderService folderService;
    private final RequestService requestService;
    private final RamService ramService;
    private final ObjectMapper objectMapper;

    private static final String KEY = "key";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(?<" + KEY + ">[^}]+)\\}");

    public List<Action> getActions(UUID projectId) {
        return repository.findAll();
    }

    /**
     * Execute action by request and environment id.
     * @environmentId need for desire action strategy.
     */
    public ExecuteStepResponse executeAction(ExecutionCollectionRequestExecuteRequest request,
                                             UUID environmentId) {
        ramService.provideInfo(request);
        ExecuteStepResponse response = new ExecuteStepResponse();
        Function<ExecutionCollectionRequestExecuteRequest, ExecuteStepResponse> actionStrategy = null;
        try {
            compileActionName(request.getActionEntity(), request.getContext());
            actionStrategy = getStrategyByAction(request.getActionEntity(), request.getProjectId(), environmentId,
                    request.getContext());
        } catch (Exception ex) {
            log.error("Failed to get action strategy by action {}", request.getActionEntity().getName(), ex);
            ramService.updateMessageAndTestingStatus(ex.getMessage(), TestingStatuses.FAILED);
            response.setTestingStatus(TestingStatuses.FAILED);
        }
        try {
            if (nonNull(actionStrategy)) {
                response = actionStrategy.apply(request);
            }
        } catch (Exception ex) {
            log.error("Failed to execute action {}", request.getActionEntity().getName(), ex);
            ramService.updateMessageAndTestingStatus(ex.getMessage(), TestingStatuses.FAILED);
            response.setTestingStatus(TestingStatuses.FAILED);
        }
        return response;
    }

    private Function<ExecutionCollectionRequestExecuteRequest, ExecuteStepResponse> getStrategyByAction(
            ActionEntity action, UUID projectId, UUID environmentId, Map<String, Object> context) {
        if (CollectionUtils.isEmpty(action.getParameters())) {
            log.error("Action parameters are null or empty");
            throw new ItfLiteException("Action parameters are null or empty");
        }

        String actionName = action.getName();
        if (actionName.matches(ActionName.EXECUTE_REQUEST_BY_ID.getRegexp())) {
            String requestIdStr = resolveActionParameter(action.getParameters().get(0).getValue(), context);
            try {
                UUID requestId = UUID.fromString(requestIdStr);
                return getExecuteRequestActionStrategy(requestId, environmentId);
            } catch (IllegalArgumentException ex) {
                log.error("Failed to parse requestId from action parameter: {}", requestIdStr, ex);
                throw new ItfLiteException(String.format("Failed to get requestId - bad uuid format: %s",
                        requestIdStr));
            }
        } else if (actionName.matches(ActionName.EXECUTE_FOLDER_BY_ID.getRegexp())) {
            String folderIdStr = resolveActionParameter(action.getParameters().get(0).getValue(), context);
            try {
                UUID folderId = UUID.fromString(folderIdStr);
                return getExecuteFolderStrategy(folderId, environmentId);
            } catch (IllegalArgumentException ex) {
                log.error("Failed to parse folderId from action parameter: {}", folderIdStr, ex);
                throw new ItfLiteException(String.format("Failed to get folderId - bad uuid format: %s", folderIdStr));
            }
        } else if (actionName.matches(ActionName.EXECUTE_FOLDER_BY_PATH.getRegexp())) {
            ComplexActionParameter complexParam = action.getParameters().get(0).getComplexParam();
            if (Objects.isNull(complexParam)) {
                log.error("ComplexActionParameters are null");
                throw new ItfLiteException("ComplexActionParameters are null");
            }
            List<ActionParameter> actionParameters = complexParam.getArrayParams();
            if (CollectionUtils.isEmpty(actionParameters)) {
                log.error("Action parameters are null or empty");
                throw new ItfLiteException("Action parameters are null or empty");
            }
            List<String> path = actionParameters.stream().map(ActionParameter::getValue).collect(Collectors.toList());
            resolveActionParameters(path, context);
            UUID folderId = folderService.getIdByFoldersPath(projectId, path);
            return getExecuteFolderStrategy(folderId, environmentId);
        }
        throw new ItfLiteActionNotFoundException(action.getName());
    }

    private Function<ExecutionCollectionRequestExecuteRequest, ExecuteStepResponse> getExecuteRequestActionStrategy(
            UUID requestId, UUID environmentId) {
        return (requestExecuteRequest) -> {
            log.debug("Check if request with requestId {} exists", requestId);
            // EntityNotFoundException will be thrown if not found
            Request request = requestService.getRequestByProjectIdAndRequestId(
                    requestExecuteRequest.getProjectId(), requestId);
            if (requestExecuteRequest.getActionEntity().getName().contains(requestId.toString())) {
                log.debug("Replace requestId = {} by requestName = {} in action entity.", requestId,
                        request.getName());
                ramService.updateExecutionLogRecordName(requestExecuteRequest,
                        requestExecuteRequest.getActionEntity().getName().replace(requestId.toString(),
                                request.getName()));
            }
            return requestService.executeRequestWithRamAdapterLogging(requestExecuteRequest, request, environmentId);
        };
    }

    private Function<ExecutionCollectionRequestExecuteRequest, ExecuteStepResponse> getExecuteFolderStrategy(
            UUID folderId,
            UUID environmentId) {
        return (requestExecuteRequest) -> {
            ExecuteStepResponse response = new ExecuteStepResponse();

            try {
                GroupResponse requestTree = folderService.getRequestTreeByParentFolderId(folderId);
                String actionName = requestExecuteRequest.getActionEntity().getName();
                if (actionName.contains(folderId.toString())) {
                    log.debug("Replace folderId = {} by folderName = {} in action entity.", folderId,
                            requestTree.getName());
                    ramService.updateExecutionLogRecordName(requestExecuteRequest, actionName.replace(
                            folderId.toString(), requestTree.getName()));
                }
                response = executeFolderRequests(requestTree, requestExecuteRequest, environmentId);
                response.setStatus(ExecutionStatuses.FINISHED);
            } catch (Exception ex) {
                log.error("Failed to execute folder action by id {}", folderId, ex);
                ramService.updateMessageAndTestingStatus(ex.getMessage(), TestingStatuses.FAILED);
                response.setTestingStatus(TestingStatuses.FAILED);
            }

            return response;
        };
    }

    private ExecuteStepResponse executeFolderRequests(GroupResponse requestTree,
                                                      ExecutionCollectionRequestExecuteRequest requestExecuteRequest,
                                                      UUID environmentId) {
        ExecuteStepResponse response = new ExecuteStepResponse();
        TestingStatuses finalStatus = TestingStatuses.PASSED;

        List<GroupResponse> children = requestTree.getChildren();
        if (!CollectionUtils.isEmpty(children)) {
            for (GroupResponse child : children) {
                if (EntityType.FOLDER.equals(child.getType())) {
                    if (!CollectionUtils.isEmpty(child.getChildren())) {
                        AtpRamWriter ramWriter = AtpRamWriter.getAtpRamWriter();
                        Message msg = new Message();
                        msg.setName(child.getName());
                        msg.setType(TypeAction.COMPOUND.toString());
                        ramWriter.openSection(msg);
                        try {
                            response = executeFolderRequests(child, requestExecuteRequest, environmentId);
                            finalStatus = TestingStatuses.PASSED.equals(finalStatus)
                                    && (TestingStatuses.PASSED.equals(response.getTestingStatus())
                                    || TestingStatuses.SKIPPED.equals(response.getTestingStatus()))
                                    ? TestingStatuses.PASSED : TestingStatuses.FAILED;
                        } finally {
                            ramWriter.closeSection(null);
                        }
                    }
                } else {
                    ExecuteStepResponse newResponse = executeRequest(child.getId(), requestExecuteRequest,
                            environmentId);
                    response.setContext(newResponse.getContext());
                    finalStatus = TestingStatuses.PASSED.equals(finalStatus)
                            && (TestingStatuses.PASSED.equals(newResponse.getTestingStatus())
                            || TestingStatuses.SKIPPED.equals(newResponse.getTestingStatus()))
                            ? TestingStatuses.PASSED : TestingStatuses.FAILED;
                    response.setContext(newResponse.getContext());
                    response.setMetadata(newResponse.getMetadata());
                }
            }
        }
        response.setTestingStatus(finalStatus);
        return response;
    }

    private ExecuteStepResponse executeRequest(UUID requestId, ExecutionCollectionRequestExecuteRequest parentRer,
                                               UUID environmentId) {
        Request request = requestService.getRequestByProjectIdAndRequestId(
                parentRer.getProjectId(), requestId);

        // open itf section
        Message itfMessage = new Message();
        itfMessage.setName(String.format("%s \"%s\"", ActionName.EXECUTE_REQUEST_BY_ID.getName(), request.getName()));
        itfMessage.setExecutionStatus(ExecutionStatuses.IN_PROGRESS.name());
        itfMessage.setType(TypeAction.ITF.toString());
        AtpRamWriter writer = AtpRamWriter.getAtpRamWriter();
        writer.openSection(itfMessage);

        ExecuteStepResponse response;
        try {
            response = requestService.executeRequestWithRamAdapterLogging(parentRer, request, environmentId);
            Map<String, Object> newContext = getContextAfterExecution(response);
            writer.getAdapter().updateContextVariables(writer.getContext().getLogRecordUuid(),
                    ramService.getContextVariables(parentRer.getContext(), newContext));
            parentRer.setContext(newContext);
        } finally {
            writer.closeSection(null);
        }
        return response;
    }

    private Map<String, Object> getContextAfterExecution(ExecuteStepResponse response) {
        ContextEntity context = response.getContext();
        if (nonNull(context)) {
            String contextStr = context.getJsonString();
            if (!StringUtils.isEmpty(contextStr)) {
                try {
                    return objectMapper.readValue(contextStr, new TypeReference<HashMap<String, Object>>() {});
                } catch (JsonProcessingException ex) {
                    log.error("Failed to convert execution context from string", ex);
                    throw new ItfLiteException("Failed to read execution context");
                }
            }
        }
        return null;
    }

    private void resolveActionParameters(List<String> parameters, Map<String, Object> context) {
       for (ListIterator<String> i = parameters.listIterator(); i.hasNext();) {
           i.set(resolveActionParameter(i.next(), context));
       }
    }

    private String resolveActionParameter(String parameter, Map<String, Object> context) {
        Matcher matcher = VARIABLE_PATTERN.matcher(parameter);
        if (matcher.matches()) {
            String key = matcher.group(KEY);
            Object value = context.get(key);
            if (nonNull(value)) {
                return matcher.replaceFirst(value.toString());
            }
        }
        return parameter;
    }

    /**
     * Replace context variables with value from context or action parameters.
     *
     * @param context Some additional or changed parameters.
     */
    public void compileActionName(ActionEntity action, Map<String, Object> context) {
        if (context != null) {
            for (String key : context.keySet()) {
                Object value = context.get(key);
                if (nonNull(value)) {
                    action.setName(action.getName().replaceAll("\\$\\{" + key + "}", value.toString()));
                }
            }
        }
        for (ActionParameter actionParameter : action.getParameters()) {
            if (nonNull(actionParameter.getName()) && nonNull(actionParameter.getValue())) {
                action.setName(action.getName().replaceAll("\\$\\{" + actionParameter.getName() + "}",
                        actionParameter.getValue()));
            }
        }
    }
}
