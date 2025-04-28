package org.qubership.atp.itf.lite.backend.controllers;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateHttpRequestSaveFromHttpRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestCreateFromRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestEditFromRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestEntitiesCopyRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestEntitiesMoveRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestEntityCopyRequestFromRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestEntityMoveRequestFromRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestExecuteRequest;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.ID_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.ORDER_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.REQUESTS_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SERVICE_API_V1_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SETTINGS_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.itf.lite.backend.enums.ContextVariableType;
import org.qubership.atp.itf.lite.backend.feign.service.RamService;
import org.qubership.atp.itf.lite.backend.handlers.MethodArgumentExceptionHandler;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.api.ApiPath;
import org.qubership.atp.itf.lite.backend.model.api.request.ContextVariable;
import org.qubership.atp.itf.lite.backend.model.api.request.CurlStringImportRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.ExecutionCollectionRequestExecuteRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitiesBulkDelete;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitiesCopyRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitiesMoveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityCopyRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityCreateRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityEditRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityMoveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestOrderChangeRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.Settings;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.ContextResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExportResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.collections.ExecuteStepResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.service.ActionService;
import org.qubership.atp.itf.lite.backend.service.ConcurrentModificationService;
import org.qubership.atp.itf.lite.backend.service.FormDataPartService;
import org.qubership.atp.itf.lite.backend.service.RequestService;
import org.qubership.atp.itf.lite.backend.service.RequestSnapshotService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Sets;

@ContextConfiguration(classes = {RequestController.class, MethodArgumentExceptionHandler.class})
@MockBeans({
        @MockBean(FormDataPartService.class),
        @MockBean(RamService.class)
})
@Isolated
public class RequestControllerTest extends AbstractControllerTest {

    static final String FULL_REQUESTS_PATH = ApiPath.SERVICE_API_V1_PATH + ApiPath.REQUESTS_PATH;

    @MockBean
    private RequestService requestService;
    @MockBean
    private ActionService actionService;
    @MockBean
    private FormDataPartService formDataPartService;
    @MockBean
    private RamService ramService;
    @MockBean
    private RequestSnapshotService requestSnapshotService;

    @MockBean
    private ConcurrentModificationService concurrentModificationService;

    @BeforeAll
    public static void beforeAll() {
        // filled createdWhen and modifiedWhen for request
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    public void getAllRequestsTest() {
        // given
        UUID projectId = randomUUID();
        // when
        when(requestService.getAllRequests(eq(projectId), eq(null)))
                .thenReturn(new ArrayList<>());
        // then
        try {
            this.mockMvc.perform(get(FULL_REQUESTS_PATH + "?projectId=" + projectId))
                    .andDo(print())
                    .andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createRequestTest_correctRequestEntityUpsertRequestSpecified_shouldSuccessfullyCreated() throws Exception {
        // given
        Request request = generateRandomHttpRequest();
        RequestEntityCreateRequest requestEntityCreateRequest = generateRequestCreateFromRequest(request);
        // when
        when(requestService.createRequest(eq(requestEntityCreateRequest)))
                .thenReturn(request);
        // then
        this.mockMvc.perform(post(FULL_REQUESTS_PATH)
                        .content(objectMapper.writeValueAsString(requestEntityCreateRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void createRequestTest_incorrectRequestEntityUpsertRequestSpecified_expected400Error() throws Exception {
        // given, when
        RequestEntityCreateRequest request = new RequestEntityCreateRequest(null, null, null, null);
        // then
        this.mockMvc.perform(post(FULL_REQUESTS_PATH)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException));
    }

    @Test
    public void saveRequestTest_correctRequestEntitySaveRequestSpecified_shouldSuccessfullyEdited() throws Exception {
        // given
        UUID requestId = randomUUID();
        HttpRequest request = generateRandomHttpRequest();
        request.setName("Edited Request");
        HttpRequestEntitySaveRequest requestEntitySaveRequest = generateHttpRequestSaveFromHttpRequest(request);

        // when
        when(requestService.saveRequest(eq(requestId), eq(requestEntitySaveRequest), any(), any()))
                .thenReturn(request);
        when(concurrentModificationService.getConcurrentModificationHttpStatus(eq(requestId), any(Date.class), any()))
                .thenReturn(HttpStatus.OK);
        // then
        // save endpoint receives form data with dictionary and request entity
        MockMultipartHttpServletRequestBuilder builder = multipart(FULL_REQUESTS_PATH + "/" + requestId);
        MockMultipartFile requestEntity = new MockMultipartFile("requestEntity", "requestEntity",
                MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsString(requestEntitySaveRequest).getBytes());
        builder.with(new RequestPostProcessor() {
            @Override
            public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
                request.setMethod("PUT");
                return request;
            }
        });
        this.mockMvc.perform(builder.file(requestEntity))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void saveRequestTest_incorrectRequestEntitySaveRequestSpecified_expected400Error() throws Exception {
        // given
        UUID requestId = randomUUID();
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest(null, null, null, null, null, null, null);
        // when, then
        // save endpoint receives form data with dictionary and request entity
        MockMultipartHttpServletRequestBuilder builder = multipart(FULL_REQUESTS_PATH + "/" + requestId);
        MockMultipartFile requestEntity = new MockMultipartFile("requestEntity", "requestEntity",
                MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsString(request).getBytes());
        builder.with(new RequestPostProcessor() {
            @Override
            public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
                request.setMethod("PUT");
                return request;
            }
        });
        this.mockMvc.perform(builder.file(requestEntity))
                .andDo(print())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException));
    }

    @Test
    public void saveRequestTest_concurrentModification_shouldBeSaved_status409() throws Exception {
        // given
        UUID requestId = randomUUID();
        HttpRequest request = generateRandomHttpRequest();
        request.setName("Edited Request");
        HttpRequestEntitySaveRequest requestEntitySaveRequest = generateHttpRequestSaveFromHttpRequest(request);

        // when
        when(requestService.saveRequest(eq(requestId), eq(requestEntitySaveRequest), any(), any()))
                .thenReturn(request);
        when(concurrentModificationService.getConcurrentModificationHttpStatus(eq(requestId), any(Date.class), any()))
                .thenReturn(HttpStatus.OK);

        // then
        // save endpoint receives form data with dictionary and request entity
        MockMultipartHttpServletRequestBuilder builder = multipart(FULL_REQUESTS_PATH + "/" + requestId);
        MockMultipartFile requestEntity = new MockMultipartFile("requestEntity", "requestEntity",
                MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsString(requestEntitySaveRequest).getBytes());
        builder.with(new RequestPostProcessor() {
            @Override
            public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
                request.setMethod("PUT");
                return request;
            }
        });
        this.mockMvc.perform(builder.file(requestEntity))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void copyRequestTest_correctRequestEntityCopyRequestSpecified_shouldSuccessfullyCopied() throws Exception {
        // given
        Request requestCopy = generateRandomHttpRequest();
        requestCopy.setName("Copied request");
        // copy to requestCopy.getFolderId()
        RequestEntityCopyRequest requestEntityCopyRequest = generateRequestEntityCopyRequestFromRequest(requestCopy);
        // when
        when(requestService.copyRequest(eq(requestCopy.getId()), eq(requestEntityCopyRequest)))
                .thenReturn(requestCopy);
        // then
        this.mockMvc.perform(post(FULL_REQUESTS_PATH + "/" + requestCopy.getId() + ApiPath.COPY_PATH)
                        .content(objectMapper.writeValueAsString(requestEntityCopyRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void copyRequestTest_incorrectRequestEntityCopyRequestSpecified_expected400Error() throws Exception {
        // given
        UUID requestId = randomUUID();
        RequestEntityCopyRequest requestEntityCopyRequest = new RequestEntityCopyRequest(null, null);
        // when, then
        this.mockMvc.perform(post(FULL_REQUESTS_PATH + "/" + requestId + ApiPath.COPY_PATH)
                        .content(objectMapper.writeValueAsString(requestEntityCopyRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException));
    }

    @Test
    public void copyRequestsTest_correctRequestEntitiesCopyRequestSpecified_shouldSuccessfullyCopied() throws Exception {
        // given
        RequestEntitiesCopyRequest requestEntitiesCopyRequest = generateRequestEntitiesCopyRequest();
        // when, then
        this.mockMvc.perform(post(FULL_REQUESTS_PATH + ApiPath.COPY_PATH)
                        .content(objectMapper.writeValueAsString(requestEntitiesCopyRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void copyRequestsTest_incorrectRequestEntitiesCopyRequestSpecified_expected400Error() throws Exception {
        // given
        RequestEntitiesCopyRequest requestEntitiesCopyRequest = generateRequestEntitiesCopyRequest();
        requestEntitiesCopyRequest.setRequestIds(new HashSet<>());
        requestEntitiesCopyRequest.setProjectId(null);
        // when, then
        this.mockMvc.perform(post(FULL_REQUESTS_PATH + ApiPath.COPY_PATH)
                        .content(objectMapper.writeValueAsString(requestEntitiesCopyRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException));
    }

    @Test
    public void moveRequestTest_correctRequestEntityMoveRequestSpecified_shouldSuccessfullyMoved() throws Exception {
        // given
        Request request = generateRandomHttpRequest();
        // copy to requestCopy.getFolderId()
        RequestEntityMoveRequest requestEntityMoveRequest = generateRequestEntityMoveRequestFromRequest(request);

        // when
        when(requestService.moveRequest(eq(request.getId()), eq(requestEntityMoveRequest)))
                .thenReturn(request);
        when(concurrentModificationService.getConcurrentModificationHttpStatus(any(), any()))
                .thenReturn(Pair.of(HttpStatus.OK, new ArrayList<>()));

        // then
        this.mockMvc.perform(post(FULL_REQUESTS_PATH + "/" + request.getId() + ApiPath.MOVE_PATH)
                        .content(objectMapper.writeValueAsString(requestEntityMoveRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void moveRequestTest_incorrectRequestEntityMoveRequestSpecified_expected400Error() throws Exception {
        // given, when
        UUID requestId = randomUUID();
        RequestEntityMoveRequest requestEntityUpsertRequest1 = new RequestEntityMoveRequest(null, null);
        // then
        this.mockMvc.perform(post(FULL_REQUESTS_PATH + "/" + requestId + ApiPath.MOVE_PATH)
                        .content(objectMapper.writeValueAsString(requestEntityUpsertRequest1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException));
    }

    @Test
    public void moveRequestsTest_correctRequestEntitiesMoveRequestSpecified_shouldSuccessfullyMoved() throws Exception {
        // given
        RequestEntitiesMoveRequest requestEntitiesMoveRequest = generateRequestEntitiesMoveRequest();

        // when
        when(concurrentModificationService.getConcurrentModificationHttpStatus(any(), any()))
                .thenReturn(Pair.of(HttpStatus.OK, new ArrayList<>()));

        // then
        this.mockMvc.perform(post(FULL_REQUESTS_PATH + ApiPath.MOVE_PATH)
                        .content(objectMapper.writeValueAsString(requestEntitiesMoveRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void moveRequestsTest_incorrectRequestEntitiesMoveRequestSpecified_expected400Error() throws Exception {
        // given
        RequestEntitiesMoveRequest requestEntitiesMoveRequest = generateRequestEntitiesMoveRequest();
        requestEntitiesMoveRequest.setRequestIds(new HashSet<>());
        requestEntitiesMoveRequest.setProjectId(null);
        // when, then
        this.mockMvc.perform(post(FULL_REQUESTS_PATH + ApiPath.MOVE_PATH)
                        .content(objectMapper.writeValueAsString(requestEntitiesMoveRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException));
    }

    @Test
    public void deleteRequestTest_shouldSuccessfullyDeleted() throws Exception {
        // given
        UUID requestId = randomUUID();
        // when, then
        this.mockMvc.perform(delete(FULL_REQUESTS_PATH + "/" + requestId))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void deleteRequestsTest_shouldSuccessfullyDeleted() throws Exception {
        // given
        UUID requestId1 = randomUUID();
        UUID requestId2 = randomUUID();
        UUID projectId = randomUUID();
        Set<UUID> requestIds = Sets.newHashSet(requestId1, requestId2);
        RequestEntitiesBulkDelete deleteRequest = new RequestEntitiesBulkDelete(requestIds, projectId);
        // when, then
        this.mockMvc.perform(delete(FULL_REQUESTS_PATH)
                        .content(objectMapper.writeValueAsString(deleteRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void exportRequest_shouldSuccessfullyExported() throws Exception {
        // given
        UUID requestId = randomUUID();
        RequestExportResponse response = new RequestExportResponse("curl http://test.test");
        // when
        when(requestService.exportRequest(eq(requestId), any(), any(), eq(null)))
                .thenReturn(response.getCurlRequest());
        // then
        this.mockMvc.perform(get(FULL_REQUESTS_PATH + "/" + requestId + ApiPath.EXPORT_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()))
                .andExpect(result -> assertEquals(objectMapper.writeValueAsString(response),
                        result.getResponse().getContentAsString()));
    }

    @Test
    public void exportRequest_withContextVariables_shouldSuccessfullyExported() throws Exception {
        // given
        UUID requestId = randomUUID();
        RequestExportResponse response = new RequestExportResponse("curl http://test.test");
        List<ContextVariable> contextVariables = new ArrayList<>();
        contextVariables.add(new ContextVariable("text", "value", ContextVariableType.GLOBAL));
        contextVariables.add(new ContextVariable("num", 1, ContextVariableType.GLOBAL));
        String content = objectMapper.writeValueAsString(contextVariables);

        // when
        when(requestService.exportRequest(eq(requestId), any(), any(), eq(contextVariables)))
                .thenReturn(response.getCurlRequest());

        // then
        this.mockMvc.perform(post(FULL_REQUESTS_PATH + "/" + requestId + ApiPath.EXPORT_PATH)
                        .accept(MediaType.APPLICATION_JSON).content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()))
                .andExpect(result -> assertEquals(objectMapper.writeValueAsString(response),
                        result.getResponse().getContentAsString()));
    }

    @Test
    public void importRequest_shouldSuccessfullyImported() throws Exception {
        // given
        UUID projectId = randomUUID();
        CurlStringImportRequest curlStringImportRequest = new CurlStringImportRequest(projectId,
                "curl http://test.test");
        Request request = generateRandomHttpRequest();
        // when
        when(requestService.importRequest(eq(curlStringImportRequest)))
                .thenReturn(request);
        // then
        this.mockMvc.perform(post(FULL_REQUESTS_PATH + ApiPath.IMPORT_PATH
                        + "?projectId=" + projectId)
                        .content(objectMapper.writeValueAsString(curlStringImportRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()))
                .andExpect(result -> {
                    assertEquals(objectMapper.writeValueAsString(request),
                            result.getResponse().getContentAsString());
                });
    }

    @Test
    public void getContext_shouldSuccessfullyGetContext() throws Exception {
        // given
        UUID projectId = randomUUID();
        String contextId = new BigInteger("1").toString();
        String context = "{\"server\": \"SomeText\"}";
        ContextResponse expectedContext = new ContextResponse(context);
        // when
        when(requestService.getContext(eq(projectId), eq(contextId)))
                .thenReturn(context);
        // then
        this.mockMvc.perform(get(FULL_REQUESTS_PATH + ApiPath.CONTEXT_PATH
                        + "?projectId=" + projectId + "&context=" + contextId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals(objectMapper.writeValueAsString(expectedContext),
                        result.getResponse().getContentAsString()));
    }

    @Test
    public void getRequestTest_correctRequestIdAndProjectIdSpecified_shouldSuccessfullyGetRequest() throws Exception {
        // given
        Request request = generateRandomHttpRequest();
        UUID requestId = request.getId();
        UUID projectId = request.getProjectId();
        // when
        when(requestService.getRequest(eq(requestId), eq(projectId)))
                .thenReturn(request);
        // then
        this.mockMvc.perform(get(FULL_REQUESTS_PATH + "/" + requestId + "?projectId=" + projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()))
                .andExpect(result -> assertEquals(objectMapper.writeValueAsString(request),
                        result.getResponse().getContentAsString()));
    }

    @Test
    public void getRequestTest_inCorrectProjectIdSpecified_shouldReturn404() throws Exception {
        // given
        Request request = generateRandomHttpRequest();
        UUID requestId = request.getId();
        UUID projectId = request.getProjectId();
        // when
        when(requestService.getRequest(eq(requestId), eq(projectId)))
                .thenThrow(AtpEntityNotFoundException.class);
        // then
        this.mockMvc.perform(get(FULL_REQUESTS_PATH + "/" + requestId + "?projectId=" + projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void editRequestTest_correctRequestEntityEditRequestSpecified_shouldSuccessfullyEditRequest() throws Exception {
        // given
        Request request = generateRandomHttpRequest();
        RequestEntityEditRequest requestEntityEditRequest = generateRequestEditFromRequest(request);

        // when
        when(requestService.editRequest(eq(request.getId()), eq(requestEntityEditRequest)))
                .thenReturn(request);
        when(concurrentModificationService.getConcurrentModificationHttpStatus(
                eq(request.getId()), any(Date.class), any()))
                .thenReturn(HttpStatus.OK);

        // then
        this.mockMvc.perform(patch(FULL_REQUESTS_PATH + "/" + request.getId())
                        .content(objectMapper.writeValueAsString(requestEntityEditRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()))
                .andExpect(result -> assertEquals(objectMapper.writeValueAsString(request),
                        result.getResponse().getContentAsString()));
    }

    @Test
    public void editRequestTest_concurrentModification_shouldBeSaved_status409() throws Exception {
        // given
        Request request = generateRandomHttpRequest();
        RequestEntityEditRequest requestEntityEditRequest = generateRequestEditFromRequest(request);

        // when
        when(requestService.editRequest(eq(request.getId()), eq(requestEntityEditRequest)))
                .thenReturn(request);
        when(concurrentModificationService.getConcurrentModificationHttpStatus(
                eq(request.getId()), any(Date.class), any()))
                .thenReturn(HttpStatus.CONFLICT);

        // then
        this.mockMvc.perform(patch(FULL_REQUESTS_PATH + "/" + request.getId())
                        .content(objectMapper.writeValueAsString(requestEntityEditRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(result -> assertNull(result.getResolvedException()))
                .andExpect(result -> assertEquals(objectMapper.writeValueAsString(request),
                        result.getResponse().getContentAsString()));
    }

    @Test
    public void correctOrderChangeRequestSpecified_orderTest_shouldBeSuccessfullyExecuted() throws Exception {
        final RequestOrderChangeRequest request = new RequestOrderChangeRequest(randomUUID(), null, 0);
        this.mockMvc.perform(post(SERVICE_API_V1_PATH + REQUESTS_PATH + ID_PATH + ORDER_PATH, randomUUID())
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void executeRequestTest_shouldSuccessfullyExecute() throws Exception {
        // given
        ExecutionCollectionRequestExecuteRequest requestExecuteRequest = generateRequestExecuteRequest();
        // when
        when(actionService.executeAction(eq(requestExecuteRequest), any())).thenReturn(new ExecuteStepResponse());
        // then
        this.mockMvc.perform(post(FULL_REQUESTS_PATH + "/" + ApiPath.EXECUTE_PATH)
                        .content(objectMapper.writeValueAsString(requestExecuteRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void getRequestSettingsTest_allSettingsAppear() throws Exception {
        // given
        UUID requestId = randomUUID();
        AuthorizationSaveRequest authorization = EntitiesGenerator.generateRandomOAuth2AuthorizationSaveRequest();
        Settings expectedSettings = new Settings(true, true, true, true, true, authorization);
        expectedSettings.setId(requestId);
        expectedSettings.setName("test");

        // when
        when(requestService.getSettings(any())).thenReturn(expectedSettings);

        // then
        this.mockMvc.perform(get(FULL_REQUESTS_PATH + "/" + requestId + SETTINGS_PATH))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()))
                .andExpect(result -> assertEquals(objectMapper.writeValueAsString(expectedSettings),
                        result.getResponse().getContentAsString()));
    }

    @Test
    public void downloadResponseAsFileTest_shouldSuccessfullyReturn200() throws Exception {
        // given, when, then
        this.mockMvc.perform(get(FULL_REQUESTS_PATH + "/" + UUID.randomUUID() + ApiPath.DOWNLOAD_RESPONSE_PATH + "?executionId=" + UUID.randomUUID()))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
