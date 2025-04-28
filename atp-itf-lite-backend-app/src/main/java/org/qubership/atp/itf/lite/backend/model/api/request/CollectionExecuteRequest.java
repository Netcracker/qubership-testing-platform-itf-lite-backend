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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.qubership.atp.itf.lite.backend.catalog.models.Flags;
import org.qubership.atp.itf.lite.backend.catalog.models.FlagsDeserializer;
import org.qubership.atp.itf.lite.backend.model.api.response.GroupResponse;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionExecuteRequest {

    private String name;
    private List<GroupResponse> treeNodes;

    @NotNull
    private List<UUID> environmentIds;

    private List<String> emailRecipients;
    private UUID emailTemplateId;
    private String emailSubject;

    @NotNull
    private List<UUID> taToolIds;

    @JsonDeserialize(using = FlagsDeserializer.class)
    private List<Flags> flags;

    private UUID logCollectorTemplateId;

    private UUID projectId;
    private UUID testPlanId;

    private boolean isMandatoryCheck;
    private boolean isSsmCheck;
    private boolean isIgnoreFailedChecks;
    private int threadCount;
    private UUID dataSetStorageId;
    private UUID dataSetId;
    private List<ContextVariable> contextVariables;
    private boolean propagateCookies;

    /**
     * Converts context variables to map.
     * @return map
     */
    public Map<String, Object> convertContextVariablesToMap() {
        if (!CollectionUtils.isEmpty(contextVariables)) {
            return contextVariables.stream().collect(Collectors.toMap(variable -> variable
                    .getContextVariableType()
                    .getContextScope()
                    .getPrefix() + variable.getKey(), ContextVariable::getValue));
        }
        return new HashMap<>();
    }

}
