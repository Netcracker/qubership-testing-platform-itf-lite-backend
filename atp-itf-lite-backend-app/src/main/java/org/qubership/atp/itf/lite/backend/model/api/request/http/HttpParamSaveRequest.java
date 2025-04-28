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

package org.qubership.atp.itf.lite.backend.model.api.request.http;

import java.util.UUID;

import org.qubership.atp.itf.lite.backend.model.api.Parameter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HttpParamSaveRequest implements Parameter {
    private UUID id;
    private String key;
    private String value;
    private String description;
    private boolean disabled;
    private boolean generated;

    /**
     * HttpParamSaveRequest constructor.
     */
    public HttpParamSaveRequest(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }

    /**
     * Constructor without id.
     */
    public HttpParamSaveRequest(String key, String value, String description, boolean disabled) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.disabled = disabled;
    }
}
