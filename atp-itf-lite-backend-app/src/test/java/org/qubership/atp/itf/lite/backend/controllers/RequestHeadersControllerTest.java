package org.qubership.atp.itf.lite.backend.controllers;

import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.DISABLE_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.ENABLE_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.ID_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.REQUEST_HEADERS_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SERVICE_API_V1_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.qubership.atp.itf.lite.backend.service.RequestHeaderService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {RequestHeadersController.class})
@MockBean(RequestHeaderService.class)
class RequestHeadersControllerTest extends AbstractControllerTest {

    @Test
    void disableRequestHeader() throws Exception {
        final UUID headerId = UUID.randomUUID();
        final UUID projectId = UUID.randomUUID();
        final UUID requestId = UUID.randomUUID();
        this.mockMvc.perform(put(SERVICE_API_V1_PATH + REQUEST_HEADERS_PATH + ID_PATH + DISABLE_PATH, headerId)
                .param("projectId", projectId.toString())
                .param("requestId", requestId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void enableRequestHeader() throws Exception {
        final UUID headerId = UUID.randomUUID();
        final UUID projectId = UUID.randomUUID();
        final UUID requestId = UUID.randomUUID();
        this.mockMvc.perform(put(SERVICE_API_V1_PATH + REQUEST_HEADERS_PATH + ID_PATH + ENABLE_PATH, headerId)
                .param("projectId", projectId.toString())
                .param("requestId", requestId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }
}