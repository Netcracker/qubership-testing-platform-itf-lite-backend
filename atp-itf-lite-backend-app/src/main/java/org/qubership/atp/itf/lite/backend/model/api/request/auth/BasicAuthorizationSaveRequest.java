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

import org.qubership.atp.itf.lite.backend.model.entities.auth.BasicRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BasicAuthorizationSaveRequest extends AuthorizationSaveRequest {

    @NotNull
    private String username;

    @NotNull
    private String password;

    @Override
    public Class<? extends RequestAuthorization> getAuthEntityType() {
        return BasicRequestAuthorization.class;
    }

    @Override
    public void resolveTemplates(Function<String, String> evaluateFunction) {
        this.username = evaluateFunction.apply(this.username);
        this.password = evaluateFunction.apply(this.password);
    }
}
