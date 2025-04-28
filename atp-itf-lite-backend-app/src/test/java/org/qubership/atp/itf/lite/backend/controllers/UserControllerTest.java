package org.qubership.atp.itf.lite.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SERVICE_API_V1_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.USER_SETTINGS_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.itf.lite.backend.handlers.MethodArgumentExceptionHandler;
import org.qubership.atp.itf.lite.backend.model.entities.user.UserSettings;
import org.qubership.atp.itf.lite.backend.service.UserService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import joptsimple.internal.Strings;

@ContextConfiguration(classes = {UserController.class, MethodArgumentExceptionHandler.class})
@Isolated
public class UserControllerTest extends AbstractControllerTest {

    @MockBean
    private UserService userService;

    @Test
    public void createUserSettings() throws Exception {
        final UUID projectId = UUID.randomUUID();
        final UserSettings settings = new UserSettings();
        when(userService.saveUserSettings(any(), any())).thenReturn(settings);
        this.mockMvc.perform(post(SERVICE_API_V1_PATH + USER_SETTINGS_PATH)
                        .header("Authorization", Strings.EMPTY)
                        .param("projectId", String.valueOf(projectId))
                        .content(objectMapper.writeValueAsString(settings))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void getSettingsByUser_userExistInDataBase_successfullyReturnUserSettings() {
        final UUID projectId = UUID.randomUUID();
        final UserSettings settings = new UserSettings();
        when(userService.getSettingsByUser(any())).thenReturn(Collections.singletonList(settings));
        try {
            this.mockMvc.perform(get(SERVICE_API_V1_PATH + USER_SETTINGS_PATH)
                            .header("Authorization", Strings.EMPTY)
                            .param("projectId", projectId.toString()))
                    .andDo(print())
                    .andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getSettingsByUser_userNotExistInDataBase_failedReturnUserSettings() {
        final UUID projectId = UUID.randomUUID();
        when(userService.getSettingsByUser(any())).thenReturn(null);
        try {
            this.mockMvc.perform(get(SERVICE_API_V1_PATH + USER_SETTINGS_PATH)
                            .header("Authorization", Strings.EMPTY)
                            .param("projectId", projectId.toString()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
