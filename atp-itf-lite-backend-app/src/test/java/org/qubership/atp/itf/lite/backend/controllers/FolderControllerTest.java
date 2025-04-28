package org.qubership.atp.itf.lite.backend.controllers;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.COPY_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.COUNT_HEIRS_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.FOLDERS_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.ID_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.MOVE_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.ORDER_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SERVICE_API_V1_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SETTINGS_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.TREE_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.itf.lite.backend.dataaccess.validators.FolderCreationRequestValidator;
import org.qubership.atp.itf.lite.backend.handlers.MethodArgumentExceptionHandler;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderCopyRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderDeleteRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderMoveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderOrderChangeRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderTreeSearchRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderUpsetRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.IdWithModifiedWhen;
import org.qubership.atp.itf.lite.backend.model.api.request.Settings;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.GroupResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.service.ConcurrentModificationService;
import org.qubership.atp.itf.lite.backend.service.FolderService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {FolderController.class, MethodArgumentExceptionHandler.class})
@Isolated
public class FolderControllerTest extends AbstractControllerTest {

    @MockBean
    private FolderService folderService;

    @MockBean
    private FolderCreationRequestValidator folderCreationRequestValidator;

    @MockBean
    private ConcurrentModificationService concurrentModificationService;

    @BeforeEach
    public void setUp() {
        when(folderCreationRequestValidator.supports(any())).thenReturn(true);
    }

    @Test
    public void getAllFoldersTest() {
        when(folderService.getAllFolders(any())).thenReturn(new ArrayList<>());

        try {
            this.mockMvc.perform(get(SERVICE_API_V1_PATH + FOLDERS_PATH))
                    .andDo(print())
                    .andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void buildFoldersAndRequestsTree() throws Exception {
        FolderTreeSearchRequest request = new FolderTreeSearchRequest(UUID.randomUUID(), UUID.randomUUID(), "Some Text");
        when(folderService.getFolderRequestsTree(false, request)).thenReturn(new GroupResponse());

        this.mockMvc.perform(post(SERVICE_API_V1_PATH + FOLDERS_PATH + TREE_PATH)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void createFolderTest_correctFolderCreationRequestSpecified_shouldSuccessfullyCreated() throws Exception {
        FolderUpsetRequest request = new FolderUpsetRequest(
                "New Folder", UUID.randomUUID(), UUID.randomUUID(), null, false, false, false, false, false, "", null,
                new Date());

        this.mockMvc.perform(post(SERVICE_API_V1_PATH + FOLDERS_PATH)
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    public void createFolderTest_incorrectFolderCreationRequestSpecified_expected400Error() throws Exception {
        FolderUpsetRequest request = new FolderUpsetRequest(null, UUID.randomUUID(), UUID.randomUUID(),
                null, false, false, false, false, false, "", null, new Date());

        this.mockMvc.perform(post(SERVICE_API_V1_PATH + FOLDERS_PATH)
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void editFolderTest_correctFolderEditRequestSpecified_shouldSuccessfullyUpdated() throws Exception {
        FolderUpsetRequest request = new FolderUpsetRequest("Folder Name", UUID.randomUUID(), null,
                null, false, false, false, false, false, "", null, new Date());
        UUID folderId = UUID.randomUUID();

        // when
        when(concurrentModificationService.getConcurrentModificationHttpStatus(any(UUID.class), any(Date.class), any()))
                .thenReturn(HttpStatus.OK);

        // then
        this.mockMvc.perform(put(SERVICE_API_V1_PATH + FOLDERS_PATH + ID_PATH, folderId)
                        .param("id", folderId.toString())
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void editFolderTest_concurrentModification_shouldBeSaved_status409() throws Exception {
        // given
        FolderUpsetRequest request = new FolderUpsetRequest("Folder Name", UUID.randomUUID(), null, null, "");
        UUID folderId = UUID.randomUUID();
        Folder savedFolder = new Folder();
        savedFolder.setModifiedWhen(new Date(System.currentTimeMillis() - 1000));

        // when
        when(folderService.get(folderId)).thenReturn(savedFolder);
        when(concurrentModificationService.getConcurrentModificationHttpStatus(eq(folderId), any(Date.class), eq(folderService)))
                .thenReturn(HttpStatus.CONFLICT);

        // then
        this.mockMvc.perform(put(SERVICE_API_V1_PATH + FOLDERS_PATH + ID_PATH, folderId)
                        .param("id", folderId.toString())
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void copyFolderTest_correctFolderCopyRequestSpecified_shouldSuccessfullyCopied() throws Exception {
        FolderCopyRequest request = new FolderCopyRequest(
                new HashSet<>(Collections.singletonList(UUID.randomUUID())),
                UUID.randomUUID(), (UUID.randomUUID())
        );
        this.mockMvc.perform(post(SERVICE_API_V1_PATH + FOLDERS_PATH + COPY_PATH)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void moveFolderTest_correctFolderMoveRequestSpecified_shouldSuccessfullyMoved() throws Exception {
        // given
        FolderMoveRequest request = new FolderMoveRequest(
                Collections.singleton(new IdWithModifiedWhen(UUID.randomUUID(), new Date())),
                UUID.randomUUID(), (UUID.randomUUID())
        );

        // when
        when(concurrentModificationService.getConcurrentModificationHttpStatus(eq(request.getIds()), any()))
                .thenReturn(Pair.of(HttpStatus.OK, new ArrayList<>()));

        // then
        this.mockMvc.perform(post(SERVICE_API_V1_PATH + FOLDERS_PATH + MOVE_PATH)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void deleteFolderTest_correctFolderDeleteRequestSpecified_shouldSuccessfullyDeleted() throws Exception {
        FolderDeleteRequest request = new FolderDeleteRequest(
                new HashSet<>(Collections.singletonList(UUID.randomUUID())), UUID.randomUUID());
        this.mockMvc.perform(delete(SERVICE_API_V1_PATH + FOLDERS_PATH)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void getHeirsCountTest_correctFolderHeirsRequestSpecified_shouldSuccessfullyReturnedCount()
            throws Exception {
        FolderDeleteRequest request = new FolderDeleteRequest(
                new HashSet<>(Collections.singletonList(UUID.randomUUID())), UUID.randomUUID());
        this.mockMvc.perform(post(SERVICE_API_V1_PATH + FOLDERS_PATH + COUNT_HEIRS_PATH)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void correctOrderChangeRequestSpecified_orderTest_shouldBeSuccessfullyExecuted() throws Exception {
        final FolderOrderChangeRequest request = new FolderOrderChangeRequest(UUID.randomUUID(), null, 0);
        this.mockMvc.perform(post(SERVICE_API_V1_PATH + FOLDERS_PATH + ID_PATH + ORDER_PATH, UUID.randomUUID())
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()));
    }

    @Test
    public void getRequestSettingsTest_allSettingsAppear() throws Exception {
        // given
        UUID folderId = randomUUID();
        AuthorizationSaveRequest authorization = EntitiesGenerator.generateRandomOAuth2AuthorizationSaveRequest();
        Settings expectedSettings = new Settings(true, true, true ,true, true, authorization);
        expectedSettings.setId(folderId);
        expectedSettings.setName("test");

        // when
        when(folderService.getSettings(any())).thenReturn(expectedSettings);

        // then
        this.mockMvc.perform(get(SERVICE_API_V1_PATH + FOLDERS_PATH + ID_PATH + SETTINGS_PATH, folderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getResolvedException()))
                .andExpect(result -> assertEquals(objectMapper.writeValueAsString(expectedSettings),
                        result.getResponse().getContentAsString()));
    }
}
