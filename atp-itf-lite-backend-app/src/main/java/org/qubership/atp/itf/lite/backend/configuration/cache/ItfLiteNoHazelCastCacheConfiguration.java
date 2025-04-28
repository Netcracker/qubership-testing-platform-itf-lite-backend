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

package org.qubership.atp.itf.lite.backend.configuration.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.qubership.atp.itf.lite.backend.enums.CacheKeys;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@ConditionalOnProperty(
        value = "spring.cache.hazelcast.client.enable",
        havingValue = "false"
)
public class ItfLiteNoHazelCastCacheConfiguration {

    /**
     * Returns CacheManager Object.
     *
     * @return CacheManager Object.
     */
    @Bean
    public CacheManager cacheManager() {
        List<Cache> caches = new ArrayList<>();
        Arrays.stream(CacheKeys.values()).forEach(cacheKey ->
                caches.add(new CaffeineCache(cacheKey.getKey(),
                        Caffeine.newBuilder().expireAfterWrite(cacheKey.getTimeToLive(),
                                cacheKey.getTimeUnit()).recordStats().maximumSize(100).build(),
                        true)));
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(caches);
        return cacheManager;
    }
}
