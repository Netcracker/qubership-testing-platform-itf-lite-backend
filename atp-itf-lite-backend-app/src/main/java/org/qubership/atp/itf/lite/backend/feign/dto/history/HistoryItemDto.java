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

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

@JsonTypeName("HistoryItem")
@Data
public class HistoryItemDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private HistoryItemTypeDto type;
  private Integer version;
  private String modifiedWhen;
  private String modifiedBy;
  private List<String> added ;
  private List<String> deleted;
  private List<String> changed;
}

