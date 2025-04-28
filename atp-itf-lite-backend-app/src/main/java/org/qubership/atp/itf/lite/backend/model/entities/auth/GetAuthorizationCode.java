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

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "get_authorization_code")
@AllArgsConstructor
public class GetAuthorizationCode implements Serializable {

    private static final long serialVersionUID = 7554727020800563080L;

    @Id
    @Column(name = "sse_id", nullable = false)
    private UUID sseId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "started_at", nullable = false)
    private Timestamp startedAt;

    @Column(name = "access_token_url", nullable = false)
    private String accessTokenUrl;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "client_secret", nullable = false)
    private String clientSecret;

    @Column(name = "scope")
    private String scope;

    @Column(name = "state")
    private String state;

    @Column(name = "redirect_uri")
    private String redirectUri;

    @Column(name = "response_state")
    private String responseState;

    @Column(name = "username", nullable = false)
    private String userName;

    @Column(name = "authorization_code")
    private String authorizationCode;

    @Column(name = "token")
    private String token;
}
