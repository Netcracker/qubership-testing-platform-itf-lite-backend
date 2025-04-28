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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.Data;

@Data
public class TestRunDto {

  private UUID parentTestRunId;
  private Boolean isGroupedTestRun;
  private UUID executionRequestId;
  private UUID testCaseId;
  private String testCaseName;
  private ExecutionStatusDto executionStatus;
  private TestingStatusDto testingStatus;
  private OffsetDateTime startDate;
  private OffsetDateTime finishDate;
  private Long duration;
  private String executor;
  private String jiraTicket;
  private List<String> taHost;
  private List<String> qaHost;
  private List<String> solutionBuild;
  private UUID rootCauseId;
  private String dataSetUrl;
  private List<FlagsDto> flags;
  private String dataSetListUrl;
  private String logCollectorData;
  private Boolean fdrWasSent;
  private String fdrLink;
  private Integer numberOfScreens;
  private Set<String> urlToBrowserOrLogs;
  private String urlToBrowserSession;
  private Integer passedRate;
  private Integer warningRate;
  private Integer failedRate;
  private CommentDto comment;
  private MetaInfoDto metaInfo;
  private TestRunStatisticDto statistic;
  private TestScopeSectionsDto testScopeSection;
  private Integer order;
  private List<UUID> labelIds;
  private Set<String> browserNames;
  private UUID uuid;
  private String name;
  private Boolean isFinalTestRun;
  private UUID initialTestRunId;
}

