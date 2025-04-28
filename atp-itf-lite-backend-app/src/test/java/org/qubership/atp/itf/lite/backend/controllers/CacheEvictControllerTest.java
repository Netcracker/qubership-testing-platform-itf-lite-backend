package org.qubership.atp.itf.lite.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.CERTIFICATE_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.ENVIRONMENT_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.itf.lite.backend.feign.service.CatalogueService;
import org.qubership.atp.itf.lite.backend.feign.service.EnvironmentFeignService;
import org.qubership.atp.itf.lite.backend.handlers.MethodArgumentExceptionHandler;
import org.qubership.atp.itf.lite.backend.model.api.ApiPath;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {CacheEvictController.class, MethodArgumentExceptionHandler.class})
@Isolated
public class CacheEvictControllerTest extends AbstractControllerTest {

    static final String FULL_REQUESTS_PATH = ApiPath.SERVICE_API_V1_PATH + ApiPath.CACHE_EVICT_PATH;

    @MockBean
    private CatalogueService catalogueFeignService;
    @MockBean
    private EnvironmentFeignService environmentFeignService;

    @Test
    public void evictProjectCertificateCacheByProjectIdTest_shouldSuccessfullyEvicted() throws Exception {
        // given
        UUID projectId = UUID.randomUUID();
        // when
        this.mockMvc.perform(post(FULL_REQUESTS_PATH + CERTIFICATE_PATH)
                        .queryParam("projectId", projectId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
        // then
        verify(catalogueFeignService).evictProjectCertificateCacheByProjectId(any());
    }

    @Test
    public void evictEnvironmentSystemsCacheByEnvironmentIdTest_shouldSuccessfullyEvicted() throws Exception {
        // given
        UUID environmentId = UUID.randomUUID();
        // when
        this.mockMvc.perform(post(FULL_REQUESTS_PATH + ENVIRONMENT_PATH)
                        .queryParam("environmentId", environmentId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
        // then
        verify(environmentFeignService).evictEnvironmentSystemsCacheByEnvironmentId(any());
    }
}
