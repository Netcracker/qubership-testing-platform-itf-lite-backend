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

import org.qubership.atp.itf.lite.backend.enums.auth.OAuth1AddDataType;
import org.qubership.atp.itf.lite.backend.enums.auth.OAuth1SignatureMethod;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth1RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OAuth1AuthorizationSaveRequest extends AuthorizationSaveRequest {

    @NotNull
    private String url;

    @NotNull
    private String httpMethod;

    @NotNull
    private OAuth1SignatureMethod signatureMethod;

    @NotNull
    private String consumerKey;

    @NotNull
    private String consumerSecret;

    private String accessToken;

    private String tokenSecret;

    // default value is REQUEST_HEADERS
    private OAuth1AddDataType addDataType = OAuth1AddDataType.REQUEST_HEADERS;

    @Override
    public Class<? extends RequestAuthorization> getAuthEntityType() {
        return OAuth1RequestAuthorization.class;
    }

    @Override
    public void resolveTemplates(Function<String, String> evaluateFunction) {
        consumerKey = evaluateFunction.apply(consumerKey);
        consumerSecret = evaluateFunction.apply(consumerSecret);
        accessToken = evaluateFunction.apply(accessToken);
        tokenSecret = evaluateFunction.apply(tokenSecret);
    }
}
