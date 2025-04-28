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

package org.qubership.atp.itf.lite.backend.model.api.kafka;

import java.util.UUID;

import org.qubership.atp.itf.lite.backend.model.api.kafka.entities.HttpRequestExportEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MiaExportRequestEvent extends ExportRequestEvent {

    private String miaPath;
    private String miaProcessName;
    private HttpRequestExportEntity request;

    /**
     * Constructor with all arguments.
     * @param id export id
     * @param projectId projectId
     * @param miaPath mia path
     * @param miaProcessName mia process name (agreed that it's request name)
     * @param request request
     */
    public MiaExportRequestEvent(UUID id, UUID projectId, String miaPath, String miaProcessName,
                                 HttpRequestExportEntity request) {
        super(id, projectId);
        this.miaPath = miaPath;
        this.miaProcessName = miaProcessName;
        this.request = request;
    }
}
