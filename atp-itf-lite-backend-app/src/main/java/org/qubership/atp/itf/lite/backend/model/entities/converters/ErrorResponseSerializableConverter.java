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

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.itf.lite.backend.exceptions.ItfLiteException;
import org.qubership.atp.itf.lite.backend.model.api.response.ErrorResponseSerializable;
import org.qubership.atp.itf.lite.backend.utils.RequestUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Converter
@Component
@NoArgsConstructor
@Slf4j
public class ErrorResponseSerializableConverter implements AttributeConverter<ErrorResponseSerializable, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ErrorResponseSerializable errorMessage) {
        String errorMessageJson = null;
        if (errorMessage != null) {
            try {
                errorMessageJson = objectMapper.writeValueAsString(errorMessage);
            } catch (final JsonProcessingException e) {
                log.error("JSON writing error", e);
            }
        }
        return errorMessageJson;
    }

    @Override
    public ErrorResponseSerializable convertToEntityAttribute(String errorMessageString) {
        if (Strings.isNotBlank(errorMessageString)) {
            try {
                return objectMapper.readValue(errorMessageString, ErrorResponseSerializable.class);
            } catch (final IOException e) {
                return RequestUtils.getErrorResponse(new ItfLiteException(errorMessageString));
            }
        }
        return null;
    }
}
