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

import org.qubership.atp.adapter.common.utils.RequestUtils;
import org.qubership.atp.auth.springbootstarter.security.oauth2.client.config.Oauth2FeignClientInterceptor;
import org.qubership.atp.itf.lite.backend.interceptors.ItfLiteHttpRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("default")
public class HttpRequestConfiguration {

    /**
     * Register Http interceptor.
     */
    @Bean
    public ItfLiteHttpRequestInterceptor itfLiteHttpRequestInterceptor(Oauth2FeignClientInterceptor oauthInterceptor) {
        ItfLiteHttpRequestInterceptor httpRequestInterceptor = new ItfLiteHttpRequestInterceptor(oauthInterceptor);
        RequestUtils.registerHttpInterceptor(httpRequestInterceptor);
        return httpRequestInterceptor;
    }
}
