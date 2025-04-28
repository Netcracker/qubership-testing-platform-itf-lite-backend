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

package org.qubership.atp.itf.lite.backend.feign.dto.history;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

@JsonIgnoreProperties(
  value = "entityType",
  allowSetters = true
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "entityType", visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = FolderHistoryChangeDto.class, name = "FOLDER"),
  @JsonSubTypes.Type(value = RequestDto.class, name = "REQUEST")
})
@JsonTypeName("HttpRequestHistory")
@Data
public class HttpRequestHistoryDto extends RequestDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private String folderName;
  private String httpMethod;
  private String url;
  private List<RequestParamDto> requestParams;
  private List<RequestHeaderDto> requestHeaders;
  private RequestBodyDto body;
}