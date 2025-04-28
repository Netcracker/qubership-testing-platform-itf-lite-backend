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

package org.qubership.atp.itf.lite.backend.model;

import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class AuthorizationStrategyResponse {

    private String safeAuthorizationToken;
    private String unsafeAuthorizationToken;
    private Map<String, String> authorizationParams;

    public AuthorizationStrategyResponse(String safeAuthorizationToken, String unsafeAuthorizationToken) {
        this.safeAuthorizationToken = safeAuthorizationToken;
        this.unsafeAuthorizationToken = unsafeAuthorizationToken;
    }

    /**
     * Constructor for the case when we need to return only authorization parameters.
     *
     * @param authorizationParams authorization parameters
     */
    public AuthorizationStrategyResponse(String safeAuthorizationToken,
                                         String unsafeAuthorizationToken,
                                         Map<String, String> authorizationParams) {
        this.safeAuthorizationToken = safeAuthorizationToken;
        this.unsafeAuthorizationToken = unsafeAuthorizationToken;
        this.authorizationParams = authorizationParams;
    }

    public AuthorizationStrategyResponse(Map<String, String> authorizationParams) {
        this.authorizationParams = authorizationParams;
    }
}
