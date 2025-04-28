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

import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractNamedEntity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Settings extends AbstractNamedEntity {

    private boolean isAutoCookieDisabled;
    private boolean disableSslCertificateVerification;
    private boolean disableSslClientCertificate;
    private boolean disableFollowingRedirect;
    private boolean disableAutoEncoding;
    private AuthorizationSaveRequest authorization;

    @JsonProperty(value = "isAutoCookieDisabled")
    public boolean isAutoCookieDisabled() {
        return isAutoCookieDisabled;
    }

    public void setAutoCookieDisabled(boolean isAutoCookieDisabled) {
        this.isAutoCookieDisabled = isAutoCookieDisabled;
    }

}
