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

package org.qubership.atp.itf.lite.backend.model.api.request;

import java.io.Serializable;

import org.qubership.atp.itf.lite.backend.annotations.SerializableCheckable;
import org.qubership.atp.itf.lite.backend.enums.ContextVariableType;
import org.qubership.atp.itf.lite.backend.feign.dto.ContextVariableDto;
import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@SerializableCheckable
public class ContextVariable implements Serializable {
    private String key;
    private Object value;
    private final ContextVariableType contextVariableType;

    public ContextVariable() {
        this.contextVariableType = ContextVariableType.GLOBAL;
    }

    /**
     * Constructor.
     * @param contextVariableDto context variables dto
     */
    public ContextVariable(ContextVariableDto contextVariableDto) {
        this.key = contextVariableDto.getName();
        this.value = StringUtils.isEmpty(contextVariableDto.getAfterValue())
                ? contextVariableDto.getBeforeValue()
                : contextVariableDto.getAfterValue();
        this.contextVariableType = ContextVariableType.fromContextVariableKey(contextVariableDto.getName());
    }
}
