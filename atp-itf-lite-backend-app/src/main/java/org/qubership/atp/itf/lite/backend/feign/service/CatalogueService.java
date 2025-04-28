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

package org.qubership.atp.itf.lite.backend.feign.service;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.itf.lite.backend.enums.CacheKeys;
import org.qubership.atp.itf.lite.backend.feign.clients.CatalogueExecuteRequestFeignClient;
import org.qubership.atp.itf.lite.backend.feign.clients.CatalogueProjectFeignClient;
import org.qubership.atp.itf.lite.backend.feign.dto.CertificateDto;
import org.qubership.atp.itf.lite.backend.feign.dto.ExecuteRequestDto;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class CatalogueService {

    private final CatalogueProjectFeignClient catalogueFeignClient;
    private final CatalogueExecuteRequestFeignClient catalogueExecuteRequestFeignClient;

    /**
     * Execute request in catalogue service.
     */
    public List<UUID> execute(String authorization, ExecuteRequestDto executeRequestDto) {
        return catalogueExecuteRequestFeignClient.execute(authorization, executeRequestDto).getBody();
    }

    /**
     * Gets certificate from project.
     *
     * @param projectId project id
     * @return response with certificate or null if any exception happened
     */
    @Cacheable(value = CacheKeys.Constants.PROJECT_CERT, key = "#projectId",
            condition = "#projectId != null", sync = true)
    public ResponseEntity<CertificateDto> getCertificate(UUID projectId) {
        log.info("Takes actual certificate information from catalogue");
        try {
            return catalogueFeignClient.getCertificate(projectId);
        } catch (Exception e) {
            log.warn("Can't get project certificate. See error message for more details: {}", e.getMessage(), e);
            return null;
        }
    }

    @CacheEvict(value = CacheKeys.Constants.PROJECT_CERT, key = "#projectId")
    public void evictProjectCertificateCacheByProjectId(UUID projectId) {
        log.info("Project certificate cache for projectId = '{}' has been evicted", projectId);
    }

    public ResponseEntity<Resource> downloadFile(String fileId) {
        log.info("Download file with {} from catalogue", fileId);
        return catalogueFeignClient.downloadFile(fileId);
    }
}

