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

public enum TypeActionDto {
  
  UI("UI"),
  ITF("ITF"),
  BV("BV"),
  NEWMAN("NEWMAN"),
  R_B_M("R_B_M"),
  MIA("MIA"),
  REST("REST"),
  SQL("SQL"),
  SSH("SSH"),
  COMPOUND("COMPOUND"),
  TECHNICAL("TECHNICAL"),
  TRANSPORT("TRANSPORT");

  private String value;

  TypeActionDto(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}

