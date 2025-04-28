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

package org.qubership.atp.itf.lite.backend.components.export;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.qubership.atp.itf.lite.backend.components.export.strategies.request.RequestExportStrategy;
import org.qubership.atp.itf.lite.backend.enums.ImportToolType;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RequestExportStrategiesRegistry {

    @Autowired
    private List<RequestExportStrategy> exportStrategies;

    /**
     * Lookup export strategy by import tool type and request transport type parameters.
     *
     * @param importToolType       import tool type name
     * @param requestTransportType request transport type name
     * @return export strategy implementation
     */
    public RequestExportStrategy getStrategy(@NotNull ImportToolType importToolType,
                                             @NotNull TransportType requestTransportType) {
        return exportStrategies.stream()
                .filter(exportStrategy -> importToolType.equals(exportStrategy.getImportToolType()))
                .filter(exportStrategy -> requestTransportType.equals(exportStrategy.getRequestTransportType()))
                .findFirst()
                .orElseThrow(() -> {
                    String errMsg = String.format("Failed to find export strategy by import tool type '%s' and request "
                            + "transport type '%s'", importToolType, requestTransportType);
                    log.error(errMsg);
                    return new IllegalStateException(errMsg);
                });
    }
}
