package org.qubership.atp.itf.lite.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateHttpRequestSaveFromHttpRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestItfExportRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestMiaExportRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mock;
import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.handlers.MethodArgumentExceptionHandler;
import org.qubership.atp.itf.lite.backend.model.api.ApiPath;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestItfExportRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestMiaExportRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.service.RequestExportService;
import org.qubership.atp.itf.lite.backend.service.RequestService;
import org.qubership.atp.itf.lite.backend.service.SseEmitterService;
import org.qubership.atp.itf.lite.backend.service.UserService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ContextConfiguration(classes = {SseController.class, MethodArgumentExceptionHandler.class})
@MockBeans({
        @MockBean(UserService.class),
        @MockBean(ModelMapper.class)
})
@Isolated
public class SseControllerTest extends AbstractControllerTest {

    static final String FULL_REQUESTS_PATH = ApiPath.SERVICE_API_V1_PATH + ApiPath.SSE_PATH;

    @MockBean
    private RequestService requestService;

    @MockBean
    private RequestExportService requestExportService;

    @MockBean
    private SseEmitterService sseEmitterService;

    @Mock
    private static SseEmitter sseEmitter;

    private static final String PROJECT_ID = "projectId";
    private static final String SSE_ID = "sseId";
    private static final String ENVIRONMENT_ID = "environmentId";

    @Test
    public void connect_projectIdIsSpecified_shouldCreatedEvent() throws Exception {
        // given
        final UUID projectId = UUID.randomUUID();
        final UUID sseId = UUID.randomUUID();
        // when
        when(sseEmitterService.generateAndConfigureEmitter(eq(sseId), any()))
                .thenReturn(sseEmitter);
        MvcResult mvcResult = mockMvc.perform(
                get(FULL_REQUESTS_PATH + ApiPath.REQUESTS_PATH + ApiPath.CONNECT_PATH)
                        .queryParam(PROJECT_ID, projectId.toString())
                        .queryParam(SSE_ID, sseId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(request().asyncStarted())
                .andDo(MockMvcResultHandlers.log())
                .andReturn();
        // then
        String event = mvcResult.getResponse().getContentAsString();
        Assertions.assertNotNull(event);
    }

    @Test
    public void connect_projectIdIsSpecifiedAndEmitterExists_shouldGetEvent() throws Exception {
        // given
        final UUID projectId = UUID.randomUUID();
        final UUID sseId = UUID.randomUUID();
        // when
        when(sseEmitterService.getEmitter(eq(sseId)))
                .thenReturn(sseEmitter);

        MvcResult mvcResult = mockMvc.perform(
                        get(FULL_REQUESTS_PATH + ApiPath.REQUESTS_PATH + ApiPath.CONNECT_PATH)
                                .queryParam(PROJECT_ID, projectId.toString())
                                .queryParam(SSE_ID, sseId.toString())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(request().asyncStarted())
                .andDo(MockMvcResultHandlers.log())
                .andReturn();

        // then
        String event = mvcResult.getResponse().getContentAsString();
        Assertions.assertNotNull(event);
    }

    @Test
    public void executeRequest_shouldCompleteEmitter() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        final UUID environmentId = UUID.randomUUID();
        HttpRequest httpRequest = generateRandomHttpRequest();
        HttpRequestEntitySaveRequest requestEntitySaveRequest = generateHttpRequestSaveFromHttpRequest(httpRequest);
        // when
        when(requestService.getRequest(eq(httpRequest.getId())))
                .thenReturn(httpRequest);

        MockMultipartHttpServletRequestBuilder builder = multipart(FULL_REQUESTS_PATH + ApiPath.REQUESTS_PATH
                + "/" + httpRequest.getId() + ApiPath.EXECUTE_PATH);
        MockMultipartFile requestEntity = new MockMultipartFile("requestEntity", "requestEntity",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(requestEntitySaveRequest).getBytes());
        MvcResult mvcResult = mockMvc.perform(builder
                        .file(requestEntity)
                        .queryParam(SSE_ID, sseId.toString())
                        .queryParam(ENVIRONMENT_ID, environmentId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andDo(MockMvcResultHandlers.log())
                .andReturn();
        // then
        String event = mvcResult.getResponse().getContentAsString();
        Assertions.assertNotNull(event);

        verify(sseEmitterService).processRequestExecution(any(), any(), any(), any(), any(), any(), any(),
                any(), any());
    }

    @Test
    public void exportToMiaRequestsTest_shouldCompleteEmitter() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        RequestMiaExportRequest requestMiaExportRequest = generateRequestMiaExportRequest();
        // when
        when(sseEmitterService.getEmitter(eq(sseId)))
                .thenReturn(sseEmitter);

        // then
        mockMvc.perform(post(FULL_REQUESTS_PATH
                        + ApiPath.REQUESTS_PATH + ApiPath.MIA_PATH + ApiPath.EXPORT_PATH)
                        .queryParam(SSE_ID, sseId.toString())
                        .content(objectMapper.writeValueAsString(requestMiaExportRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andDo(MockMvcResultHandlers.log())
                .andReturn();

        verify(requestExportService).exportRequests(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void exportToItfRequestsTest_shouldCompleteEmitter() throws Exception {
        // given
        final UUID sseId = UUID.randomUUID();
        RequestItfExportRequest requestItfExportRequest = generateRequestItfExportRequest();
        // when
        when(sseEmitterService.getEmitter(eq(sseId)))
                .thenReturn(sseEmitter);

        // then
        mockMvc.perform(post(FULL_REQUESTS_PATH
                        + ApiPath.REQUESTS_PATH + ApiPath.ITF_PATH + ApiPath.EXPORT_PATH)
                        .queryParam(SSE_ID, sseId.toString())
                        .content(objectMapper.writeValueAsString(requestItfExportRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andDo(MockMvcResultHandlers.log())
                .andReturn();

        verify(requestExportService).exportRequests(any(), any(), any(), any(), any(), any());
    }
}
