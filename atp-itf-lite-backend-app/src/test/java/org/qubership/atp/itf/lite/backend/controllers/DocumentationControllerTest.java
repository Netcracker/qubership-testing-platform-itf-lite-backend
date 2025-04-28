package org.qubership.atp.itf.lite.backend.controllers;

import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SERVICE_API_V1_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.itf.lite.backend.enums.EntityType;
import org.qubership.atp.itf.lite.backend.facade.DocumentationFacade;
import org.qubership.atp.itf.lite.backend.model.api.ApiPath;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderTreeSearchRequest;
import org.qubership.atp.itf.lite.backend.model.documentation.RequestDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.RequestEntityEditDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.TreeFolderDocumentationResponse;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {
        DocumentationController.class
})
@Isolated
class DocumentationControllerTest extends AbstractControllerTest {
    @MockBean
    private DocumentationFacade documentationFacade;

    @Test
    public void getFolderRequestsTree() throws Exception {
        FolderTreeSearchRequest request = new FolderTreeSearchRequest(UUID.randomUUID(), UUID.randomUUID(), "Some Text");

        when(documentationFacade.getDocumentationByFolder(request, 0, 1)).thenReturn(ResponseEntity.ok(new TreeFolderDocumentationResponse()));

        this.mockMvc.perform(post(SERVICE_API_V1_PATH + ApiPath.DOCUMENTATION_PATH + ApiPath.FOLDER_PATH + "?page=0&pageSize=1")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void getRequestDocumentation() throws Exception {
        FolderTreeSearchRequest request = new FolderTreeSearchRequest(UUID.randomUUID(), UUID.randomUUID(), "Some Text");
        UUID uuid = UUID.randomUUID();

        when(documentationFacade.getRequestDocumentation(UUID.randomUUID())).thenReturn(ResponseEntity.ok(new RequestDocumentation()));

        this.mockMvc.perform(get(SERVICE_API_V1_PATH + ApiPath.DOCUMENTATION_PATH + ApiPath.REQUEST_PATH + ApiPath.REQUEST_ID_PATH, uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void editDocumentationRequest() throws Exception {
        UUID uuid = UUID.randomUUID();
        RequestEntityEditDocumentation request = new RequestEntityEditDocumentation();
        request.setType(EntityType.REQUEST);
        request.setProjectId(UUID.randomUUID());
        request.setDescription("doc");

        this.mockMvc.perform(patch(SERVICE_API_V1_PATH + ApiPath.DOCUMENTATION_PATH + ApiPath.EDIT_PATH + ApiPath.ID_PATH, uuid)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
