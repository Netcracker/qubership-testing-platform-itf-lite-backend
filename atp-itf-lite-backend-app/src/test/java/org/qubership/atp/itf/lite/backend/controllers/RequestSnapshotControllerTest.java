package org.qubership.atp.itf.lite.backend.controllers;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.itf.lite.backend.feign.service.RamService;
import org.qubership.atp.itf.lite.backend.handlers.MethodArgumentExceptionHandler;
import org.qubership.atp.itf.lite.backend.model.api.ApiPath;
import org.qubership.atp.itf.lite.backend.model.api.request.BulkDeleteSnapshotsRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestSnapshotResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.RequestSnapshot;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.service.FormDataPartService;
import org.qubership.atp.itf.lite.backend.service.RequestSnapshotService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.SerializationFeature;

@ContextConfiguration(classes = {RequestSnapshotController.class, MethodArgumentExceptionHandler.class})
@MockBeans({
        @MockBean(FormDataPartService.class),
        @MockBean(RamService.class)
})
@Isolated
public class RequestSnapshotControllerTest extends AbstractControllerTest {
    static final String FULL_REQUESTS_PATH = ApiPath.SERVICE_API_V1_PATH + ApiPath.REQUEST_SNAPSHOT_PATH;

    @MockBean
    private RequestSnapshotService requestSnapshotService;

    @BeforeAll
    public static void beforeAll() {
        // filled createdWhen and modifiedWhen for request
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }


    @Test
    public void saveRequestSnapshotTest_correctRequestEntitySaveRequestSpecified_shouldSuccessfullyEdited() throws Exception {
        // given
        UUID requestId = randomUUID();
        UUID sessionId = randomUUID();
        HttpRequest httpRequest = generateRandomHttpRequest();
        httpRequest.setName("Edited Request");
        RequestSnapshot requestSnapshot = new RequestSnapshot();
        requestSnapshot.setSessionId(sessionId);
        requestSnapshot.setRequestId(requestId);
        requestSnapshot.setRequest(objectMapper.writeValueAsString(httpRequest));
        // then
        // save endpoint receives form data with dictionary and request entity
        MockMultipartHttpServletRequestBuilder builder = multipart(FULL_REQUESTS_PATH);
        MockMultipartFile requestEntity = new MockMultipartFile("snapshotEntity", "snapshotEntity",
                MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsString(requestSnapshot).getBytes());
        builder.with(new RequestPostProcessor() {
            @Override
            public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
                request.setMethod("POST");
                return request;
            }
        });
        this.mockMvc.perform(builder.file(requestEntity)
                            .queryParam("projectId", UUID.randomUUID().toString()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }


    @Test
    public void deleteRequestSnapshotTest_shouldSuccessfullyDeleted() throws Exception {
        // given
        UUID requestId = randomUUID();
        UUID projectId = randomUUID();
        UUID sessionId = randomUUID();
        // when, then
        this.mockMvc.perform(delete(FULL_REQUESTS_PATH)
                .queryParam("projectId", projectId.toString())
                .queryParam("sessionId", sessionId.toString())
                .queryParam("requestId", requestId.toString()))
                .andDo(print())
                .andExpect(status().isOk());
    }


    @Test
    public void bulkDeleteRequestSnapshotTest_shouldSuccessfullyDeleted() throws Exception {
        // given
        UUID requestId = randomUUID();
        UUID projectId = randomUUID();
        UUID sessionId = randomUUID();
        BulkDeleteSnapshotsRequest bulkDeleteSnapshotsRequest = new BulkDeleteSnapshotsRequest();
        bulkDeleteSnapshotsRequest.setRequestIds(Collections.singletonList(requestId));
        // when, then
        this.mockMvc.perform(post(FULL_REQUESTS_PATH + "/bulkDelete")
                .content(objectMapper.writeValueAsString(bulkDeleteSnapshotsRequest))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .queryParam("projectId", projectId.toString())
                .queryParam("sessionId", sessionId.toString()))
                .andDo(print())
                .andExpect(status().isOk());
    }


    @Test
    public void getRequestTest_correctRequestIdAndProjectIdSpecified_shouldSuccessfullyGetRequest() throws Exception {
        // given
        Request request = generateRandomHttpRequest();
        UUID requestId = randomUUID();
        UUID projectId = randomUUID();
        UUID sessionId = randomUUID();

        HttpRequest httpRequest = generateRandomHttpRequest();
        httpRequest.setName("Edited Request");
        RequestSnapshotResponse requestSnapshotResponse = RequestSnapshotResponse.builder()
                .request(objectMapper.writeValueAsString(httpRequest))
                .requestId(requestId)
                .sessionId(sessionId).build();
        // when
        when(requestSnapshotService.getSnapshot(eq(sessionId), eq(requestId)))
                .thenReturn(requestSnapshotResponse);
        // then
        this.mockMvc.perform(get(FULL_REQUESTS_PATH)
                .queryParam("projectId", projectId.toString())
                .queryParam("sessionId", sessionId.toString())
                .queryParam("requestId", requestId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()))
                .andExpect(result -> assertEquals(objectMapper.writeValueAsString(requestSnapshotResponse),
                        result.getResponse().getContentAsString()));
    }


}
