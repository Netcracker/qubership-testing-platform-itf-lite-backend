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

package org.qubership.atp.itf.lite.backend.model.entities.converters;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Converter
@Component
@NoArgsConstructor
@Slf4j
public class HttpHeaderSaveRequestConverter implements AttributeConverter<HttpHeaderSaveRequest, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(HttpHeaderSaveRequest httpHeaderSaveRequest) {
        String errorMessageJson = null;
        if (httpHeaderSaveRequest != null) {
            try {
                errorMessageJson = objectMapper.writeValueAsString(httpHeaderSaveRequest);
            } catch (final JsonProcessingException e) {
                log.error("JSON writing error", e);
            }
        }
        return errorMessageJson;
    }

    @Override
    public HttpHeaderSaveRequest convertToEntityAttribute(String httpHeaderSaveRequest) {
        if (StringUtils.isNotBlank(httpHeaderSaveRequest)) {
            try {
                return objectMapper.readValue(httpHeaderSaveRequest, HttpHeaderSaveRequest.class);
            } catch (final IOException e) {
                log.error("JSON writing error", e);
            }
        }
        return null;
    }
}
