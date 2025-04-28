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

package org.qubership.atp.itf.lite.backend.model.entities.history;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.qubership.atp.itf.lite.backend.enums.TestingStatus;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpParamSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionResponse;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractEntity;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;
import org.springframework.util.CollectionUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "request_executions")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class RequestExecution extends AbstractEntity {

    @Column(name = "url")
    private String url;

    @Column(name = "name")
    private String name;

    @Column(name = "project_id")
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type")
    private TransportType transportType;

    @Column(name = "executed_when")
    private Date executedWhen;

    @Column(name = "executor")
    private String executor;

    @Column(name = "status_code")
    private String statusCode;

    @Column(name = "status_text")
    private String statusText;

    @Column(name = "duration")
    private BigInteger duration;

    @Column(name = "sse_id")
    private UUID sseId;

    @Column(name = "authorization_token", columnDefinition = "TEXT")
    private String authorizationToken;

    @Column(name = "testing_status")
    @Enumerated(EnumType.STRING)
    private TestingStatus testingStatus;

    /**
     * RequestExecution constructor.
     */
    public RequestExecution(String executor, UUID sseId, RequestEntitySaveRequest request,
                            RequestExecutionResponse response, TestingStatus testingStatus) {
        this.executor = executor;
        this.sseId = sseId;
        this.update(request, response, testingStatus);
    }

    /**
     * Update fields by request, response and testing status.
     */
    public void update(RequestEntitySaveRequest request,
                       RequestExecutionResponse response, TestingStatus testingStatus) {
        this.url = collectUrl((HttpRequestEntitySaveRequest) request);
        this.name = request.getName();
        this.projectId = request.getProjectId();
        this.transportType = request.getTransportType();
        if (Objects.nonNull(response)) {
            this.executedWhen = response.getExecutedWhen();
            this.statusCode = response.getStatusCode();
            this.statusText = response.getStatusText();
            this.duration = response.getDuration();
            this.authorizationToken = response.getAuthorizationToken();
        }
        if (Objects.isNull(this.executedWhen)) {
            this.executedWhen = new Date();
        }
        this.testingStatus = testingStatus;
    }

    public void update(RequestEntitySaveRequest request,
                            RequestExecutionResponse response) {
        this.update(request, response, this.testingStatus);
    }

    private String collectUrl(HttpRequestEntitySaveRequest request) {
        final HttpMethod httpMethod = request.getHttpMethod();
        final String url = request.getUrl();
        final List<HttpParamSaveRequest> requestParams = request.getRequestParams();
        final StringBuilder builder = new StringBuilder()
                .append(httpMethod.name())
                .append(" ")
                .append(url);

        if (!CollectionUtils.isEmpty(requestParams)) {
            String params = requestParams.stream()
                    .filter(param -> !param.isDisabled())
                    .map(param -> param.getKey() + "=" + param.getValue())
                    .collect(Collectors.joining("&", "?", ""));
            builder.append(params);
        }

        return builder.toString();
    }
}
