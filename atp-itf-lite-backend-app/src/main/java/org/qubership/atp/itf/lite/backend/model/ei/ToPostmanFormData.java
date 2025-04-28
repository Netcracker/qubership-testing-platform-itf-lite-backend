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

package org.qubership.atp.itf.lite.backend.model.ei;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
public class ToPostmanFormData extends ToPostmanMapDescriptionAndType {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String src;

    ToPostmanFormData(String key, String value, String description, String type, String src) {
        super(key, value, description, type);
        this.src = src;
    }

    public static ToPostmanFormData file(String key, String description, String src) {
        return new ToPostmanFormData(key, null, description, "file", src);
    }

    public static ToPostmanFormData text(String key, String value, String description) {
        return new ToPostmanFormData(key, value, description, "text", null);
    }
}
