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

import java.util.List;
import java.util.Map;

import org.qubership.atp.itf.lite.backend.feign.dto.ConsoleLogDto;
import org.qubership.atp.itf.lite.backend.model.api.dto.ResponseCookie;
import org.qubership.atp.itf.lite.backend.model.api.request.ContextVariable;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HistoryRequestDetailsResponse {

    private RequestExecution requestExecution;
    private HistoryRequestBody requestBody;
    private String responseBody;
    private String errorMessage;
    private String requestPreScript;
    private String requestPostScript;
    private List<TestStatus> requestTests;
    private List<ConsoleLogDto> consoleLogs;
    private List<ContextVariable> contextVariables;
    private List<ResponseCookie> cookies;
    private HttpHeaderSaveRequest cookieHeader;

    private Map<String, List<String>> requestHeaders;
    private Map<String, List<String>> responseHeaders;

    private String capabilitiesExchangeRequest;
    private String watchdogDefaultTemplate;
    private Map<String, String> properties;

}
