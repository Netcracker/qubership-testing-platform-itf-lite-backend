/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

import java.net.URI;
import java.util.Properties;
import java.util.UUID;

import org.qubership.atp.itf.lite.backend.model.api.response.itf.ItfParametersResolveResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface ItfFeignClient {

    @GetMapping("{route}/context/get")
    String getContext(URI itfUrl, @PathVariable(value = "route") String route,
                                      @RequestParam(value = "projectUuid") UUID projectId,
                                      @RequestParam(value = "id") String contextId);

    @PutMapping("{route}/velocity")
    ItfParametersResolveResponse processVelocity(URI itfUrl, @PathVariable(value = "route") String route,
                                                 @RequestParam(value = "projectUuid") UUID projectId,
                                                 @RequestBody Properties properties);
}
