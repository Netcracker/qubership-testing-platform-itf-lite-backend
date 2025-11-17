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

package org.qubership.atp.itf.lite.backend.service.kafka.listeners;

import static org.qubership.atp.itf.lite.backend.service.CertificateService.CERTIFICATE_FOLDER;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.qubership.atp.integration.configuration.mdc.MdcField;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.itf.lite.backend.configuration.KafkaConfiguration;
import org.qubership.atp.itf.lite.backend.enums.CacheKeys;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ProjectEvent;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProjectEventKafkaListener {

    /**
     * Listen Project event from catalogue.
     *
     * @param event event.
     */
    @Caching(evict = {
            @CacheEvict(value = CacheKeys.Constants.AUTH_PROJECTS_KEY, allEntries = true),
            @CacheEvict(value = CacheKeys.Constants.PROJECT_CERT, key = "#event.projectId")
    })
    @KafkaListener(
            id = "${kafka.catalog.notification.group}",
            groupId = "${kafka.catalog.notification.group}",
            topics = "${kafka.catalog.notification.topic:catalog_notification_topic}",
            containerFactory = KafkaConfiguration.CATALOG_PROJECT_EVENT_CONTAINER_FACTORY,
            autoStartup = "true"
    )
    public void listen(ProjectEvent event) {
        MDC.clear();
        MdcUtils.put(MdcField.PROJECT_ID.toString(), event.getProjectId());
        switch (event.getType()) {
            case CREATE:
            case UPDATE:
            case DELETE: {
                log.info("Received event from catalogue: {}", event);
                // clear files with certificates for project
                File folder = new File(String.format(CERTIFICATE_FOLDER, event.getProjectId()));
                try {
                    FileUtils.cleanDirectory(folder);
                } catch (Exception e) {
                    log.error("Can't clean folder with certificates {} on event from kafka: {}",
                            folder.getAbsoluteFile(), e.getMessage());
                }
                break;
            }
            default: {
                log.error("Unknown type of event from catalogue: {}", event.getType());
            }
        }
    }
}

