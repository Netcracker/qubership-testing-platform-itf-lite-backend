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

package org.qubership.atp.itf.lite.backend.service.rest;

import java.util.UUID;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestTemplateService {

    private final HttpClientService httpClientService;

    /**
     * Configuration for rest template.
     *
     * @return configured rest template
     */
    public RestTemplate restTemplate(UUID projectId) {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        CloseableHttpClient httpClient = httpClientService.getHttpClient(projectId);
        clientHttpRequestFactory.setHttpClient(httpClient);
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
        restTemplate.setUriTemplateHandler(factory);
        return restTemplate;
    }
}
