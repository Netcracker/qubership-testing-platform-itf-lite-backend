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
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class PostmanDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private PostmanPostmanRequestDto postmanRequest;
  private PostmanPostmanResponseDto postmanResponse;
  private Map<String, Object> globals;
  private Map<String, Object> collectionVariables;
  private Map<String, Object> environment;
  private Map<String, Object> iterationData;
  private Map<String, Object> variables;
  private List<PostmanCookieDto> cookies;

  public PostmanDto postmanRequest(PostmanPostmanRequestDto postmanRequest) {
    this.postmanRequest = postmanRequest;
    return this;
  }

  public PostmanDto postmanResponse(PostmanPostmanResponseDto postmanResponse) {
    this.postmanResponse = postmanResponse;
    return this;
  }

  public PostmanDto globals(Map<String, Object> globals) {
    this.globals = globals;
    return this;
  }

  public PostmanDto collectionVariables(Map<String, Object> collectionVariables) {
    this.collectionVariables = collectionVariables;
    return this;
  }

  public PostmanDto environment(Map<String, Object> environment) {
    this.environment = environment;
    return this;
  }

  public PostmanDto iterationData(Map<String, Object> iterationData) {
    this.iterationData = iterationData;
    return this;
  }

  public PostmanDto variables(Map<String, Object> variables) {
    this.variables = variables;
    return this;
  }

  public PostmanDto cookies(List<PostmanCookieDto> cookies) {
    this.cookies = cookies;
    return this;
  }
}

