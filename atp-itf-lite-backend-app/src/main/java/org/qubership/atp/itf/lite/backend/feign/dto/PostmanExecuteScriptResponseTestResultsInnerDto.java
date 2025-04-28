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
import java.math.BigDecimal;

import lombok.Data;

@Data
public class PostmanExecuteScriptResponseTestResultsInnerDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name;
  private Boolean async;
  private Boolean skipped;
  private Boolean passed;
  private PostmanExecuteScriptResponseTestResultsInnerErrorDto error;
  private BigDecimal index;

  public PostmanExecuteScriptResponseTestResultsInnerDto name(String name) {
    this.name = name;
    return this;
  }

  public PostmanExecuteScriptResponseTestResultsInnerDto async(Boolean async) {
    this.async = async;
    return this;
  }

  public PostmanExecuteScriptResponseTestResultsInnerDto skipped(Boolean skipped) {
    this.skipped = skipped;
    return this;
  }

  public PostmanExecuteScriptResponseTestResultsInnerDto index(BigDecimal index) {
    this.index = index;
    return this;
  }

  public PostmanExecuteScriptResponseTestResultsInnerDto passed(Boolean passed) {
    this.passed = passed;
    return this;
  }

  public PostmanExecuteScriptResponseTestResultsInnerDto error(
          PostmanExecuteScriptResponseTestResultsInnerErrorDto error) {
    this.error = error;
    return this;
  }
}

