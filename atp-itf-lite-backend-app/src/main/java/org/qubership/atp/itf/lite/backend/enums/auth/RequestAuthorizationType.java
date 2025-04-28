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

package org.qubership.atp.itf.lite.backend.enums.auth;

import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.BearerAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.InheritFromParentAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth1AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth2AuthorizationSaveRequest;

import lombok.Getter;

public enum RequestAuthorizationType {
    OAUTH1(AuthorizationTypeNames.OAUTH1, OAuth1AuthorizationSaveRequest.class),
    OAUTH2(AuthorizationTypeNames.OAUTH2, OAuth2AuthorizationSaveRequest.class),
    BEARER(AuthorizationTypeNames.BEARER, BearerAuthorizationSaveRequest.class),
    BASIC(AuthorizationTypeNames.BASIC, BearerAuthorizationSaveRequest.class),
    INHERIT_FROM_PARENT(AuthorizationTypeNames.INHERIT_FROM_PARENT, InheritFromParentAuthorizationSaveRequest.class);

    @Getter
    private String name;

    @Getter
    private Class<? extends AuthorizationSaveRequest> requestType;

    RequestAuthorizationType(String name, Class<? extends AuthorizationSaveRequest> requestType) {
        this.name = name;
        this.requestType = requestType;
    }

    // all authorization type names must be in upper case for correct postman collections import
    public static class AuthorizationTypeNames {
        public static final String OAUTH1 = "OAUTH1";
        public static final String OAUTH2 = "OAUTH2";
        public static final String BEARER = "BEARER";
        public static final String BASIC = "BASIC";
        public static final String INHERIT_FROM_PARENT = "INHERIT_FROM_PARENT";
    }
}
