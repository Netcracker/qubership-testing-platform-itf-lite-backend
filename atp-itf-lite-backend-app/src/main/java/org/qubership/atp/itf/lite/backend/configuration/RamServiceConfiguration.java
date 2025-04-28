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

package org.qubership.atp.itf.lite.backend.configuration;

import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.REDIRECT_PATH;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RamServiceConfiguration {
    @Value("${atp.catalogue.frontend.url:}")
    private String catalogueUrl;

    @Value("${feign.atp.ram.route:api/atp-ram/v1}")
    private String ramPrefix;
    @Value("${atp.service.internal:true}")
    private boolean isInternalGateWayEnabled;

    /**
     * Catalogue link to download with REDIRECT_PATH and ram prefix.
     */
    @Bean("fileDownloadLink")
    public String fileDownloadLink() {
        String fileDownloadLink = "";
        if (catalogueUrl != null && !catalogueUrl.isEmpty()) {
            log.debug("InternalGateway is Enabled & setting the file download URL prefix");
            try {
                fileDownloadLink =
                        new URI(catalogueUrl + REDIRECT_PATH + ramPrefix).normalize().toString();
            } catch (URISyntaxException e) {
                log.error("Problem in download file URL Prefix Formation. {}", e.getMessage());
            }
            log.info("File download URL Prefix set to {}", fileDownloadLink);
        } else {
            log.error("Invalid atp-catalog URL. Please set environment variable ATP_CATALOGUE_URL");
        }
        return fileDownloadLink;
    }
}
