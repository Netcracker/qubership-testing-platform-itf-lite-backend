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
import java.util.UUID;

import org.qubership.atp.itf.lite.backend.feign.dto.history.ActionParameterDto;

import lombok.Data;

@Data
public class EnrichedCompoundDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private List<EnrichedCompoundDto> childCompounds;
  private String comment;
  private String content;
  private Boolean deprecated;
  private List<DirectiveDto> directives;
  private ActionEntityDto entity;
  private List<FlagsEnum> flags;
  private Boolean hidden;
  private UUID id;
  private Integer lineNumber;
  private List<ActionParameterDto> parameters = null;
  private String scenarioHashSum;
  private UUID systemId;
  private Integer timeout;
  private TypeEnum type;
}

