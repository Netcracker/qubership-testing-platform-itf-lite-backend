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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.qubership.atp.itf.lite.backend.feign.dto.ConsoleLogDto;
import org.qubership.atp.itf.lite.backend.model.api.dto.ResponseCookie;
import org.qubership.atp.itf.lite.backend.model.api.request.ContextVariable;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.ErrorResponseSerializable;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionResponse;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractEntity;
import org.qubership.atp.itf.lite.backend.model.entities.converters.ErrorResponseSerializableConverter;
import org.qubership.atp.itf.lite.backend.model.entities.converters.HttpHeaderSaveRequestConverter;
import org.qubership.atp.itf.lite.backend.model.entities.converters.ListConsoleLogConverter;
import org.qubership.atp.itf.lite.backend.model.entities.converters.ListContextVariableConverter;
import org.qubership.atp.itf.lite.backend.model.entities.converters.ListResponseCookieConverter;
import org.qubership.atp.itf.lite.backend.utils.RequestUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "request_execution_details")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class RequestExecutionDetails extends AbstractEntity {

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_execution_id")
    protected RequestExecution requestExecution;

    @Embedded
    protected HistoryRequestBody requestBody;

    @Column(name = "response_body", columnDefinition = "TEXT")
    protected String responseBody;

    @Column(name = "error_message", columnDefinition = "TEXT")
    @Convert(converter = ErrorResponseSerializableConverter.class)
    protected ErrorResponseSerializable errorMessage;

    @Column(name = "request_pre_script", columnDefinition = "TEXT")
    protected String requestPreScript;

    @Column(name = "request_post_script", columnDefinition = "TEXT")
    protected String requestPostScript;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "request_execution_details_id")
    protected List<TestStatus> requestTests;

    @Column(name = "console_logs")
    @Convert(converter = ListConsoleLogConverter.class)
    protected List<ConsoleLogDto> consoleLogs;

    @Column(name = "context_variables")
    @Convert(converter = ListContextVariableConverter.class)
    protected List<ContextVariable> contextVariables;

    @Column(name = "cookies")
    @Convert(converter = ListResponseCookieConverter.class)
    protected List<ResponseCookie> cookies;

    @Column(name = "cookie_header")
    @Convert(converter = HttpHeaderSaveRequestConverter.class)
    protected HttpHeaderSaveRequest cookieHeader;

    @Column(name = "response_body_byte")
    protected byte[] responseBodyByte;

    /**
     * Update request.
     *
     * @param requestExecution requestExecution
     * @param request          request
     * @param response         response
     * @param errorMessage     errorMessage
     * @param requestBody      requestBody
     */
    public void update(RequestExecution requestExecution, RequestEntitySaveRequest request,
                       RequestExecutionResponse response, Exception errorMessage, HistoryRequestBody requestBody) {
        this.requestExecution = requestExecution;
        this.requestBody = requestBody;
        if (response != null && response.getBody() != null) {
            this.responseBodyByte = response.getBody().getBytes(StandardCharsets.UTF_8);
        }
        this.errorMessage = RequestUtils.getErrorResponse(errorMessage);
        this.requestPreScript = request.getPreScripts();
        this.requestPostScript = request.getPostScripts();
    }

    /**
     * Add console log to list.
     *
     * @param consoleLog console log
     */
    public void addConsoleLog(ConsoleLogDto consoleLog) {
        if (consoleLog != null) {
            if (this.consoleLogs == null) {
                this.consoleLogs = new ArrayList<>();
            }
            this.consoleLogs.add(consoleLog);
        }
    }
}
