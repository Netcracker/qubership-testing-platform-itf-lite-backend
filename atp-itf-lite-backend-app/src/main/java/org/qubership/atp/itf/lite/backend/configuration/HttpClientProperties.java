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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Configuration
@ConfigurationProperties(prefix = "atp.itf.lite")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HttpClientProperties {
    // Determines the timeout in milliseconds until a connection is established.
    private int connectionTimeout;

    // The timeout when requesting a connection from the connection manager.
    private int requestTimeout;

    // The timeout for waiting for data
    private int socketTimeout;

    private int maxTotalConnections;
    private int defaultKeepAliveTimeMillis;
    private int closeIdleConnectionWaitTimeSecs;
}
