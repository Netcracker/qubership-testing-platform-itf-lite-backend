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

package org.qubership.atp.itf.lite.backend.interceptors;

import static org.qubership.atp.auth.springbootstarter.Constants.AUTHORIZATION_HEADER_NAME;
import static org.qubership.atp.auth.springbootstarter.Constants.BEARER_TOKEN_TYPE;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.qubership.atp.auth.springbootstarter.security.oauth2.client.config.Oauth2FeignClientInterceptor;

import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Interceptor is needed for the atp-adapter to receive the token before sending http requests.
 */
@Slf4j
@RequiredArgsConstructor
public class ItfLiteHttpRequestInterceptor implements HttpRequestInterceptor {
    private final Oauth2FeignClientInterceptor oauth2FeignClientInterceptor;

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        log.info("process [request={}, context={}]", request, context);
        Optional<String> token = getAuthorizationToken();
        if (token.isPresent()) {
            log.info("Set authorization token");
            request.addHeader(AUTHORIZATION_HEADER_NAME, token.get());
            return;
        }
        log.warn("Token in context is empty");
    }

    private Optional<String> getAuthorizationToken() {
        RequestTemplate requestTemplate = new RequestTemplate();
        oauth2FeignClientInterceptor.apply(requestTemplate);
        Collection<String> headers = requestTemplate.headers().get(AUTHORIZATION_HEADER_NAME);
        if (Objects.nonNull(headers)) {
            return headers.size() < 2 ? headers.stream().findFirst()
                    : headers.stream().filter(header -> header.startsWith(BEARER_TOKEN_TYPE)).findFirst();
        }
        return Optional.empty();
    }
}
