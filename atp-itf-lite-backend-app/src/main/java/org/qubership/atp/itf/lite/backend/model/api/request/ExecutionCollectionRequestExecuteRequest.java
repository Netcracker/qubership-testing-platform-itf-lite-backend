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

package org.qubership.atp.itf.lite.backend.model.api.request;

import java.util.Map;
import java.util.UUID;

import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.itf.lite.backend.catalog.models.ActionEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionCollectionRequestExecuteRequest {
    private UUID projectId;
    private UUID executionRequestId;
    private UUID testRunId;
    private UUID testPlanId;
    private UUID parentStepId;
    private UUID logRecordId;
    private AtpCompaund section;
    private ActionEntity actionEntity;
    private Map<String, Object> context;
}
