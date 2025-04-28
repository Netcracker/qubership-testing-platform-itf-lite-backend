package org.qubership.atp.itf.lite.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.feign.dto.CertificateDto;
import org.qubership.atp.itf.lite.backend.exceptions.internal.ItfLiteCatalogFileDownloadException;
import org.qubership.atp.itf.lite.backend.exceptions.internal.ItfLiteIllegalFileInfoDownloadArgumentsException;
import org.qubership.atp.itf.lite.backend.feign.clients.CatalogueExecuteRequestFeignClient;
import org.qubership.atp.itf.lite.backend.feign.clients.CatalogueProjectFeignClient;
import org.qubership.atp.itf.lite.backend.feign.service.CatalogueService;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.utils.FileUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@Isolated
public class CertificateServiceTest {

    private static CatalogueService catalogueFeignService;
    private static CertificateService certificateService;
    private UUID projectId;
    private CatalogueProjectFeignClient catalogueProjectFeignClient;
    private CatalogueExecuteRequestFeignClient catalogueExecuteRequestFeignClient;
    private File folder;

    @BeforeEach
    public void setUp() {
        projectId = UUID.randomUUID();
        catalogueProjectFeignClient = mock(CatalogueProjectFeignClient.class);
        catalogueExecuteRequestFeignClient = mock(CatalogueExecuteRequestFeignClient.class);
        catalogueFeignService = new CatalogueService(catalogueProjectFeignClient, catalogueExecuteRequestFeignClient);
        certificateService = new CertificateService(catalogueFeignService);

        folder = new File(String.format(CertificateService.CERTIFICATE_FOLDER, projectId));
    }

    @AfterEach
    public void after() throws IOException {
        FileUtils.deleteDirectoryRecursively(folder.toPath());

        // cleanup caches
        catalogueFeignService.evictProjectCertificateCacheByProjectId(projectId);
    }

    @Test
    public void getCertificate_whenPresent() {
        //given
        CertificateDto cert = EntitiesGenerator.generateRandomCertificate();
        ResponseEntity resp = new ResponseEntity(cert, HttpStatus.OK);
        //when
        Mockito.when(catalogueProjectFeignClient.getCertificate(any())).thenReturn(resp);
        CertificateDto result = certificateService.getCertificate(UUID.randomUUID());
        //then
        assertEquals(cert.getProtocol(), result.getProtocol());
    }

    @Test
    public void getCertificateVerificationFile_shouldReturnCertificateFile() throws IOException {
        //given
        ResponseEntity certificateInfoEntity = new ResponseEntity(EntitiesGenerator.generateRandomCertificate(), HttpStatus.OK);
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("identity.p12");
        ResponseEntity fileInfoEntity = new ResponseEntity(resource, HttpStatus.OK);

        //when
        Mockito.when(catalogueProjectFeignClient.getCertificate(any())).thenReturn(certificateInfoEntity);
        Mockito.when(catalogueProjectFeignClient.downloadFile(anyString())).thenReturn(fileInfoEntity);
        File actualVerificationFile = certificateService.getCertificateVerificationFile(projectId);

        //then
        assertTrue(actualVerificationFile.exists());
        assertEquals(folder.getAbsolutePath(), actualVerificationFile.getParentFile().getAbsolutePath());
        assertEquals("identity.p12", actualVerificationFile.getName());
    }

    @Test
    public void certificateVerificationFile_fileInfoIsNull_returnRequestExecutionFailedException() {
        //given
        ResponseEntity entity1 = new ResponseEntity(new CertificateDto(), HttpStatus.OK);
        //when
        Mockito.when(catalogueFeignService.getCertificate(any())).thenReturn(entity1);
        ItfLiteIllegalFileInfoDownloadArgumentsException ex = assertThrows(
                ItfLiteIllegalFileInfoDownloadArgumentsException.class,
                () -> certificateService.getCertificateVerificationFile(projectId));
        //then
        String expectedErrorMessage = ItfLiteIllegalFileInfoDownloadArgumentsException.DEFAULT_MESSAGE;
        assertEquals(expectedErrorMessage, ex.getMessage());
    }

    @Test
    public void certificateVerificationFile_notPresent_returnRequestExecutionFailedException() {
        //given
        ResponseEntity entity1 = new ResponseEntity(EntitiesGenerator.generateRandomCertificate(), HttpStatus.OK);
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:resource.txt");
        ResponseEntity entity2 = new ResponseEntity(resource, HttpStatus.OK);
        //when
        Mockito.when(catalogueFeignService.getCertificate(any())).thenReturn(entity1);
        Mockito.when(catalogueFeignService.downloadFile(anyString())).thenReturn(entity2);
        ItfLiteCatalogFileDownloadException exception = assertThrows(ItfLiteCatalogFileDownloadException.class,
                () -> certificateService.getCertificateVerificationFile(projectId));
        //then
        String expectedErrorMessage = ItfLiteCatalogFileDownloadException.DEFAULT_MESSAGE;
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    @Test
    public void getCertificateVerificationFileTest_fileNameHasVulnerabilityInName_returnCertificateFileNameException() {
        //given
        CertificateDto certificateDto = EntitiesGenerator.generateRandomCertificate();
        String fileName = "/../../../../../identity.p12";
        certificateDto.getTrustStoreFileInfo().setName(fileName);
        ResponseEntity certificateInfoEntity = new ResponseEntity(certificateDto, HttpStatus.OK);
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("identity.p12");
        ResponseEntity fileInfoEntity = new ResponseEntity(resource, HttpStatus.OK);
        //when
        Mockito.when(catalogueProjectFeignClient.getCertificate(any())).thenReturn(certificateInfoEntity);
        Mockito.when(catalogueProjectFeignClient.downloadFile(anyString())).thenReturn(fileInfoEntity);

        File actualVerificationFile = certificateService.getCertificateVerificationFile(projectId);

        //then
        assertTrue(actualVerificationFile.exists());
        assertEquals(folder.getAbsolutePath(), actualVerificationFile.getParentFile().getAbsolutePath());
        assertEquals("identity.p12", actualVerificationFile.getName());
    }
}
