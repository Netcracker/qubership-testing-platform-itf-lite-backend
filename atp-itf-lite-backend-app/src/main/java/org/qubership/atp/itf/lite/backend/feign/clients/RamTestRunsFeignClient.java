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

import java.util.List;
import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.itf.lite.backend.feign.dto.ContextVariableDto;
import org.qubership.atp.itf.lite.backend.feign.dto.LogRecordDto;
import org.qubership.atp.itf.lite.backend.feign.dto.LogRecordFilteringRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name = "${feign.atp.ram.name}", url = "${feign.atp.ram.url}",
        path = "${feign.atp.ram.route}", configuration = FeignConfiguration.class)
public interface RamTestRunsFeignClient {

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/api/testruns/{testRunId}/contextVariables/all",
            produces = { "application/json" }
    )
    ResponseEntity<List<ContextVariableDto>> getAllContextVariables(@PathVariable("testRunId") UUID testRunId);

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/api/testruns/{uuid}/logrecords",
            produces = { "application/json" },
            consumes = { "application/json" }
    )
    ResponseEntity<List<LogRecordDto>> getAllFilteredLogRecords(
            @PathVariable("uuid") UUID uuid,
            @RequestBody(required = false) LogRecordFilteringRequestDto logRecordFilteringRequestDto
    );
}
