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

import java.util.UUID;

import org.qubership.atp.itf.lite.backend.configuration.KafkaConfiguration;
import org.qubership.atp.itf.lite.backend.feign.service.EnvironmentFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EnvironmentKafkaListener {

    private static final String KAFKA_ENVIRONMENT_EVENT_RESPONSE_LISTENER_ID =
            "kafkaEnvironmentEventResponseListenerId";

    private final EnvironmentFeignService environmentFeignService;

    @Autowired
    public EnvironmentKafkaListener(EnvironmentFeignService environmentFeignService) {
        this.environmentFeignService = environmentFeignService;
    }

    @KafkaListener(
            groupId = KAFKA_ENVIRONMENT_EVENT_RESPONSE_LISTENER_ID
                    + "_#{T(org.qubership.atp.itf.lite.backend.utils.PodNameUtils).getServicePodName()}",
            topics = "${kafka.environment.notification.topic}",
            containerFactory = KafkaConfiguration.ENVIRONMENT_KAFKA_CONTAINER_FACTORY_BEAN_NAME
    )
    public void listenEnvironmentNotificationEvent(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) UUID environmentId) {
        environmentFeignService.evictEnvironmentSystemsCacheByEnvironmentId(environmentId);
    }
}
