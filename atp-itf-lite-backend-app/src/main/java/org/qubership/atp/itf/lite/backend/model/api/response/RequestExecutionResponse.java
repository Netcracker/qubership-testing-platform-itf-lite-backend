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

package org.qubership.atp.itf.lite.backend.model.api.response;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.itf.lite.backend.annotations.SerializableCheckable;
import org.qubership.atp.itf.lite.backend.enums.ContextVariableType;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanPostmanResponseDto;
import org.qubership.atp.itf.lite.backend.model.api.dto.ResponseCookie;
import org.qubership.atp.itf.lite.backend.model.api.request.ContextVariable;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.context.SaveRequestResolvingContext;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SerializableCheckable
public class RequestExecutionResponse implements Serializable {

    private UUID id;
    private List<RequestExecutionHeaderResponse> responseHeaders;
    private String body;
    private RequestBodyType bodyType;
    private String statusCode;
    private String statusText;
    private BigInteger duration;
    private Date startedWhen;
    private Date executedWhen;
    private String authorizationToken;
    @JsonIgnore
    private boolean testsPassed;
    private List<ContextVariable> contextVariables;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID executionId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("error")
    private ErrorResponseSerializable error;
    private List<ResponseCookie> cookies;
    private HttpHeaderSaveRequest cookieHeader;

    /**
     * Update fields by PostmanPostmanResponseDto.
     */
    public void updateFromPostmanResponse(PostmanPostmanResponseDto pmResponse) {
        if (Objects.nonNull(pmResponse.getHeader())) {
            this.responseHeaders = pmResponse.getHeader()
                    .stream()
                    .map(h -> new RequestExecutionHeaderResponse(h.getKey(), (String) h.getValue()))
                    .collect(Collectors.toList());
        }
        this.body = pmResponse.getBody();
        this.statusCode = String.valueOf(pmResponse.getCode());
        this.statusText = pmResponse.getStatus();
        this.duration = BigInteger.valueOf(pmResponse.getResponseTime());
    }

    /**
     * Parses and sets context variables from resolving context.
     * @param resolvingContext resolving context
     */
    public void parseAndSetContextVariables(SaveRequestResolvingContext resolvingContext) {
        if (resolvingContext != null) {
            parseAndSetContextVariables(resolvingContext.getGlobals(), ContextVariableType.GLOBAL);
            parseAndSetContextVariables(resolvingContext.getCollectionVariables(), ContextVariableType.COLLECTION);
            parseAndSetContextVariables(resolvingContext.getEnvironmentVariables(), ContextVariableType.ENVIRONMENT);
            parseAndSetContextVariables(resolvingContext.getEnvironment(), ContextVariableType.ENVIRONMENT);
            parseAndSetContextVariables(resolvingContext.getIterationData(), ContextVariableType.DATA);
            parseAndSetContextVariables(resolvingContext.getVariables(), ContextVariableType.LOCAL);
        }
    }

    private void parseAndSetContextVariables(Map<String, Object> context, ContextVariableType contextVariableType) {
        if (CollectionUtils.isEmpty(context)) {
            return;
        }
        if (contextVariables == null) {
            contextVariables = new ArrayList<>();
        }
        contextVariables.addAll(context.entrySet().stream().map(entry -> new ContextVariable(entry.getKey(),
                entry.getValue(), contextVariableType)).collect(Collectors.toList()));
    }
}
