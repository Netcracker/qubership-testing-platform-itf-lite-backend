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

package org.qubership.atp.itf.lite.backend;

import org.qubership.atp.auth.springbootstarter.security.oauth2.client.config.annotation.EnableM2MRestTemplate;
import org.qubership.atp.auth.springbootstarter.security.oauth2.client.config.annotation.EnableOauth2FeignClientInterceptor;
import org.qubership.atp.common.lock.annotation.EnableAtpLockManager;
import org.qubership.atp.crypt.config.annotation.AtpCryptoEnable;
import org.qubership.atp.crypt.config.annotation.AtpDecryptorEnable;
import org.qubership.atp.integration.configuration.service.annotation.EnableAtpNotification;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "org.qubership.atp.itf.lite.backend",
        "org.qubership.atp.common.probes.controllers"
})
@EnableKafka
@EnableAsync
@EnableM2MRestTemplate
@EnableDiscoveryClient
@EnableCaching
@EnableFeignClients(basePackages = {"org.qubership.atp"})
@EnableOauth2FeignClientInterceptor
@EnableScheduling
@EnableAtpLockManager
@AtpCryptoEnable
@AtpDecryptorEnable
@EnableAtpNotification
public class Main {

    /**
     * Setup spring application and run it.
     *
     * @param args args
     */
    public static void main(String[] args) {
        SpringApplicationBuilder app = new SpringApplicationBuilder(Main.class);
        app.build().addListeners(
                new ApplicationPidFileWriter("application.pid")
        );
        app.run(args);
    }
}
