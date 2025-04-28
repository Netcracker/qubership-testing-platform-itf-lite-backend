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

package org.qubership.atp.itf.lite.backend.model.entities.auth;

import static javax.persistence.EnumType.STRING;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.javers.core.metamodel.annotation.DiffInclude;
import org.qubership.atp.itf.lite.backend.enums.auth.OAuth2GrantType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "oauth2_request_authorizations")
public class OAuth2RequestAuthorization extends RequestAuthorization {

    @Column(name = "header_prefix")
    @DiffInclude
    private String headerPrefix;

    @Column(name = "grant_type", nullable = false)
    @Enumerated(value = STRING)
    @DiffInclude
    private OAuth2GrantType grantType;

    @Column(name = "auth_url")
    @DiffInclude
    private String authUrl;

    //NOTE: That 'url' is used as access_token_url !!!
    @Column(name = "url", nullable = false)
    @DiffInclude
    private String url;

    @Column(name = "client_id", nullable = false)
    @DiffInclude
    private String clientId;

    @Column(name = "client_secret")
    @DiffInclude
    private String clientSecret;

    @Column(name = "username")
    @DiffInclude
    private String username;

    @Column(name = "password")
    @DiffInclude
    private String password;

    @Column(name = "scope")
    @DiffInclude
    private String scope;

    @Column(name = "state")
    @DiffInclude
    private String state;

    @Column(name = "token")
    private String token;

    /**
     * Copy OAuth2RequestAuthorization constructor.
     * @param authorization authorization
     */
    public OAuth2RequestAuthorization(OAuth2RequestAuthorization authorization) {
        super(authorization);
        this.headerPrefix = authorization.getHeaderPrefix();
        this.grantType = authorization.getGrantType();
        this.authUrl = authorization.getAuthUrl();
        this.url = authorization.getUrl();
        this.clientId = authorization.getClientId();
        this.clientSecret = authorization.getClientSecret();
        this.username = authorization.getUsername();
        this.password = authorization.getPassword();
        this.scope = authorization.getScope();
        this.state = authorization.getState();
        this.token = authorization.getToken();
    }

    /**
     * Create copy of current authorization request.
     * @return OAuth2RequestAuthorization
     */
    @Override
    public OAuth2RequestAuthorization copy() {
        return new OAuth2RequestAuthorization(this);
    }
}
