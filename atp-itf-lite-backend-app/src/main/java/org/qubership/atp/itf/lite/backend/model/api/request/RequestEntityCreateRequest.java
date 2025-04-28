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

import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.qubership.atp.itf.lite.backend.enums.TransportType;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestEntityCreateRequest {
    @NotNull
    private String name;

    @NotNull
    private UUID projectId;

    private UUID folderId;

    @NotNull
    private TransportType transportType;
    private boolean isAutoCookieDisabled;

    /**
     * Constructor RequestEntityCreateRequest.
     */
    public RequestEntityCreateRequest(String name, UUID projectId, UUID folderId, TransportType transportType) {
        this(name, projectId, folderId, transportType, false);
    }

    @JsonProperty(value = "isAutoCookieDisabled")
    public boolean isAutoCookieDisabled() {
        return isAutoCookieDisabled;
    }

    public void setAutoCookieDisabled(boolean isAutoCookieDisabled) {
        this.isAutoCookieDisabled = isAutoCookieDisabled;
    }
}
