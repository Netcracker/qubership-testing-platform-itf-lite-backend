package org.qubership.atp.itf.lite.backend.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SERVICE_API_V1_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.exceptions.history.ItfLiteRevisionHistoryIncorrectTypeException;
import org.qubership.atp.itf.lite.backend.service.history.impl.HistoryServiceFactory;
import org.qubership.atp.itf.lite.backend.service.history.impl.HttpRequestRestoreHistoryService;
import org.qubership.atp.itf.lite.backend.service.history.impl.HttpRequestRetrieveHistoryService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {HistoryChangeController.class})
@MockBean(classes = ModelMapper.class)
@Isolated
public class HistoryChangeControllerTest extends AbstractControllerTest {

    private static final String GET_ALL_HISTORY = SERVICE_API_V1_PATH
            + "/history/{projectId}/{itemType}/{id}?offset=0&limit=10";
    private static final String GET_ENTITIES_BY_VERSION = SERVICE_API_V1_PATH
            + "/entityversioning/{projectId}/{itemType}/{uuid}?versions=1,2";

    private static final String RESTORE_ENTITY_BY_VERSION = SERVICE_API_V1_PATH
            + "/history/restore/{projectId}/{itemType}/{id}/revision/{revisionId}";

    @MockBean
    private HistoryServiceFactory historyServiceFactory;

    @Test
    public void getAllHistoryTest_withHttpRequestService_callAllHistory() throws Exception {
        final UUID projectId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        HttpRequestRetrieveHistoryService historyService = mock(HttpRequestRetrieveHistoryService.class);
        when(historyServiceFactory.getRetrieveHistoryService(eq("request"), eq(itemId)))
                .thenReturn(Optional.of(historyService));

        this.mockMvc.perform(get(GET_ALL_HISTORY, projectId, "request", itemId))
                .andDo(print())
                .andExpect(status().isOk());

        verify(historyServiceFactory, times(1)).getRetrieveHistoryService(any(), any());
        verify(historyService, times(1)).getAllHistory(any(), eq(0), eq(10));
    }

    @Test
    public void getAllHistoryTest_withoutService_returnItfLiteException() throws Exception {
        final UUID projectId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        this.mockMvc.perform(get(GET_ALL_HISTORY, projectId, "request", itemId))
                .andDo(print())
                .andExpect(status().is(400))
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof ItfLiteRevisionHistoryIncorrectTypeException));
    }

    @Test
    public void getEntitiesByVersionTest_withHttpRequestService_callEntitiesByVersion() throws Exception {
        final UUID projectId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        HttpRequestRetrieveHistoryService historyService = mock(HttpRequestRetrieveHistoryService.class);
        when(historyServiceFactory.getRetrieveHistoryService(eq("request"), eq(itemId)))
                .thenReturn(Optional.of(historyService));

        this.mockMvc.perform(get(GET_ENTITIES_BY_VERSION, projectId, "request", itemId))
                .andDo(print())
                .andExpect(status().isOk());

        verify(historyServiceFactory, times(1)).getRetrieveHistoryService(any(), any());
        verify(historyService, times(1)).getEntitiesByVersions(any(), any());
    }

    @Test
    public void getEntitiesByVersionTest_withoutService_returnItfLiteException() throws Exception {
        final UUID projectId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        this.mockMvc.perform(get(GET_ENTITIES_BY_VERSION, projectId, "request", itemId))
                .andDo(print())
                .andExpect(status().is(400))
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof ItfLiteRevisionHistoryIncorrectTypeException));
    }

    @Test
    public void restoreToRevisionTest_withHttpRequestService_callRestoreEntity() throws Exception {
        final UUID projectId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        HttpRequestRestoreHistoryService historyService = mock(HttpRequestRestoreHistoryService.class);
        when(historyServiceFactory.getRestoreHistoryService(eq("request"), eq(itemId)))
                .thenReturn(Optional.of(historyService));

        this.mockMvc.perform(post(RESTORE_ENTITY_BY_VERSION, projectId, "request", itemId, 1))
                .andDo(print())
                .andExpect(status().isOk());

        verify(historyServiceFactory, times(1)).getRestoreHistoryService(any(), any());
        verify(historyService, times(1)).restoreToRevision(any(), anyLong());
    }

    @Test
    public void restoreToRevisionTest_withoutService_returnItfLiteException() throws Exception {
        final UUID projectId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        this.mockMvc.perform(post(RESTORE_ENTITY_BY_VERSION, projectId, "request", itemId, 1))
                .andDo(print())
                .andExpect(status().is(400))
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof ItfLiteRevisionHistoryIncorrectTypeException));
    }
}
