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

package org.qubership.atp.itf.lite.backend.enums.http;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum RequestBodyType {
    JSON("JSON", Collections.singletonList("application/json")),
    GraphQL("GraphQL", Collections.singletonList("application/json")),
    XML("XML", Arrays.asList("application/xml", "text/xml")),
    TEXT("TEXT", Collections.singletonList("text/plain")),
    URLENCODED("URLEncoded", Collections.singletonList("application/xml")),
    Binary("Binary", Collections.singletonList("application/octet-stream")),
    FORM_DATA("Form-data", Collections.singletonList("multipart/form-data")),
    Velocity("Velocity", Collections.emptyList()),
    Wireshark("Wireshark", Collections.emptyList()),
    HTML("HTML", Collections.singletonList("text/html")),
    JavaScript("JavaScript", Collections.singletonList("application/javascript"));

    private final String name;
    @Getter
    private List<String> contentTypes;

    RequestBodyType(String name) {
        this.name = name;
    }

    RequestBodyType(String name, List<String> contentTypes) {
        this.name = name;
        this.contentTypes = contentTypes;
    }

    @JsonValue
    public String getName() {
        return this.name;
    }

    /**
     * Return RequestBodyType by name ignoring case or null.
     *
     * @param name RequestBodyType name
     * @return RequestBodyType
     */
    public static RequestBodyType valueOfIgnoreCase(String name) {
        for (RequestBodyType value : RequestBodyType.values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Return RequestBodyType by content type header value.
     *
     * @param contentType content type header value
     * @return RequestBodyType
     */
    public static RequestBodyType valueOfContentType(String contentType, String contentDisposition) {
        log.info("Content type when define type : Content-Type: {}, Content-Disposition: {}", contentType,
                contentDisposition);
        if (!StringUtils.isEmpty(contentDisposition)) {
            return Binary;
        }
        if (StringUtils.isEmpty(contentType)) {
            return null;
        }
        for (RequestBodyType value : RequestBodyType.values()) {
            if (value.getContentTypes() == null) {
                return null;
            }
            // remove all chars after first ; if present
            String clearedContentType = contentType.split(";")[0];
            if (value.getContentTypes().stream().anyMatch(clearedContentType::equalsIgnoreCase)) {
                return value;
            }
        }
        return null;
    }

}
