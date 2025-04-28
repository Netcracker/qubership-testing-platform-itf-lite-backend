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

import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanExecuteScriptRequestDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanExecuteScriptResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name = "${feign.atp.itf.lite.script.engine.name}", url = "${feign.atp.itf.lite.script.engine.url}",
        path = "${feign.atp.itf.lite.script.engine.route}", configuration = FeignConfiguration.class)
public interface JsScriptEngineFeignClient {

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/api/v1/script/execute",
            produces = { "application/json" },
            consumes = { "application/json" }
    )
    ResponseEntity<PostmanExecuteScriptResponseDto> executePostmanScript(
            @RequestBody(required = false) PostmanExecuteScriptRequestDto postmanExecuteScriptRequestDto
    );
}
