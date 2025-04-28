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

import static java.util.Objects.nonNull;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.qubership.atp.itf.lite.backend.model.entities.auth.BasicRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BearerRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth2RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
public class ToPostmanAuth {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ToPostmanAuthType type;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ToPostmanMapType> bearer;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ToPostmanMapType> oauth2;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ToPostmanMapType> basic;

    /**
     * Create Postman auth entity by request.
     */
    public ToPostmanAuth(@Nullable RequestAuthorization requestAuthorization) {
        if (nonNull(requestAuthorization)) {
            switch (requestAuthorization.getType()) {
                case OAUTH2:
                    this.type = ToPostmanAuthType.OAUTH2;
                    OAuth2RequestAuthorization auth = (OAuth2RequestAuthorization) requestAuthorization;
                    this.oauth2 = Arrays.asList(
                            ToPostmanMapType.addTokenToHeader(),
                            ToPostmanMapType.headerPrefix(auth.getHeaderPrefix()),
                            ToPostmanMapType.grantType(auth.getGrantType().getKey()),
                            ToPostmanMapType.authUrl(auth.getAuthUrl()),
                            ToPostmanMapType.accessTokenUrl(auth.getUrl()),
                            ToPostmanMapType.clientId(auth.getClientId()),
                            ToPostmanMapType.userName(auth.getUsername()),
                            ToPostmanMapType.scope(auth.getScope()),
                            ToPostmanMapType.state(auth.getState())
                    );
                    break;
                case BEARER:
                    this.type = ToPostmanAuthType.BEARER;
                    this.bearer = Arrays.asList(
                            ToPostmanMapType.token(
                                    ((BearerRequestAuthorization) requestAuthorization).getToken()));
                    break;
                case BASIC:
                    this.type = ToPostmanAuthType.BASIC;
                    BasicRequestAuthorization basicAuth = (BasicRequestAuthorization) requestAuthorization;
                    this.basic = Arrays.asList(
                            ToPostmanMapType.userName(basicAuth.getUsername())
                    );
                    break;
                case INHERIT_FROM_PARENT:
                    // skip because INHERIT_FROM_PARENT is the default authentication type in postman
                    // and is not added to the export file
                default:
            }
        } else {
            this.type = ToPostmanAuthType.NOAUTH;
        }
    }
}
