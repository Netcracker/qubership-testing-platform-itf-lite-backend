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

package org.qubership.atp.itf.lite.backend.model.api.request;

import static org.qubership.atp.itf.lite.backend.utils.Constants.ITF_DESTINATION_TEMPLATE;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestItfExportRequest extends RequestExportRequest {
    private String itfUrl;

    @NotNull
    private BigInteger systemId;

    @NotNull
    private BigInteger operationId;
    
    private Map<UUID, BigInteger> requestIdsReceiversMap;

    @Override
    public String getDestination() {
        return String.format(ITF_DESTINATION_TEMPLATE, itfUrl, systemId, operationId);
    }
}
