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

package org.qubership.atp.itf.lite.backend.feign.dto;

import java.io.Serializable;
import java.util.UUID;

import lombok.Data;

@Data
public class GetAccessCodeParametersDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private UUID projectId;
  private UUID sseId;
  private String accessTokenUrl;
  private String clientId;
  private String clientSecret;
  private String scope;
  private String state;
  private String redirectUri;

  public GetAccessCodeParametersDto projectId(UUID projectId) {
    this.projectId = projectId;
    return this;
  }

  public GetAccessCodeParametersDto sseId(UUID sseId) {
    this.sseId = sseId;
    return this;
  }

  public GetAccessCodeParametersDto accessTokenUrl(String accessTokenUrl) {
    this.accessTokenUrl = accessTokenUrl;
    return this;
  }

  public GetAccessCodeParametersDto clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  public GetAccessCodeParametersDto clientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

  public GetAccessCodeParametersDto scope(String scope) {
    this.scope = scope;
    return this;
  }

  public GetAccessCodeParametersDto state(String state) {
    this.state = state;
    return this;
  }

  public GetAccessCodeParametersDto redirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
    return this;
  }
}

