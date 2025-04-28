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

package org.qubership.atp.itf.lite.backend.model.documentation;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.qubership.atp.itf.lite.backend.enums.EntityType;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;

import lombok.Data;

@Data
public class RequestDocumentation extends AbstractDocumentation {
    @NotNull
    private TransportType transportType;
    private List<RequestParam> requestParams;
    private List<RequestHeader> requestHeaders;
    private RequestBody body;

    private String capabilitiesExchangeRequest;
    private String watchdogDefaultTemplate;
    private Integer responseTimeout;
    private String url;
    private HttpMethod httpMethod;
    private String host;
    private String port;

    public RequestDocumentation() {
        super();
        this.setType(EntityType.REQUEST);
    }
}