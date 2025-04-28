package org.qubership.atp.itf.lite.backend.feign.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.feign.dto.CertificateDto;
import org.qubership.atp.itf.lite.backend.configuration.FeignServiceCacheTestConfiguration;
import org.qubership.atp.itf.lite.backend.configuration.RamServiceConfiguration;
import org.qubership.atp.itf.lite.backend.configuration.cache.ItfLiteNoHazelCastCacheConfiguration;
import org.qubership.atp.itf.lite.backend.feign.clients.CatalogueExecuteRequestFeignClient;
import org.qubership.atp.itf.lite.backend.feign.clients.CatalogueProjectFeignClient;
import org.qubership.atp.itf.lite.backend.feign.clients.RamTestPlansFeignClient;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.service.GridFsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {
        ItfLiteNoHazelCastCacheConfiguration.class,
        FeignServiceCacheTestConfiguration.class,
        RamServiceConfiguration.class},
        webEnvironment = NONE, properties = {"spring.cloud.consul.config.enabled=false"})
public class CatalogueFeignServiceCacheTest {

    @Autowired
    CatalogueService catalogueFeignService;
    @MockBean
    EnvironmentFeignService environmentFeignService;
    @MockBean
    ItfFeignService itfFeignService;
    @MockBean
    CatalogueProjectFeignClient catalogueFeignClient;
    @MockBean
    CatalogueExecuteRequestFeignClient catalogueExecuteRequestFeignClient;
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
    public void getCertificateTwoTimesTest_cacheEnabled_shouldExecutedOneTime() {
        // given
        final UUID projectId = UUID.randomUUID();
        final String phrase1 = "phrase1";
        final String phrase2 = "phrase2";

        // when
        when(catalogueFeignClient.getCertificate(eq(projectId)))
                .thenReturn(ResponseEntity.ok(EntitiesGenerator.generateRandomCertificateByPhrase(phrase1)))
                .thenReturn(ResponseEntity.ok(EntitiesGenerator.generateRandomCertificateByPhrase(phrase2)));
        ResponseEntity<CertificateDto> certificateDtoResponseEntity1 = catalogueFeignService.getCertificate(projectId);
        ResponseEntity<CertificateDto> certificateDtoResponseEntity2 = catalogueFeignService.getCertificate(projectId);
        //then
        assertEquals(certificateDtoResponseEntity1, certificateDtoResponseEntity2);
        assertNotNull(certificateDtoResponseEntity1.getBody());
        assertEquals(phrase1, certificateDtoResponseEntity1.getBody().getTrustStorePassphrase());
        verify(catalogueFeignClient, times(1)).getCertificate(any());
    }

    @Test
    public void evictProjectCertificateCacheByProjectIdTest_getCertificateTwoTimes_shouldExecutedTwoTimes() {
        // given
        final UUID projectId = UUID.randomUUID();
        final String phrase1 = "phrase1";
        final String phrase2 = "phrase2";

        // when
        when(catalogueFeignClient.getCertificate(eq(projectId)))
                .thenReturn(ResponseEntity.ok(EntitiesGenerator.generateRandomCertificateByPhrase(phrase1)))
                .thenReturn(ResponseEntity.ok(EntitiesGenerator.generateRandomCertificateByPhrase(phrase2)));
        ResponseEntity<CertificateDto> certificateDtoResponseEntity1 = catalogueFeignService.getCertificate(projectId);
        catalogueFeignService.evictProjectCertificateCacheByProjectId(projectId);
        ResponseEntity<CertificateDto> certificateDtoResponseEntity2 = catalogueFeignService.getCertificate(projectId);
        //then
        assertNotNull(certificateDtoResponseEntity1.getBody());
        assertEquals(phrase1, certificateDtoResponseEntity1.getBody().getTrustStorePassphrase());
        assertNotNull(certificateDtoResponseEntity2.getBody());
        assertEquals(phrase2, certificateDtoResponseEntity2.getBody().getTrustStorePassphrase());
        verify(catalogueFeignClient, times(2)).getCertificate(any());
    }
}
