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

import lombok.Data;

@Data
public class PostmanUrlDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private String protocol;
  private List<String> host;
  private String port;
  private List<String> path;
  private List<HeaderDto> query;

  public PostmanUrlDto protocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  public PostmanUrlDto host(List<String> host) {
    this.host = host;
    return this;
  }

  public PostmanUrlDto port(String port) {
    this.port = port;
    return this;
  }

  public PostmanUrlDto path(List<String> path) {
    this.path = path;
    return this;
  }

  public PostmanUrlDto query(List<HeaderDto> query) {
    this.query = query;
    return this;
  }


}

