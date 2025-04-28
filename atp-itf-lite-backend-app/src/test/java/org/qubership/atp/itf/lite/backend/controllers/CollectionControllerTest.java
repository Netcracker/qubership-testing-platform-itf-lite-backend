package org.qubership.atp.itf.lite.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.COLLECTION_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.EXECUTE_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.IMPORT_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SERVICE_API_V1_PATH;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.ArgumentCaptor;
import org.qubership.atp.itf.lite.backend.handlers.MethodArgumentExceptionHandler;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.api.request.CollectionExecuteRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.ImportCollectionsRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.GroupResponse;
import org.qubership.atp.itf.lite.backend.service.CollectionsService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@ContextConfiguration(classes = {CollectionController.class, MethodArgumentExceptionHandler.class})
@Isolated
public class CollectionControllerTest extends AbstractControllerTest {

    @MockBean
    private CollectionsService collectionsService;

    static final String PATH_TO_ZIP = "src/test/resources/tests/postmanCollection.zip";

    @Test
    public void importCollectionTest() throws IOException {
        when(collectionsService.importCollections(any(), any())).thenReturn(new ArrayList<>());

        File zipFile = new File(PATH_TO_ZIP);
        FileInputStream input = new FileInputStream(zipFile);
        MockMultipartFile file = new MockMultipartFile("file",
                zipFile.getName(), "text/plain", IOUtils.toByteArray(input));

        ImportCollectionsRequest importCollectionsRequest = new ImportCollectionsRequest(UUID.randomUUID(),
                "test collection", null);
        MockMultipartFile requestEntity = new MockMultipartFile("requestEntity", "requestEntity",
                MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsString(importCollectionsRequest).getBytes());

        try {
            this.mockMvc.perform(MockMvcRequestBuilders.multipart(
                                    SERVICE_API_V1_PATH + COLLECTION_PATH + IMPORT_PATH)
                            .file(file)
                            .file(requestEntity))
                    .andDo(print())
                    .andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void executeCollectionTest() {
        // given
        UUID projectId = UUID.randomUUID();
        CollectionExecuteRequest request = new CollectionExecuteRequest();
        request.setName("Test Collection");
        request.setEnvironmentIds(Collections.singletonList(UUID.randomUUID()));
        request.setEmailRecipients(Collections.singletonList("test@example.com"));
        request.setEmailTemplateId(UUID.randomUUID());
        request.setEmailSubject("Test Subject");
        request.setTaToolIds(Collections.singletonList(UUID.randomUUID()));
        request.setLogCollectorTemplateId(UUID.randomUUID());
        request.setProjectId(projectId);
        request.setFlags(Collections.emptyList());
        request.setTreeNodes(Collections.singletonList(new GroupResponse(EntitiesGenerator.generateRandomHttpRequest(),
                null)));
        request.setPropagateCookies(true);

        // when
        when(collectionsService.executeCollection(any(), any())).thenReturn(new ArrayList<>());

        // then
        try {
            this.mockMvc.perform(MockMvcRequestBuilders.multipart(
                    SERVICE_API_V1_PATH + COLLECTION_PATH + EXECUTE_PATH)
                            .header(HttpHeaders.AUTHORIZATION, "")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArgumentCaptor<CollectionExecuteRequest> requestCaptor =
                ArgumentCaptor.forClass(CollectionExecuteRequest.class);
        verify(collectionsService).executeCollection(any(), requestCaptor.capture());
        CollectionExecuteRequest executionRequest = requestCaptor.getValue();
        Assertions.assertTrue(executionRequest.isPropagateCookies());
    }
}
