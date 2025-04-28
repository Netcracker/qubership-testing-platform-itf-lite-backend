/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.itf.lite.backend.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.qubership.atp.itf.lite.backend.exceptions.access.ItfLiteFileSecurityAccessException;
import org.qubership.atp.itf.lite.backend.exceptions.file.ItfLiteFileCreationException;
import org.qubership.atp.itf.lite.backend.exceptions.internal.ItfLiteCatalogFileDownloadException;
import org.qubership.atp.itf.lite.backend.exceptions.internal.ItfLiteIllegalFileInfoDownloadArgumentsException;
import org.qubership.atp.itf.lite.backend.feign.dto.CertificateDto;
import org.qubership.atp.itf.lite.backend.feign.dto.FileInfoDto;
import org.qubership.atp.itf.lite.backend.feign.service.CatalogueService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CertificateService {

    public static final String CERTIFICATE_FOLDER = "ProjectCertificates" + File.separator + "%s"  + File.separator;
    private final CatalogueService catalogueFeignService;

    public CertificateService(CatalogueService catalogueFeignService) {
        this.catalogueFeignService = catalogueFeignService;
    }

    /**
     * Get certificate for catalogue or from cache.
     *
     * @param projectId projectId
     * @return CertificateDto instance
     */
    public CertificateDto getCertificate(UUID projectId) {
        ResponseEntity<CertificateDto> certificateDtoResponseEntity = catalogueFeignService.getCertificate(projectId);
        if (certificateDtoResponseEntity != null) {
            return certificateDtoResponseEntity.getBody();
        }
        return new CertificateDto();
    }

    /**
     * Get certificate verification file.
     *
     * @param projectId projectId
     * @return File instance.
     */
    public File getCertificateVerificationFile(UUID projectId) {
        FileInfoDto trustStoreFileInfo = getCertificate(projectId).getTrustStoreFileInfo();

        return getFileOrDownload(projectId, trustStoreFileInfo);
    }

    /**
     * Get client certificate file.
     *
     * @param projectId projectId
     * @return File instance.
     */
    public File getClientCertificateFile(UUID projectId) {
        FileInfoDto keyStoreFileInfo = getCertificate(projectId).getKeyStoreFileInfo();

        return getFileOrDownload(projectId, keyStoreFileInfo);
    }

    private File getFileOrDownload(UUID projectId, FileInfoDto fileInfo) {
        if (fileInfo == null || fileInfo.getName() == null || fileInfo.getId() == null) {
            log.error("Found illegal downloaded file info data: {}", fileInfo);
            throw new ItfLiteIllegalFileInfoDownloadArgumentsException();
        }

        Path rootDir = Paths.get(String.format(CERTIFICATE_FOLDER, projectId)).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            log.error("Cannot create certificate folder", e);
            throw new ItfLiteFileCreationException("Cannot create certificate folder");
        }

        String sanitizedFileName = Paths.get(fileInfo.getName()).getFileName().toString();
        sanitizedFileName = org.qubership.atp.itf.lite.backend.utils.FileUtils.sanitizeFileName(sanitizedFileName);

        Path certPath = rootDir.resolve(sanitizedFileName).normalize();
        if (!certPath.startsWith(rootDir)) {
            log.error("Detected path traversal attempt: {}", sanitizedFileName);
            throw new ItfLiteFileSecurityAccessException("Detected path traversal attempt: " + sanitizedFileName);
        }

        File certFile = certPath.toFile();

        if (!certFile.exists()) {
            log.info("Certificate file '{}' not present on pod storage, load from catalogue", sanitizedFileName);
            String fileId = fileInfo.getId();
            try (InputStream stream = catalogueFeignService.downloadFile(fileId).getBody().getInputStream()) {
                FileUtils.copyInputStreamToFile(stream, certFile);
            } catch (Exception e) {
                log.error("Failed to download file from catalogue. Project: '{}', file id: '{}'",
                        projectId, fileId, e);
                throw new ItfLiteCatalogFileDownloadException();
            }
        } else {
            log.info("Certificate file '{}' present on pod storage", sanitizedFileName);
        }

        return certFile;
    }
}
