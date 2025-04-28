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
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.validation.constraints.NotNull;

import org.qubership.atp.itf.lite.backend.annotations.SerializableCheckable;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanPostmanRequestDto;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "transportType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = HttpRequestEntitySaveRequest.class,
                name = TransportType.TransportTypeNames.REST),
        @JsonSubTypes.Type(value = HttpRequestEntitySaveRequest.class,
                name = TransportType.TransportTypeNames.SOAP)
})
@SerializableCheckable
public abstract class RequestEntitySaveRequest implements ResolvableRequest, Serializable {
    private UUID id;

    @NotNull
    private String name;

    @NotNull
    private UUID projectId;

    private UUID folderId;

    @NotNull
    private TransportType transportType;

    private UUID environmentId;

    protected AuthorizationSaveRequest authorization;

    private String environmentName;

    private String preScripts;

    private String postScripts;

    private List<ContextVariable> contextVariables;

    private boolean isAutoCookieDisabled;

    private List<Cookie> cookies;
    private Date modifiedWhen;

    /**
     * Brings the requester to its normal form.
     * Removes extra spaces.
     */
    public abstract void normalize();

    public abstract void resolveTemplates(Function<String, String> evaluateFunction);

    public abstract PostmanPostmanRequestDto getPostmanRequest();

    public abstract void updateFromPostmanRequest(PostmanPostmanRequestDto postmanRequest);
}
