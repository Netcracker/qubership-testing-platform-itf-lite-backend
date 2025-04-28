package org.qubership.atp.itf.lite.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.EXECUTORS_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.HISTORY_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.ID_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SERVICE_API_V1_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.handlers.MethodArgumentExceptionHandler;
import org.qubership.atp.itf.lite.backend.model.entities.history.HistoryRequestDetailsResponse;
import org.qubership.atp.itf.lite.backend.model.entities.history.HistorySearchRequest;
import org.qubership.atp.itf.lite.backend.model.entities.history.PaginatedResponse;
import org.qubership.atp.itf.lite.backend.model.entities.history.RequestExecution;
import org.qubership.atp.itf.lite.backend.service.RequestExecutionHistoryService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {HistoryController.class, MethodArgumentExceptionHandler.class})
@Isolated
public class HistoryControllerTest extends AbstractControllerTest {

    @MockBean
    private RequestExecutionHistoryService requestExecutionHistoryService;

    @Test
    public void getExecutionsHistory() throws Exception {
        final UUID projectId = UUID.randomUUID();
        HistorySearchRequest request = new HistorySearchRequest(projectId, 0, 10, null, null);
        when(requestExecutionHistoryService.getExecutionHistory(any())).thenReturn(
                new PaginatedResponse<>(1, Collections.singletonList(new RequestExecution())));
        this.mockMvc.perform(post(SERVICE_API_V1_PATH + HISTORY_PATH)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void getExecutionHistoryDetails() {
        final UUID projectId = UUID.randomUUID();
        UUID historyItemId = UUID.randomUUID();

        when(requestExecutionHistoryService.getExecutionHistoryDetailsByHistoryItemId(any()))
                .thenReturn(new HistoryRequestDetailsResponse());

        try {
            this.mockMvc.perform(get(SERVICE_API_V1_PATH + HISTORY_PATH + ID_PATH, historyItemId)
                            .param("type", TransportType.REST.toString())
                            .param("projectId", projectId.toString()))
                    .andDo(print())
                    .andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getExecutorsInRequestExecutionHistory() {
        final UUID projectId = UUID.randomUUID();

        when(requestExecutionHistoryService.getExecutorsInRequestExecutionHistory(any()))
                .thenReturn(Collections.emptyList());

        try {
            this.mockMvc.perform(get(SERVICE_API_V1_PATH + HISTORY_PATH + EXECUTORS_PATH)
                            .param("projectId", projectId.toString()))
                    .andDo(print())
                    .andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
