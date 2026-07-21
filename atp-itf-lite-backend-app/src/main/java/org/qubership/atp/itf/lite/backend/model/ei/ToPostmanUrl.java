/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class ToPostmanUrl {

    private String raw;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String protocol;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> host;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String port;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> path;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ToPostmanMap> query;

    /**
     * Create Postman url entity by string.
     */
    public ToPostmanUrl(String urlString) {
        this(urlString, null);
    }

    /**
     * Create Postman url entity by string and request parameters.
     */
    public ToPostmanUrl(String urlString, List<RequestParam> requestParams) {
        this.raw = urlString;
        try {
            URL url = new URL(urlString);
            this.protocol = url.getProtocol();
            if (StringUtils.isNotBlank(url.getHost())) {
                this.host = Arrays.asList(url.getHost().split("\\."));
            }
            if (url.getPort() != -1) {
                this.port = String.valueOf(url.getPort());
            }
            if (StringUtils.isNotBlank(url.getPath())) {
                String path = url.getPath().startsWith("/") ? url.getPath().replaceFirst("/", "") : url.getPath();
                this.path = Arrays.asList(path.split("/"));
            }
            if (StringUtils.isNotBlank(url.getQuery())) {
                this.query = Arrays.stream(url.getQuery().split("&"))
                        .map(s -> {
                            String[] keyValue = s.split("=");
                            return new ToPostmanMap(keyValue[0], keyValue.length == 2 ? keyValue[1] : null);
                        })
                        .collect(Collectors.toList());
            }
        } catch (MalformedURLException e) {
            this.host = List.of(urlString);
            log.warn("Failed to parse URL '{}' during export to POSTMAN", urlString);
        }
        try {
            addRequestParams(requestParams);
        } catch (RuntimeException e) {
            log.warn("Failed to add request parameters to URL '{}' during export to POSTMAN", urlString, e);
        }
    }

    private void addRequestParams(List<RequestParam> requestParams) {
        if (requestParams == null || requestParams.isEmpty()) {
            return;
        }
        if (query == null) {
            query = new ArrayList<>();
        }
        query.addAll(requestParams.stream()
                .map(param -> new ToPostmanMapDescriptionAndDisabled(param.getKey(), param.getValue(),
                        param.getDescription(), param.isDisabled()))
                .collect(Collectors.toList()));

        String activeParams = requestParams.stream()
                .filter(param -> !param.isDisabled())
                .map(param -> param.getValue() == null
                        ? param.getKey() : param.getKey() + "=" + param.getValue())
                .collect(Collectors.joining("&"));
        if (StringUtils.isNotEmpty(activeParams)) {
            raw += (raw.contains("?") ? "&" : "?") + activeParams;
        }
    }
}
