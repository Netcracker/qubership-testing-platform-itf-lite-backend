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

package org.qubership.atp.itf.lite.backend.enums;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ContextVariableType {

    GLOBAL("Global", ContextScope.GLOBALS),
    COLLECTION("Collection", ContextScope.COLLECTION),
    DATA("Data", ContextScope.DATA),
    ENVIRONMENT("Environment", ContextScope.ENVIRONMENT),
    LOCAL("Local", ContextScope.LOCAL_VARIABLES);

    private final String name;
    private final ContextScope contextScope;

    @JsonValue
    public String getName() {
        return name;
    }

    /**
     * Get contextVariableType by value.
     * @param value value
     * @return ContextVariableType
     */
    @JsonCreator
    public static ContextVariableType fromValue(String value) {
        for (ContextVariableType contextVariableType : ContextVariableType.values()) {
            if (contextVariableType.getName().equals(value)) {
                return contextVariableType;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    /**
     * Get contextVariableType by key.
     * @param key key
     * @return ContextVariableType
     */
    public static ContextVariableType fromContextVariableKey(String key) {
        if (!StringUtils.isEmpty(key)) {
            for (ContextVariableType contextVariableType : values()) {
                String prefix = contextVariableType.getContextScope().getPrefix();
                if (!StringUtils.isEmpty(prefix) && key.contains(prefix)) {
                    return contextVariableType;
                }
            }
        }
        return LOCAL;
    }
}
