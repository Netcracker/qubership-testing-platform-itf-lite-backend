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

import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionHeaderResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionResponse;
import org.qubership.atp.itf.lite.backend.model.entities.converters.HashMapConverter;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "http_request_execution_details")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class HttpRequestExecutionDetails extends RequestExecutionDetails {

    @Column(name = "request_headers", columnDefinition = "TEXT")
    @Convert(converter = HashMapConverter.class)
    private Map<String, List<String>> requestHeaders;

    @Column(name = "response_headers", columnDefinition = "TEXT")
    @Convert(converter = HashMapConverter.class)
    private Map<String, List<String>> responseHeaders;

    /**
     * HttpRequestExecutionDetails constructor.
     */
    public HttpRequestExecutionDetails(RequestExecution requestExecution, HttpRequestEntitySaveRequest request,
                                       RequestExecutionResponse response, Exception errorMessage,
                                       HistoryRequestBody requestBody) {
        this.update(requestExecution, request, response, errorMessage, requestBody);
    }

    /**
     * Update requestHeaders and responseHeaders.
     */
    public void update(RequestExecution requestExecution, HttpRequestEntitySaveRequest request,
                  RequestExecutionResponse response, Exception errorMessage, HistoryRequestBody requestBody) {
        super.update(requestExecution, request, response, errorMessage, requestBody);
        this.requestHeaders = collectRequestHeaders(request.getRequestHeaders());
        if (response != null) {
            this.responseHeaders = collectResponseHeaders(response.getResponseHeaders());
        }
    }

    private Map<String, List<String>> collectResponseHeaders(List<RequestExecutionHeaderResponse> responseHeaders) {
        Map<String, List<String>> headersOrigin = new HashMap<>();
        if (nonNull(responseHeaders)) {
            responseHeaders.forEach(header -> {
                String key = header.getKey();
                List<String> values;
                if (nonNull(headersOrigin.get(key))) {
                    values = headersOrigin.get(key);
                    values.add(header.getValue());
                } else {
                    values = new ArrayList<>();
                    values.add(header.getValue());
                }
                headersOrigin.put(key, values);
            });
        }
        return headersOrigin;
    }

    private Map<String, List<String>> collectRequestHeaders(List<HttpHeaderSaveRequest> requestHeaders) {
        Map<String, List<String>> headersOrigin = new HashMap<>();
        requestHeaders.stream()
                .filter(header -> !header.isDisabled())
                .forEach(header -> headersOrigin.put(header.getKey(), Collections.singletonList(header.getValue())));
        return headersOrigin;
    }
}
