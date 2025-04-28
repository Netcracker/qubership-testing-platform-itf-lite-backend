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

import java.util.function.Function;

import javax.validation.constraints.NotNull;

import org.qubership.atp.itf.lite.backend.enums.auth.OAuth2GrantType;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth2RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OAuth2AuthorizationSaveRequest extends AuthorizationSaveRequest {

    private String headerPrefix;

    @NotNull
    private OAuth2GrantType grantType;

    private String authUrl;

    @NotNull
    private String url;

    @NotNull
    private String clientId;

    @NotNull
    private String clientSecret;

    @NotNull
    private String username;

    @NotNull
    private String password;

    private String scope;

    private String state;

    private String token;

    @Override
    public Class<? extends RequestAuthorization> getAuthEntityType() {
        return OAuth2RequestAuthorization.class;
    }

    @Override
    public void resolveTemplates(Function<String, String> evaluateFunction) {
        url = evaluateFunction.apply(url);
        clientId = evaluateFunction.apply(clientId);
        clientSecret = evaluateFunction.apply(clientSecret);
        username = evaluateFunction.apply(username);
        password = evaluateFunction.apply(password);
        scope = scope == null ? null : evaluateFunction.apply(scope);
    }
}
