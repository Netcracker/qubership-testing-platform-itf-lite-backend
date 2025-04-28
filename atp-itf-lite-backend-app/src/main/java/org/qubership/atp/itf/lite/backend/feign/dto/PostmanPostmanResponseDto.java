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
public class PostmanPostmanResponseDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private String status;
  private Integer code;
  private List<HeaderDto> header;
  private String body;
  private Integer responseTime;

  public PostmanPostmanResponseDto status(String status) {
    this.status = status;
    return this;
  }

  public PostmanPostmanResponseDto code(Integer code) {
    this.code = code;
    return this;
  }

  public PostmanPostmanResponseDto header(List<HeaderDto> header) {
    this.header = header;
    return this;
  }

  public PostmanPostmanResponseDto body(String body) {
    this.body = body;
    return this;
  }

  public PostmanPostmanResponseDto responseTime(Integer responseTime) {
    this.responseTime = responseTime;
    return this;
  }
}

