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

package org.qubership.atp.itf.lite.backend.feign.clients;

import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "${feign.atp.itf.name}",
        url = "${feign.atp.itf.url}",
        path = "${feign.atp.itf.route}",
        configuration = FeignConfiguration.class)
public interface ItfContextBalancerFeignClient {

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/context/get",
            produces = { "application/json" }
    )
    ResponseEntity<String> get(@RequestParam(value = "id") String id,
                               @RequestParam(value = "projectUuid") UUID projectUuid);
}
