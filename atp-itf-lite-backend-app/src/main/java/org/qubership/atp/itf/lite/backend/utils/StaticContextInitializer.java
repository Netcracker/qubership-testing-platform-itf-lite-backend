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

package org.qubership.atp.itf.lite.backend.utils;

import javax.annotation.PostConstruct;

import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.service.TemplateResolverService;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StaticContextInitializer {

    private final ObjectMapper objectMapper;
    private final TemplateResolverService templateResolverService;
    private final ModelMapper modelMapper;

    @PostConstruct
    void init() {
        AuthorizationUtils.setObjectMapper(objectMapper);
        AuthorizationUtils.setModelMapper(modelMapper);
        AuthorizationUtils.setTemplateResolverService(templateResolverService);
    }

}
