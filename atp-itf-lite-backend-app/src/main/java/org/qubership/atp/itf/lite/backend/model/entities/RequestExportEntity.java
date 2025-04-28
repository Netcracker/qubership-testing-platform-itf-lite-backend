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

package org.qubership.atp.itf.lite.backend.model.entities;

import java.util.Map;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.qubership.atp.itf.lite.backend.enums.RequestExportStatus;
import org.qubership.atp.itf.lite.backend.model.entities.converters.RequestExportHashMapConverter;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "request_export")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class RequestExportEntity extends AbstractNamedEntity {

    @Column(name = "request_export_id")
    private UUID requestExportId;

    @Column(name = "sse_id")
    private UUID sseId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "request_statuses", columnDefinition = "TEXT")
    @Convert(converter = RequestExportHashMapConverter.class)
    private Map<UUID, RequestExportStatus> requestStatuses;

    @Column(name = "destination")
    private String destination;
}
