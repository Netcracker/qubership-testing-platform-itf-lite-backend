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

package org.qubership.atp.itf.lite.backend.model.ei;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.stream.Collectors;

import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
public class ToPostmanRequest {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private HttpMethod method;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ToPostmanHeader> header;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ToPostmanBody body;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ToPostmanUrl url;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ToPostmanAuth auth;

    /**
     * Create Postman request entity by request.
     */
    public ToPostmanRequest(HttpRequest request) {
        this.description = request.getName();
        this.method = request.getHttpMethod();
        if (request.getRequestHeaders() != null) {
            this.header = request.getRequestHeaders().stream().map(ToPostmanHeader::new).collect(Collectors.toList());
        }
        RequestBody body = request.getBody();
        if (body != null && isBodyHasAContent(body)) {
            this.body = new ToPostmanBody(request);
        }
        if (request.getUrl() != null) {
            this.url = new ToPostmanUrl(request.getUrl());
        }
        this.auth = new ToPostmanAuth(request.getAuthorization());
    }

    public boolean isBodyHasAContent(RequestBody body) {
        return body.getContent() != null || body.getQuery() != null
                || body.getVariables() != null || !isEmpty(body.getFormDataBody());
    }
}
