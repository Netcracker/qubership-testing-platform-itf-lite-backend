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

package org.qubership.atp.itf.lite.backend.feign.clients;

import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.itf.lite.backend.feign.dto.CertificateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name = "${feign.atp.catalogue.name}", url = "${feign.atp.catalogue.url}",
        path = "${feign.atp.catalogue.route}", configuration = FeignConfiguration.class)
public interface CatalogueProjectFeignClient {

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/catalog/api/v1/projects/certificate/{uuid}",
            produces = { "application/json" }
    )
    ResponseEntity<CertificateDto> getCertificate(@PathVariable("uuid") UUID uuid);

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/catalog/api/v1/projects/downloadFile/{fileId}",
            produces = { "application/octet-stream" }
    )
    ResponseEntity<Resource> downloadFile(@PathVariable("fileId") String fileId);
}
