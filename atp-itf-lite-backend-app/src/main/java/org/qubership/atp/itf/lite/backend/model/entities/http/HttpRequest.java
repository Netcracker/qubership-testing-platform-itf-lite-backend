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

package org.qubership.atp.itf.lite.backend.model.entities.http;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.javers.core.metamodel.annotation.DiffInclude;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "HTTP_REQUESTS")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class HttpRequest extends Request {
    @Enumerated(EnumType.STRING)
    @Column(name = "http_method")
    @DiffInclude
    private HttpMethod httpMethod;

    @Column(name = "url")
    @DiffInclude
    private String url;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "request_id")
    @DiffInclude
    private List<RequestParam> requestParams;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "request_id")
    @DiffInclude
    private List<RequestHeader> requestHeaders;

    @Embedded
    @DiffInclude
    private RequestBody body;

    @Transient
    private HttpHeaderSaveRequest cookieHeader;

    /**
     * Copy httpRequest constructor.
     *
     * @param httpRequest httpRequest to copy
     */
    public HttpRequest(HttpRequest httpRequest) {
        super(httpRequest);
        this.httpMethod = httpRequest.getHttpMethod();
        this.url = httpRequest.getUrl();
        this.requestParams = httpRequest.getRequestParams();
        this.requestHeaders = httpRequest.getRequestHeaders();
        this.body = httpRequest.getBody();
        this.cookieHeader = httpRequest.getCookieHeader();
    }
}
