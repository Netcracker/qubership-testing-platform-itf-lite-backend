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

package org.qubership.atp.itf.lite.backend.components.auth;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestAuthorizationRegistry {

    private final List<RequestAuthorizationStrategy> strategies;
    private Map<RequestAuthorizationType, RequestAuthorizationStrategy> strategiesMap;

    @PostConstruct
    public void lookupStrategies() {
        strategiesMap = strategies.stream()
                .collect(Collectors.toMap(RequestAuthorizationStrategy::getAuthorizationType, Function.identity()));
    }

    /**
     * Lookup RequestAuthorizationStrategy implementation by type.
     * @param type authorization type
     *
     * @return RequestAuthorizationStrategy implementation
     */
    public RequestAuthorizationStrategy getRequestAuthorizationStrategy(RequestAuthorizationType type) {
        if (Objects.isNull(type) || !strategiesMap.containsKey(type)) {
            String errMsg = String.format("Failed to find request authorization strategy implementation for auth type: "
                    + "%s", type);
            log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        return strategiesMap.get(type);
    }
}
