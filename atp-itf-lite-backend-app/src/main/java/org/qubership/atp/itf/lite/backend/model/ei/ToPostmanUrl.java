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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;

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
        this.raw = urlString;
        try {
            URL url = new URL(urlString);
            this.protocol = url.getProtocol();
            if (Strings.isNotBlank(url.getHost())) {
                this.host = Arrays.asList(url.getHost().split("\\."));
            }
            if (url.getPort() != -1) {
                this.port = String.valueOf(url.getPort());
            }
            if (Strings.isNotBlank(url.getPath())) {
                String path = url.getPath().startsWith("/") ? url.getPath().replaceFirst("/", "") : url.getPath();
                this.path = Arrays.asList(path.split("/"));
            }
            if (Strings.isNotBlank(url.getQuery())) {
                this.query = Arrays.stream(url.getQuery().split("&"))
                        .map(s -> {
                            String[] keyValue = s.split("=");
                            return new ToPostmanMap(keyValue[0], keyValue.length == 2 ? keyValue[1] : null);
                        })
                        .collect(Collectors.toList());
            }

        } catch (MalformedURLException e) {
            this.host = Arrays.asList(urlString);
            log.warn("Failed to parse URL '{}' during export to POSTMAN", urlString);
        }
    }
}
