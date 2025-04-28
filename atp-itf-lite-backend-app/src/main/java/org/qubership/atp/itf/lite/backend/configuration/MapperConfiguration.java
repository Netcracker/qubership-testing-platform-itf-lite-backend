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

package org.qubership.atp.itf.lite.backend.configuration;

import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.BasicAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.BearerAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.InheritFromParentAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth1AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth2AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpParamSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BasicRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BearerRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.InheritFromParentRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth1RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth2RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class MapperConfiguration {

    private static final Map<Class<? extends RequestAuthorization>, Class<? extends AuthorizationSaveRequest>>
            authorizationToSaveAuthorizationMap
            = new HashMap<Class<? extends RequestAuthorization>, Class<? extends AuthorizationSaveRequest>>() {
        {
                put(RequestAuthorization.class, AuthorizationSaveRequest.class);
                put(BasicRequestAuthorization.class, BasicAuthorizationSaveRequest.class);
                put(BearerRequestAuthorization.class, BearerAuthorizationSaveRequest.class);
                put(InheritFromParentRequestAuthorization.class, InheritFromParentAuthorizationSaveRequest.class);
                put(OAuth1RequestAuthorization.class, OAuth1AuthorizationSaveRequest.class);
                put(OAuth2RequestAuthorization.class, OAuth2AuthorizationSaveRequest.class);
        }
    };

    /**
     * Configuration for modelMapper bean.
     * @return configured modelMapper
     */
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.createTypeMap(AuthorizationSaveRequest.class, RequestAuthorization.class)
                .setConverter(mappingContext -> {
                    AuthorizationSaveRequest source = mappingContext.getSource();
                    if (nonNull(source)) {
                        return modelMapper.map(source, source.getAuthEntityType());
                    }
                    return null;
                });
        modelMapper.createTypeMap(RequestAuthorization.class, AuthorizationSaveRequest.class)
                .setConverter(mappingContext -> {
                    RequestAuthorization source = mappingContext.getSource();
                    if (nonNull(source)) {
                        return modelMapper.map(source, authorizationToSaveAuthorizationMap.get(source.getClass()));
                    }
                    return null;
                });
        modelMapper.createTypeMap(HttpRequestEntitySaveRequest.class, HttpRequest.class)
                .addMappings(map -> {
                    map.using(mappingContext -> {
                            List<HttpParamSaveRequest> source = (List<HttpParamSaveRequest>) mappingContext.getSource();
                            if (source != null) {
                                return source
                                        .stream()
                                        .map(RequestParam::new)
                                        .collect(Collectors.toList());
                            }
                            return new ArrayList<>();
                        }).map(HttpRequestEntitySaveRequest::getRequestParams, HttpRequest::setRequestParams);
                    map.using(mappingContext -> {
                        List<HttpHeaderSaveRequest> source = (List<HttpHeaderSaveRequest>) mappingContext.getSource();
                        if (source != null) {
                            return source
                                    .stream()
                                    .map(RequestHeader::new)
                                    .collect(Collectors.toList());
                        }
                        return new ArrayList<>();
                    }).map(HttpRequestEntitySaveRequest::getRequestHeaders, HttpRequest::setRequestHeaders);
                });
        return modelMapper;
    }

    /**
     * Configuration for objectMapper bean.
     * @return configured objectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper() {
            {
                this.registerModule(new JavaTimeModule());
                this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                this.findAndRegisterModules();
            }
        };
    }
}
