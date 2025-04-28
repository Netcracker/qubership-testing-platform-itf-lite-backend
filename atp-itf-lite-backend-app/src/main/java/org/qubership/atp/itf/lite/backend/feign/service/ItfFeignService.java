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

package org.qubership.atp.itf.lite.backend.feign.service;

import java.util.UUID;

import org.qubership.atp.itf.lite.backend.feign.clients.ItfContextBalancerFeignClient;
import org.qubership.atp.itf.lite.backend.feign.clients.ItfVelocityBalancerFeignClient;
import org.qubership.atp.itf.lite.backend.feign.dto.ResponseObjectDto;
import org.qubership.atp.itf.lite.backend.feign.dto.UIVelocityRequestBodyDto;
import org.qubership.atp.itf.lite.backend.model.api.response.itf.ItfParametersResolveResponse;
import org.qubership.atp.itf.lite.backend.utils.StreamUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItfFeignService {

    private final ItfContextBalancerFeignClient itfContextBalancerFeignClient;
    private final ItfVelocityBalancerFeignClient itfVelocityBalancerFeignClient;

    public String getContext(String contextId, UUID projectUuid) {
        return itfContextBalancerFeignClient.get(contextId, projectUuid).getBody();
    }

    /**
     * Substitutes parameters values from ITF if ITF feign client is enabled.
     *
     * @param projectUuid project id
     * @param requestBody object to request in itf
     * @return ItfParametersResolveResponse received object from itf
     */
    public ItfParametersResolveResponse processVelocity(UUID projectUuid, UIVelocityRequestBodyDto requestBody) {
        ResponseObjectDto responseObjectDto
                = itfVelocityBalancerFeignClient.get(projectUuid, null, requestBody).getBody();
        ItfParametersResolveResponse itfParametersResolveResponse
                = StreamUtils.mapToClazz(responseObjectDto, ItfParametersResolveResponse.class);
        return itfParametersResolveResponse;
    }
}
