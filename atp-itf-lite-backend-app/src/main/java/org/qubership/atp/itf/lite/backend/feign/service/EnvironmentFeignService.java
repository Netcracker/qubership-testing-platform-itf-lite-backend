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
import org.qubership.atp.itf.lite.backend.feign.clients.EnvironmentsFeignClient;
import org.qubership.atp.itf.lite.backend.model.api.response.environments.System;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnvironmentFeignService {

    private final EnvironmentsFeignClient environmentsFeignClient;

    /**
     * Gets list of environment's systems.
     *
     * @param environmentId environment id
     * @return response with list of environment's systems
     */
    @Cacheable(value = CacheKeys.Constants.ENVIRONMENT_SYSTEMS, key = "#environmentId",
            condition = "#environmentId != null", sync = true)
    public List<System> getEnvironmentSystems(UUID environmentId) {
        log.info("Takes actual systems from environment service");
        return environmentsFeignClient.getEnvironmentSystems(environmentId);
    }

    @CacheEvict(value = CacheKeys.Constants.ENVIRONMENT_SYSTEMS, key = "#environmentId")
    public void evictEnvironmentSystemsCacheByEnvironmentId(UUID environmentId) {
        log.info("Environment cache for environmentId = '{}' has been evicted", environmentId);
    }
}

