package org.qubership.atp.itf.lite.backend.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import javax.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.ActionRepository;
import org.qubership.atp.itf.lite.backend.feign.service.RamService;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.api.request.ExecutionCollectionRequestExecuteRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.GroupResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.collections.ExecuteStepResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;

import org.qubership.atp.itf.lite.backend.catalog.models.ActionEntity;
import org.qubership.atp.itf.lite.backend.catalog.models.ActionParameter;
import org.qubership.atp.itf.lite.backend.catalog.models.ComplexActionParameter;
import org.qubership.atp.ram.enums.TestingStatuses;

@ExtendWith(MockitoExtension.class)
public class ActionServiceTest {

    @Mock
    private ActionRepository repository;
    @Mock
    private FolderService folderService;
    @Mock
    private RequestService requestService;
    @Mock
    private RamService ramService;
    @InjectMocks
    private ActionService actionService;

    @Test
    public void executeAction_executeRequestActionSpecified_RequestIdSpecifiedAndExists() {
        // given
        ActionEntity requestExecuteAction = new ActionEntity();
        UUID requestId = UUID.randomUUID();
        requestExecuteAction.setName("Execute request \"" + requestId + "\"");
        requestExecuteAction.setParameters(Collections.singletonList(
                new ActionParameter("uuid", requestId.toString(), "", null)));

        ExecutionCollectionRequestExecuteRequest executionRequest = new ExecutionCollectionRequestExecuteRequest();
        UUID projectId = UUID.randomUUID();
        executionRequest.setActionEntity(requestExecuteAction);
        executionRequest.setProjectId(projectId);

        Request request = EntitiesGenerator.generateHttpRequest("test", projectId);

        // when
        when(requestService.getRequestByProjectIdAndRequestId(projectId, requestId)) .thenReturn(request);
        actionService.executeAction(executionRequest, null);

        // then
        verify(ramService).provideInfo(executionRequest);
        verify(ramService).updateExecutionLogRecordName(executionRequest, "Execute request \"test\"");
        verify(requestService).executeRequestWithRamAdapterLogging(executionRequest, request, null);
    }

    @Test
    public void executeAction_actionNameContainsVariable_variableMustBeResolved() {
        // given
        ActionEntity requestExecuteAction = new ActionEntity();
        UUID requestId = UUID.randomUUID();
        requestExecuteAction.setName("Execute request \"${requestId}\"");
        requestExecuteAction.setParameters(Collections.singletonList(
                new ActionParameter("uuid", "${requestId}", "", null)));

        ExecutionCollectionRequestExecuteRequest executionRequest = new ExecutionCollectionRequestExecuteRequest();
        UUID projectId = UUID.randomUUID();
        executionRequest.setActionEntity(requestExecuteAction);
        executionRequest.setProjectId(projectId);
        executionRequest.setContext(new HashMap<String, Object>(){{ put("requestId", requestId);}});

        Request request = EntitiesGenerator.generateHttpRequest("test", projectId);

        // when
        when(requestService.getRequestByProjectIdAndRequestId(projectId, requestId)) .thenReturn(request);
        actionService.executeAction(executionRequest, null);

        // then
        verify(ramService).provideInfo(executionRequest);
        verify(ramService).updateExecutionLogRecordName(executionRequest, "Execute request \"test\"");
        verify(requestService).executeRequestWithRamAdapterLogging(executionRequest, request, null);
    }

    @Test
    public void executeAction_executeRequestActionSpecified_RequestIdNotSpecified_returnTestingStatusFailed() {
        // given
        ActionEntity requestExecuteAction = new ActionEntity();
        UUID requestId = UUID.randomUUID();
        requestExecuteAction.setName("Execute request \"" + requestId + "\"");

        ExecutionCollectionRequestExecuteRequest executionRequest = new ExecutionCollectionRequestExecuteRequest();
        UUID projectId = UUID.randomUUID();
        executionRequest.setActionEntity(requestExecuteAction);
        executionRequest.setProjectId(projectId);

        // when
        ExecuteStepResponse response = actionService.executeAction(executionRequest, null);

        // then
        verify(ramService).updateMessageAndTestingStatus("Action parameters are null or empty", TestingStatuses.FAILED);
        Assertions.assertEquals(TestingStatuses.FAILED, response.getTestingStatus());
    }

    @Test
    public void executeAction_executeRequestActionSpecified_RequestIdNotExists_returnTestingStatusFailed() {
        // given
        ActionEntity requestExecuteAction = new ActionEntity();
        UUID requestId = UUID.randomUUID();
        requestExecuteAction.setName("Execute request \"" + requestId + "\"");
        requestExecuteAction.setParameters(Collections.singletonList(
                new ActionParameter("uuid", requestId.toString(), "", null)));

        ExecutionCollectionRequestExecuteRequest executionRequest = new ExecutionCollectionRequestExecuteRequest();
        UUID projectId = UUID.randomUUID();
        executionRequest.setActionEntity(requestExecuteAction);
        executionRequest.setProjectId(projectId);

        // when
        when(requestService.getRequestByProjectIdAndRequestId(projectId, requestId))
                .thenThrow(new EntityNotFoundException("request not found"));
        ExecuteStepResponse response = actionService.executeAction(executionRequest, null);

        // then
        verify(ramService).updateMessageAndTestingStatus("request not found", TestingStatuses.FAILED);
        Assertions.assertEquals(TestingStatuses.FAILED, response.getTestingStatus());
    }

    @Test
    public void executeAction_executeFolderByIdActionSpecified_FolderIdSpecifiedAndExists() {
        // given
        ActionEntity folderExecuteAction = new ActionEntity();
        UUID projectId = UUID.randomUUID();
        Folder rootFolder = EntitiesGenerator.generateFolder("folder_1", projectId, null);
        folderExecuteAction.setName("Execute requests folder \"" + rootFolder.getId() + "\"");
        folderExecuteAction.setParameters(Collections.singletonList(
                new ActionParameter("uuid", rootFolder.getId().toString(), "", null)));

        ExecutionCollectionRequestExecuteRequest executionRequest = EntitiesGenerator.generateRequestExecuteRequest();
        executionRequest.setActionEntity(folderExecuteAction);
        executionRequest.setProjectId(projectId);


        Folder childFolder = EntitiesGenerator.generateFolder("folder_2", projectId, rootFolder.getId());
        Request rootRequest = EntitiesGenerator.generateHttpRequest("request_1", projectId);
        Request childRequest = EntitiesGenerator.generateHttpRequest("request_2", projectId);
        GroupResponse root = new GroupResponse(rootFolder, null);
        GroupResponse child = new GroupResponse(childFolder, null);
        child.setChildren(Collections.singletonList(new GroupResponse(rootRequest, null)));
        root.setChildren(Arrays.asList(child, new GroupResponse(childRequest, null)));

        // when
        ExecuteStepResponse executeStepResponse = new ExecuteStepResponse();
        executeStepResponse.setTestingStatus(TestingStatuses.PASSED);
        when(ramService.provideInfo(executionRequest)).thenCallRealMethod();
        when(folderService.getRequestTreeByParentFolderId(rootFolder.getId())).thenReturn(root);
        when(requestService.getRequestByProjectIdAndRequestId(projectId, rootRequest.getId())).thenReturn(rootRequest);
        when(requestService.getRequestByProjectIdAndRequestId(projectId, childRequest.getId())).thenReturn(childRequest);
        when(requestService.executeRequestWithRamAdapterLogging(executionRequest, rootRequest, null)).thenReturn(executeStepResponse);
        when(requestService.executeRequestWithRamAdapterLogging(executionRequest, childRequest, null)).thenReturn(executeStepResponse);
        ExecuteStepResponse response = actionService.executeAction(executionRequest, null);

        // then
        verify(ramService, times(1))
                .updateExecutionLogRecordName(executionRequest, "Execute requests folder \"folder_1\"");
        verify(requestService, times(1))
                .executeRequestWithRamAdapterLogging(executionRequest, rootRequest, null);
        verify(requestService, times(1))
                .executeRequestWithRamAdapterLogging(executionRequest, childRequest, null);

        Assertions.assertEquals(TestingStatuses.PASSED, response.getTestingStatus());
    }

    @Test
    public void executeAction_executeFolderByIdActionSpecified_ChildSkippedButParentPassed() {
        // given
        ActionEntity folderExecuteAction = new ActionEntity();
        UUID projectId = UUID.randomUUID();
        Folder rootFolder = EntitiesGenerator.generateFolder("folder_1", projectId, null);
        folderExecuteAction.setName("Execute requests folder \"" + rootFolder.getId() + "\"");
        folderExecuteAction.setParameters(Collections.singletonList(
                new ActionParameter("uuid", rootFolder.getId().toString(), "", null)));

        ExecutionCollectionRequestExecuteRequest executionRequest = EntitiesGenerator.generateRequestExecuteRequest();
        executionRequest.setActionEntity(folderExecuteAction);
        executionRequest.setProjectId(projectId);


        Folder childFolder = EntitiesGenerator.generateFolder("folder_2", projectId, rootFolder.getId());
        Request rootRequest = EntitiesGenerator.generateHttpRequest("request_1", projectId);
        Request childRequest = EntitiesGenerator.generateHttpRequest("request_2", projectId);
        GroupResponse root = new GroupResponse(rootFolder, null);
        GroupResponse child = new GroupResponse(childFolder, null);
        child.setChildren(Collections.singletonList(new GroupResponse(rootRequest, null)));
        root.setChildren(Arrays.asList(child, new GroupResponse(childRequest, null)));

        // when
        ExecuteStepResponse executeStepResponseRoot = new ExecuteStepResponse();
        executeStepResponseRoot.setTestingStatus(TestingStatuses.PASSED);
        ExecuteStepResponse executeStepResponseChild = new ExecuteStepResponse();
        executeStepResponseChild.setTestingStatus(TestingStatuses.SKIPPED);
        when(ramService.provideInfo(executionRequest)).thenCallRealMethod();
        when(folderService.getRequestTreeByParentFolderId(rootFolder.getId())).thenReturn(root);
        when(requestService.getRequestByProjectIdAndRequestId(projectId, rootRequest.getId())).thenReturn(rootRequest);
        when(requestService.getRequestByProjectIdAndRequestId(projectId, childRequest.getId())).thenReturn(childRequest);
        when(requestService.executeRequestWithRamAdapterLogging(executionRequest, rootRequest, null)).thenReturn(executeStepResponseRoot);
        when(requestService.executeRequestWithRamAdapterLogging(executionRequest, childRequest, null)).thenReturn(executeStepResponseChild);
        ExecuteStepResponse response = actionService.executeAction(executionRequest, null);

        // then
        verify(ramService, times(1))
                .updateExecutionLogRecordName(executionRequest, "Execute requests folder \"folder_1\"");
        verify(requestService, times(1))
                .executeRequestWithRamAdapterLogging(executionRequest, rootRequest, null);
        verify(requestService, times(1))
                .executeRequestWithRamAdapterLogging(executionRequest, childRequest, null);

        Assertions.assertEquals(TestingStatuses.PASSED, response.getTestingStatus());
    }

    @Test
    public void executeAction_executeFolderByIdActionSpecified_ChildrenSkippedAndFailedButParentFailed() {
        // given
        ActionEntity folderExecuteAction = new ActionEntity();
        UUID projectId = UUID.randomUUID();
        Folder rootFolder = EntitiesGenerator.generateFolder("folder_1", projectId, null);
        folderExecuteAction.setName("Execute requests folder \"" + rootFolder.getId() + "\"");
        folderExecuteAction.setParameters(Collections.singletonList(
                new ActionParameter("uuid", rootFolder.getId().toString(), "", null)));

        ExecutionCollectionRequestExecuteRequest executionRequest = EntitiesGenerator.generateRequestExecuteRequest();
        executionRequest.setActionEntity(folderExecuteAction);
        executionRequest.setProjectId(projectId);


        Folder childFolder = EntitiesGenerator.generateFolder("folder_2", projectId, rootFolder.getId());
        Request rootRequest = EntitiesGenerator.generateHttpRequest("request_1", projectId);
        Request childRequest1 = EntitiesGenerator.generateHttpRequest("request_2", projectId);
        Request childRequest2 = EntitiesGenerator.generateHttpRequest("request_3", projectId);
        GroupResponse root = new GroupResponse(rootFolder, null);
        GroupResponse child1 = new GroupResponse(childFolder, null);
        GroupResponse child2 = new GroupResponse(childFolder, null);
        child1.setChildren(Collections.singletonList(new GroupResponse(rootRequest, null)));
        child2.setChildren(Collections.singletonList(new GroupResponse(rootRequest, null)));
        root.setChildren(Arrays.asList(child1, new GroupResponse(childRequest1, null), child2, new GroupResponse(childRequest2, null)));

        // when
        ExecuteStepResponse executeStepResponseRoot = new ExecuteStepResponse();
        executeStepResponseRoot.setTestingStatus(TestingStatuses.PASSED);
        ExecuteStepResponse executeStepResponseChild1 = new ExecuteStepResponse();
        executeStepResponseChild1.setTestingStatus(TestingStatuses.FAILED);
        ExecuteStepResponse executeStepResponseChild2 = new ExecuteStepResponse();
        executeStepResponseChild2.setTestingStatus(TestingStatuses.SKIPPED);
        when(ramService.provideInfo(executionRequest)).thenCallRealMethod();
        when(folderService.getRequestTreeByParentFolderId(rootFolder.getId())).thenReturn(root);
        when(requestService.getRequestByProjectIdAndRequestId(projectId, rootRequest.getId())).thenReturn(rootRequest);
        when(requestService.getRequestByProjectIdAndRequestId(projectId, childRequest1.getId())).thenReturn(childRequest1);
        when(requestService.getRequestByProjectIdAndRequestId(projectId, childRequest2.getId())).thenReturn(childRequest2);
        when(requestService.executeRequestWithRamAdapterLogging(executionRequest, rootRequest, null)).thenReturn(executeStepResponseRoot);
        when(requestService.executeRequestWithRamAdapterLogging(executionRequest, childRequest1, null)).thenReturn(executeStepResponseChild1);
        when(requestService.executeRequestWithRamAdapterLogging(executionRequest, childRequest2, null)).thenReturn(executeStepResponseChild2);
        ExecuteStepResponse response = actionService.executeAction(executionRequest, null);

        // then
        verify(ramService, times(1))
                .updateExecutionLogRecordName(executionRequest, "Execute requests folder \"folder_1\"");
        verify(requestService, times(2))
                .executeRequestWithRamAdapterLogging(executionRequest, rootRequest, null);
        verify(requestService, times(1))
                .executeRequestWithRamAdapterLogging(executionRequest, childRequest1, null);
        verify(requestService, times(1))
                .executeRequestWithRamAdapterLogging(executionRequest, childRequest2, null);
        Assertions.assertEquals(TestingStatuses.FAILED, response.getTestingStatus());
    }

    @Test
    public void executeAction_executeFolderByIdActionSpecified_FolderIdSpecifiedAndExists_CorrectTestingStatus() {
        // given
        ActionEntity folderExecuteAction = new ActionEntity();
        UUID projectId = UUID.randomUUID();
        Folder rootFolder = EntitiesGenerator.generateFolder("folder_1", projectId, null);
        folderExecuteAction.setName("Execute requests folder \"" + rootFolder.getId() + "\"");
        folderExecuteAction.setParameters(Collections.singletonList(
                new ActionParameter("uuid", rootFolder.getId().toString(), "", null)));

        ExecutionCollectionRequestExecuteRequest executionRequest = EntitiesGenerator.generateRequestExecuteRequest();
        executionRequest.setActionEntity(folderExecuteAction);
        executionRequest.setProjectId(projectId);


        Folder childFolder = EntitiesGenerator.generateFolder("folder_2", projectId, rootFolder.getId());
        Request rootRequest = EntitiesGenerator.generateHttpRequest("request_1", projectId);
        Request childRequest = EntitiesGenerator.generateHttpRequest("request_2", projectId);
        GroupResponse root = new GroupResponse(rootFolder, null);
        GroupResponse child = new GroupResponse(childFolder, null);
        child.setChildren(Collections.singletonList(new GroupResponse(rootRequest, null)));
        root.setChildren(Arrays.asList(child, new GroupResponse(childRequest, null)));

        // when
        when(ramService.provideInfo(executionRequest)).thenCallRealMethod();
        when(folderService.getRequestTreeByParentFolderId(rootFolder.getId())).thenReturn(root);
        when(requestService.getRequestByProjectIdAndRequestId(projectId, rootRequest.getId())).thenReturn(rootRequest);
        when(requestService.getRequestByProjectIdAndRequestId(projectId, childRequest.getId())).thenReturn(childRequest);
        ExecuteStepResponse executeStepResponseRoot = new ExecuteStepResponse();
        executeStepResponseRoot.setTestingStatus(TestingStatuses.PASSED);
        when(requestService.executeRequestWithRamAdapterLogging(executionRequest, rootRequest, null)).thenReturn(executeStepResponseRoot);
        ExecuteStepResponse executeStepResponseChild = new ExecuteStepResponse();
        executeStepResponseChild.setTestingStatus(TestingStatuses.FAILED);
        when(requestService.executeRequestWithRamAdapterLogging(executionRequest, childRequest, null)).thenReturn(executeStepResponseChild);
        ExecuteStepResponse response = actionService.executeAction(executionRequest, null);

        // then
        verify(ramService, times(1))
                .updateExecutionLogRecordName(executionRequest, "Execute requests folder \"folder_1\"");
        verify(requestService, times(1))
                .executeRequestWithRamAdapterLogging(executionRequest, rootRequest, null);
        verify(requestService, times(1))
                .executeRequestWithRamAdapterLogging(executionRequest, childRequest, null);

        Assertions.assertEquals(TestingStatuses.FAILED, response.getTestingStatus());
    }

    @Test
    public void executeAction_executeFolderByIdActionSpecified_FolderIdNotSpecified_returnTestingStatusFailed() {
        // given
        ActionEntity folderExecuteAction = new ActionEntity();
        UUID projectId = UUID.randomUUID();
        Folder rootFolder = EntitiesGenerator.generateFolder("folder_1", projectId, null);
        folderExecuteAction.setName("Execute requests folder \"" + rootFolder.getId() + "\"");

        ExecutionCollectionRequestExecuteRequest executionRequest = EntitiesGenerator.generateRequestExecuteRequest();
        executionRequest.setActionEntity(folderExecuteAction);
        executionRequest.setProjectId(projectId);

        ExecuteStepResponse response = actionService.executeAction(executionRequest, null);

        verify(ramService).updateMessageAndTestingStatus("Action parameters are null or empty", TestingStatuses.FAILED);
        Assertions.assertEquals(TestingStatuses.FAILED, response.getTestingStatus());
    }

    @Test
    public void executeAction_executeFolderByIdActionSpecified_FolderNotExists_returnTestingStatusFailed() {
        // given
        ActionEntity folderExecuteAction = new ActionEntity();
        UUID projectId = UUID.randomUUID();
        Folder rootFolder = EntitiesGenerator.generateFolder("folder_1", projectId, null);
        folderExecuteAction.setName("Execute requests folder \"" + rootFolder.getId() + "\"");
        folderExecuteAction.setParameters(Collections.singletonList(
                new ActionParameter("uuid", rootFolder.getId().toString(), "", null)));

        ExecutionCollectionRequestExecuteRequest executionRequest = EntitiesGenerator.generateRequestExecuteRequest();
        executionRequest.setActionEntity(folderExecuteAction);
        executionRequest.setProjectId(projectId);

        // when
        when(folderService.getRequestTreeByParentFolderId(rootFolder.getId()))
                .thenThrow(new AtpEntityNotFoundException("Folder", rootFolder.getId()));
        ExecuteStepResponse response = actionService.executeAction(executionRequest, null);

        // then
        verify(ramService).updateMessageAndTestingStatus(
                "Failed to find Folder with id: " + rootFolder.getId(),
                TestingStatuses.FAILED);
        Assertions.assertEquals(TestingStatuses.FAILED, response.getTestingStatus());
    }

    @Test
    public void executeAction_executeFolderByPathActionSpecified_FolderIdSpecifiedAndExists() {
        // given
        ActionEntity folderExecuteAction = new ActionEntity();
        UUID projectId = UUID.randomUUID();
        Folder rootFolder = EntitiesGenerator.generateFolder("folder_1", projectId, null);
        folderExecuteAction.setName("Execute requests folder by path (\"folder_1\")");
        folderExecuteAction.setParameters(Collections.singletonList(
                new ActionParameter("uuid", null, "",
                        new ComplexActionParameter(
                                ComplexActionParameter.Type.ARRAY,
                                Collections.singletonList(new ActionParameter("", "folder_1", null, null)),
                                null))));

        ExecutionCollectionRequestExecuteRequest executionRequest = EntitiesGenerator.generateRequestExecuteRequest();
        executionRequest.setActionEntity(folderExecuteAction);
        executionRequest.setProjectId(projectId);


        Folder childFolder = EntitiesGenerator.generateFolder("folder_2", projectId, rootFolder.getId());
        Request rootRequest = EntitiesGenerator.generateHttpRequest("request_1", projectId);
        Request childRequest = EntitiesGenerator.generateHttpRequest("request_2", projectId);
        GroupResponse root = new GroupResponse(rootFolder, null);
        GroupResponse child = new GroupResponse(childFolder, null);
        child.setChildren(Collections.singletonList(new GroupResponse(rootRequest, null)));
        root.setChildren(Arrays.asList(child, new GroupResponse(childRequest, null)));

        // when
        ExecuteStepResponse executeStepResponse = new ExecuteStepResponse();
        executeStepResponse.setTestingStatus(TestingStatuses.PASSED);when(ramService.provideInfo(executionRequest)).thenCallRealMethod();
        when(folderService.getIdByFoldersPath(projectId, Collections.singletonList("folder_1"))).thenReturn(rootFolder.getId());
        when(folderService.getRequestTreeByParentFolderId(rootFolder.getId())).thenReturn(root);
        when(requestService.getRequestByProjectIdAndRequestId(projectId, rootRequest.getId())).thenReturn(rootRequest);
        when(requestService.getRequestByProjectIdAndRequestId(projectId, childRequest.getId())).thenReturn(childRequest);
        when(requestService.executeRequestWithRamAdapterLogging(executionRequest, rootRequest, null)).thenReturn(executeStepResponse);
        when(requestService.executeRequestWithRamAdapterLogging(executionRequest, childRequest, null)).thenReturn(executeStepResponse);
        ExecuteStepResponse response = actionService.executeAction(executionRequest, null);

        // then
        verify(requestService, times(1))
                .executeRequestWithRamAdapterLogging(executionRequest, rootRequest, null);
        verify(requestService, times(1))
                .executeRequestWithRamAdapterLogging(executionRequest, childRequest, null);

        Assertions.assertEquals(TestingStatuses.PASSED, response.getTestingStatus());
    }

    @Test
    public void executeAction_executeFolderByPathActionSpecified_FolderPathNotSpecified_returnTestingStatusFailed() {
        // given
        ActionEntity folderExecuteAction = new ActionEntity();
        UUID projectId = UUID.randomUUID();
        folderExecuteAction.setName("Execute requests folder by path (\"folder_1\")");

        ExecutionCollectionRequestExecuteRequest executionRequest = EntitiesGenerator.generateRequestExecuteRequest();
        executionRequest.setActionEntity(folderExecuteAction);
        executionRequest.setProjectId(projectId);

        ExecuteStepResponse response = actionService.executeAction(executionRequest, null);

        verify(ramService).updateMessageAndTestingStatus("Action parameters are null or empty", TestingStatuses.FAILED);
        Assertions.assertEquals(TestingStatuses.FAILED, response.getTestingStatus());
    }

    @Test
    public void executeAction_executeFolderByPathActionSpecified_FolderNotExists_returnTestingStatusFailed() {
        // given
        ActionEntity folderExecuteAction = new ActionEntity();
        UUID projectId = UUID.randomUUID();
        folderExecuteAction.setName("Execute requests folder by path (\"folder_1\")");
        folderExecuteAction.setParameters(Collections.singletonList(
                new ActionParameter("uuid", null, "",
                        new ComplexActionParameter(
                                ComplexActionParameter.Type.ARRAY,
                                Collections.singletonList(new ActionParameter("", "folder_1", null, null)),
                                null))));

        ExecutionCollectionRequestExecuteRequest executionRequest = EntitiesGenerator.generateRequestExecuteRequest();
        executionRequest.setActionEntity(folderExecuteAction);
        executionRequest.setProjectId(projectId);

        // when
        when(folderService.getIdByFoldersPath(projectId, Collections.singletonList("folder_1")))
                .thenThrow(new AtpEntityNotFoundException("Folder", "path", Collections.singletonList("folder_1")));
        ExecuteStepResponse response = actionService.executeAction(executionRequest, null);

        // then
        verify(ramService).updateMessageAndTestingStatus(
                "Failed to find Folder by path: [folder_1]",
                TestingStatuses.FAILED);
        Assertions.assertEquals(TestingStatuses.FAILED, response.getTestingStatus());
    }

}
