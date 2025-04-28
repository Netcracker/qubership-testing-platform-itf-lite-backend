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

package org.qubership.atp.itf.lite.backend.model.api.kafka.entities;

import java.util.HashMap;
import java.util.Map;

import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HttpRequestExportEntity extends ExportRequestEntity {
    private HttpMethod httpMethod;
    private String url;
    private Map<String, String> queryParameters;
    private Map<String, String> requestHeaders;
    private RequestBody body;

    /**
     * Create http request entity from http request.
     * @param request http request
     */
    public HttpRequestExportEntity(HttpRequest request) {
        commonProcessing(request);
        this.setBody(request.getBody());
    }

    /**
     * Constructor HttpRequestExportEntity by request and flag simplify.
     */
    public HttpRequestExportEntity(HttpRequest request, boolean simplify) {
        commonProcessing(request);
        if (simplify && request.getBody() != null && RequestBodyType.GraphQL.equals(request.getBody().getType())) {
            RequestBody simplifiedBody = new RequestBody();
            simplifiedBody.setContent(request.getBody().computeAndGetContent());
            simplifiedBody.setType(RequestBodyType.JSON);
            this.setBody(simplifiedBody);
        } else {
            this.setBody(request.getBody());
        }
    }

    private void commonProcessing(HttpRequest request) {
        this.setId(request.getId());
        this.setName(request.getName());
        this.setTransportType(request.getTransportType());
        this.setHttpMethod(request.getHttpMethod());
        this.setUrl(request.getUrl());
        this.setBody(request.getBody());
        if (!request.getRequestParams().isEmpty()) {
            Map<String, String> parameters = new HashMap<>();
            request.getRequestParams().forEach(param -> {
                if (parameters.containsKey(param.getKey())) {
                    String value = parameters.get(param.getKey());
                    parameters.put(param.getKey(), value + "," + param.getValue());
                } else {
                    parameters.put(param.getKey(), param.getValue());
                }
            });
            this.setQueryParameters(parameters);
        }
        if (!request.getRequestHeaders().isEmpty()) {
            Map<String, String> headers = new HashMap<>();
            request.getRequestHeaders().forEach(header -> {
                if (headers.containsKey(header.getKey())) {
                    String value = headers.get(header.getKey());
                    headers.put(header.getKey(), value + ";" + header.getValue());
                } else {
                    headers.put(header.getKey(), header.getValue());
                }
            });
            this.setRequestHeaders(headers);
        }
    }
}
