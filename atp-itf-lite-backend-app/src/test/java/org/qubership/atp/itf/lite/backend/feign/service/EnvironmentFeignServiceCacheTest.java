package org.qubership.atp.itf.lite.backend.feign.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.configuration.FeignServiceCacheTestConfiguration;
import org.qubership.atp.itf.lite.backend.configuration.RamServiceConfiguration;
import org.qubership.atp.itf.lite.backend.configuration.cache.ItfLiteNoHazelCastCacheConfiguration;
import org.qubership.atp.itf.lite.backend.feign.clients.EnvironmentsFeignClient;
import org.qubership.atp.itf.lite.backend.feign.clients.RamTestPlansFeignClient;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.api.response.environments.System;
import org.qubership.atp.itf.lite.backend.service.GridFsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {
        ItfLiteNoHazelCastCacheConfiguration.class,
        FeignServiceCacheTestConfiguration.class,
        RamServiceConfiguration.class},
        webEnvironment = NONE, properties = {"spring.cloud.consul.config.enabled=false"})
public class EnvironmentFeignServiceCacheTest {

    @Autowired
    EnvironmentFeignService environmentFeignService;
    @MockBean
    CatalogueService catalogueFeignService;
    @MockBean
    ItfFeignService itfFeignService;
    @MockBean
    EnvironmentsFeignClient environmentsFeignClient;
    @MockBean
    RamTestPlansFeignClient ramTestPlansFeignClient;
    @MockBean
    JsScriptEngineService jsScriptEngineService;
    @MockBean
    RamService ramService;
    @MockBean
    GridFsService gridFsService;

    @Test
    @ResourceLock(Resources.GLOBAL)
    public void getEnvironmentSystemsTwoTimesTest_cacheEnabled_shouldExecutedOneTime() {
        // given
        final UUID environmentId = UUID.randomUUID();
        final UUID systemId1 = UUID.randomUUID();
        final String systemName1 = "QA";
        System system1 = EntitiesGenerator.generateEnvironmentSystem(systemId1, systemName1);
        final UUID systemId2 = UUID.randomUUID();
        final String systemName2 = "ABC";
        System system2 = EntitiesGenerator.generateEnvironmentSystem(systemId2, systemName2);

        // when
        when(environmentsFeignClient.getEnvironmentSystems(any()))
                .thenReturn(Collections.singletonList(system1))
                .thenReturn(Collections.singletonList(system2));
        List<System> systemsResponseEntity1 =
                environmentFeignService.getEnvironmentSystems(environmentId);
        List<System> systemsResponseEntity2 =
                environmentFeignService.getEnvironmentSystems(environmentId);
        //then
        assertEquals(1, systemsResponseEntity1.size());
        assertEquals(1, systemsResponseEntity2.size());
        assertEquals(systemsResponseEntity1.get(0), systemsResponseEntity2.get(0));
        assertEquals(system1.getId(), systemsResponseEntity1.get(0).getId());
        assertEquals(system1.getName(), systemsResponseEntity1.get(0).getName());
        verify(environmentsFeignClient, times(1)).getEnvironmentSystems(any());
    }

    @Test
    public void evictEnvironmentSystemsCacheByEnvironmentIdTest_getEnvironmentSystemsTwoTimes_shouldExecutedTwoTimes() {
        // given
        final UUID environmentId = UUID.randomUUID();
        final UUID systemId1 = UUID.randomUUID();
        final String systemName1 = "QA";
        System system1 = EntitiesGenerator.generateEnvironmentSystem(systemId1, systemName1);
        final UUID systemId2 = UUID.randomUUID();
        final String systemName2 = "ABC";
        System system2 = EntitiesGenerator.generateEnvironmentSystem(systemId2, systemName2);

        // when
        when(environmentsFeignClient.getEnvironmentSystems(any()))
                .thenReturn(Collections.singletonList(system1))
                .thenReturn(Collections.singletonList(system2));
        List<System> systemsResponseEntity1 =
                environmentFeignService.getEnvironmentSystems(environmentId);
        environmentFeignService.evictEnvironmentSystemsCacheByEnvironmentId(environmentId);
        List<System> systemsResponseEntity2 =
                environmentFeignService.getEnvironmentSystems(environmentId);
        //then
        assertEquals(1, systemsResponseEntity1.size());
        assertEquals(1, systemsResponseEntity2.size());
        assertEquals(system1.getId(), systemsResponseEntity1.get(0).getId());
        assertEquals(system1.getName(), systemsResponseEntity1.get(0).getName());
        assertEquals(system2.getId(), systemsResponseEntity2.get(0).getId());
        assertEquals(system2.getName(), systemsResponseEntity2.get(0).getName());
        verify(environmentsFeignClient, times(2)).getEnvironmentSystems(any());
    }
}
