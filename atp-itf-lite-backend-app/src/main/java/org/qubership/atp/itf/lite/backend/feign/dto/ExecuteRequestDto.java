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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class ExecuteRequestDto implements Serializable {

  private static final long serialVersionUID = 1L;
  private Boolean autoSyncCasesWithJira;
  private Boolean autoSyncRunsWithJira;
  private Object contextVariables;
  private List<String> emailRecipients = null;
  private String emailSubject;
  private UUID emailTemplateId;
  private List<UUID> environmentIds = new ArrayList<>();
  private Boolean executeExecution;
  private Boolean executePrerequisites;
  private Boolean executeValidation;
  private List<FlagsEnum> flags = null;
  private UUID labelTemplateId;
  private UUID logCollectorTemplateId;
  private String name;
  private UUID projectId;
  private List<UUID> taToolIds = new ArrayList<>();
  private List<UUID> testCaseIds = null;
  private List<EnrichedScenarioDto> testScenarios = null;
  private UUID testPlanId;
  private UUID testScopeId;
  private Integer threadCount;
  private UUID widgetConfigTemplateId;
  private List<ItemDto> filterLabels = null;
  private Boolean isMandatoryCheck;
  private Boolean isSsmCheck;
  private Boolean isIgnoreFailedChecks;
  private UUID jointExecutionKey;
  private BigDecimal jointExecutionCount;
  private BigDecimal jointExecutionTimeout;
  private UUID dataSetStorageId;
  private UUID datasetId;

  public ExecuteRequestDto contextVariables(Object contextVariables) {
    this.contextVariables = contextVariables;
    return this;
  }

  public ExecuteRequestDto emailRecipients(List<String> emailRecipients) {
    this.emailRecipients = emailRecipients;
    return this;
  }

  public ExecuteRequestDto emailSubject(String emailSubject) {
    this.emailSubject = emailSubject;
    return this;
  }

  public ExecuteRequestDto emailTemplateId(UUID emailTemplateId) {
    this.emailTemplateId = emailTemplateId;
    return this;
  }

  public ExecuteRequestDto environmentIds(List<UUID> environmentIds) {
    this.environmentIds = environmentIds;
    return this;
  }

  public ExecuteRequestDto flags(List<FlagsEnum> flags) {
    this.flags = flags;
    return this;
  }

  public ExecuteRequestDto logCollectorTemplateId(UUID logCollectorTemplateId) {
    this.logCollectorTemplateId = logCollectorTemplateId;
    return this;
  }

  public ExecuteRequestDto name(String name) {
    this.name = name;
    return this;
  }

  public ExecuteRequestDto projectId(UUID projectId) {
    this.projectId = projectId;
    return this;
  }

  public ExecuteRequestDto taToolIds(List<UUID> taToolIds) {
    this.taToolIds = taToolIds;
    return this;
  }

  public ExecuteRequestDto testScenarios(List<EnrichedScenarioDto> testScenarios) {
    this.testScenarios = testScenarios;
    return this;
  }

  public ExecuteRequestDto testPlanId(UUID testPlanId) {
    this.testPlanId = testPlanId;
    return this;
  }

  public ExecuteRequestDto threadCount(Integer threadCount) {
    this.threadCount = threadCount;
    return this;
  }

  public ExecuteRequestDto isMandatoryCheck(Boolean isMandatoryCheck) {
    this.isMandatoryCheck = isMandatoryCheck;
    return this;
  }

  public ExecuteRequestDto isSsmCheck(Boolean isSsmCheck) {
    this.isSsmCheck = isSsmCheck;
    return this;
  }

  public ExecuteRequestDto isIgnoreFailedChecks(Boolean isIgnoreFailedChecks) {
    this.isIgnoreFailedChecks = isIgnoreFailedChecks;
    return this;
  }

  public ExecuteRequestDto dataSetStorageId(UUID dataSetStorageId) {
    this.dataSetStorageId = dataSetStorageId;
    return this;
  }

  public ExecuteRequestDto datasetId(UUID datasetId) {
    this.datasetId = datasetId;
    return this;
  }

  public enum FlagsEnum {
    COLLECT_LOGS("COLLECT_LOGS"),
    COLLECT_LOGS_ON_FAIL("COLLECT_LOGS_ON_FAIL"),
    COLLECT_LOGS_ON_SKIPPED("COLLECT_LOGS_ON_SKIPPED"),
    COLLECT_LOGS_ON_WARNING("COLLECT_LOGS_ON_WARNING"),
    DO_NOT_PASS_INITIAL_CONTEXT("DO_NOT_PASS_INITIAL_CONTEXT"),
    EXECUTE_ANYWAY("EXECUTE_ANYWAY"),
    FAIL_IMMEDIATELY("FAIL_IMMEDIATELY"),
    IGNORE_PREREQUISITE_IN_PASS_RATE("IGNORE_PREREQUISITE_IN_PASS_RATE"),
    IGNORE_VALIDATION_IN_PASS_RATE("IGNORE_VALIDATION_IN_PASS_RATE"),
    INVERT_RESULT("INVERT_RESULT"),
    SKIP("SKIP"),
    SKIP_IF_DEPENDENCIES_FAIL("SKIP_IF_DEPENDENCIES_FAIL"),
    STOP_ON_FAIL("STOP_ON_FAIL"),
    TERMINATE_IF_FAIL("TERMINATE_IF_FAIL"),
    TERMINATE_IF_PREREQUISITE_FAIL("TERMINATE_IF_PREREQUISITE_FAIL"),
    COLLECT_SSM_METRICS_ON_FAIL("COLLECT_SSM_METRICS_ON_FAIL");

    private String value;

    FlagsEnum(String value) {
      this.value = value;
    }
  }
}

