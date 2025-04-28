package org.qubership.atp.itf.lite.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.COOKIES_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.IMPORT_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.PROJECT_ID_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.PROJECT_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SERVICE_API_V1_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.itf.lite.backend.model.api.dto.CookieDto;
import org.qubership.atp.itf.lite.backend.model.api.dto.CookiesDto;
import org.qubership.atp.itf.lite.backend.model.api.request.ImportFromRamRequest;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;
import org.qubership.atp.itf.lite.backend.service.CookieService;
import org.qubership.atp.itf.lite.backend.utils.CookieUtils;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {CookieController.class})
@Isolated
public class CookieControllerTest extends AbstractControllerTest {

    @MockBean
    private CookieService cookieService;

    private static final String CONTROLLER_PATH = SERVICE_API_V1_PATH + PROJECT_PATH + PROJECT_ID_PATH + COOKIES_PATH;

    @Test
    public void saveCookies() throws Exception {
        // given
        UUID projectId = UUID.randomUUID();
        List<CookiesDto> cookiesDtoList = new ArrayList<>();
        CookiesDto cookiesDto = new CookiesDto();
        cookiesDto.setDomain("test");
        cookiesDto.setCookies(Arrays.asList(
                new CookieDto("Cookie_1", "Cookie_1=value", false),
                new CookieDto("Cookie_2", "Cookie_2=value", false)));
        List<Cookie> expectedCookie = CookieUtils.convertToCookieList(cookiesDtoList);

        // when
        this.mockMvc.perform(post(CONTROLLER_PATH, projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cookiesDtoList))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
        // then
        verify(cookieService).deleteByUserIdAndProjectId(eq(projectId));
        verify(cookieService).fillCookieInfo(any(), eq(projectId));
        verify(cookieService).save(eq(expectedCookie));
    }

    @Test
    public void getCookies() throws Exception {
        // given
        UUID projectId = UUID.randomUUID();

        // when
        this.mockMvc.perform(get(CONTROLLER_PATH, projectId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
        // then
        verify(cookieService).getNotExpiredCookiesByUserIdAndProjectId(eq(projectId));
    }

    @Test
    public void importCookiesFromRam() throws Exception {
        // given
        UUID projectId = UUID.randomUUID();
        ImportFromRamRequest importRequest = new ImportFromRamRequest();

        // when
        this.mockMvc.perform(post(CONTROLLER_PATH + IMPORT_PATH, projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        // then
        verify(cookieService).importCookiesFromRam(eq(projectId), eq(importRequest));
    }
}
