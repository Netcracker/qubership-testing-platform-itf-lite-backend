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

package org.qubership.atp.itf.lite.backend.model.api.request.auth;

import static org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType.AuthorizationTypeNames.BASIC;
import static org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType.AuthorizationTypeNames.BEARER;
import static org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType.AuthorizationTypeNames.INHERIT_FROM_PARENT;
import static org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType.AuthorizationTypeNames.OAUTH1;
import static org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType.AuthorizationTypeNames.OAUTH2;

import java.io.Serializable;
import java.util.function.Function;

import javax.validation.constraints.NotNull;

import org.qubership.atp.itf.lite.backend.annotations.SerializableCheckable;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.model.api.request.ResolvableRequest;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OAuth1AuthorizationSaveRequest.class, name = OAUTH1),
        @JsonSubTypes.Type(value = OAuth2AuthorizationSaveRequest.class, name = OAUTH2),
        @JsonSubTypes.Type(value = BearerAuthorizationSaveRequest.class, name = BEARER),
        @JsonSubTypes.Type(value = BasicAuthorizationSaveRequest.class, name = BASIC),
        @JsonSubTypes.Type(value = InheritFromParentAuthorizationSaveRequest.class, name = INHERIT_FROM_PARENT)
})
@SerializableCheckable
public abstract class AuthorizationSaveRequest implements ResolvableRequest, Serializable {

    @NotNull
    protected RequestAuthorizationType type;

    /**
     * Need for modelMapper and all fields runtime mapping to RequestAuthorization sub type.
     */
    @JsonIgnore
    public abstract Class<? extends RequestAuthorization> getAuthEntityType();

    public abstract void resolveTemplates(Function<String, String> evaluateFunction);
}
