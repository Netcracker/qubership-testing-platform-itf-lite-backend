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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.javers.core.metamodel.annotation.DiffInclude;
import org.qubership.atp.itf.lite.backend.enums.auth.OAuth1AddDataType;
import org.qubership.atp.itf.lite.backend.enums.auth.OAuth1SignatureMethod;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "oauth1_request_authorizations")
public class OAuth1RequestAuthorization extends RequestAuthorization {

    @Column(name = "url")
    @DiffInclude
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method")
    @DiffInclude
    private HttpMethod httpMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "add_data_type", nullable = false)
    @DiffInclude
    private OAuth1AddDataType addDataType;

    @Column(name = "signature_method", nullable = false)
    @Enumerated(value = STRING)
    @DiffInclude
    private OAuth1SignatureMethod signatureMethod;

    @Column(name = "consumer_key", nullable = false)
    @DiffInclude
    private String consumerKey;

    @Column(name = "consumer_secret", nullable = false)
    @DiffInclude
    private String consumerSecret;

    @Column(name = "access_token")
    @DiffInclude
    private String accessToken;

    @Column(name = "token_secret")
    @DiffInclude
    private String tokenSecret;

    /**
     * Copy OAuth1RequestAuthorization constructor.
     * @param authorization authorization
     */
    public OAuth1RequestAuthorization(OAuth1RequestAuthorization authorization) {
        super(authorization);
        this.signatureMethod = authorization.getSignatureMethod();
        this.consumerKey = authorization.getConsumerKey();
        this.consumerSecret = authorization.getConsumerSecret();
        this.accessToken = authorization.getAccessToken();
        this.tokenSecret = authorization.getTokenSecret();
    }

    /**
     * Create copy of current authorization request.
     * @return OAuth1RequestAuthorization
     */
    @Override
    public OAuth1RequestAuthorization copy() {
        return new OAuth1RequestAuthorization(this);
    }
}
