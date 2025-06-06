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

package org.qubership.atp.itf.lite.backend.model.entities.converters;

import java.io.IOException;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.qubership.atp.itf.lite.backend.model.api.dto.ResponseCookie;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Converter
@Component
@NoArgsConstructor
@Slf4j
public class ListResponseCookieConverter implements AttributeConverter<List<ResponseCookie>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<ResponseCookie> requestParams) {
        String requestParamsJson = null;
        if (requestParams != null) {
            try {
                requestParamsJson = objectMapper.writeValueAsString(requestParams);
            } catch (final JsonProcessingException e) {
                log.error("JSON writing error", e);
            }
        }
        return requestParamsJson;
    }

    @Override
    public List<ResponseCookie> convertToEntityAttribute(String requestParamsJson) {
        List<ResponseCookie> requestParams = null;
        if (requestParamsJson != null) {
            try {
                requestParams = objectMapper.readValue(requestParamsJson,
                        new TypeReference<List<ResponseCookie>>() {});
            } catch (final IOException e) {
                log.error("JSON reading error", e);
            }
        }
        return requestParams;
    }
}
