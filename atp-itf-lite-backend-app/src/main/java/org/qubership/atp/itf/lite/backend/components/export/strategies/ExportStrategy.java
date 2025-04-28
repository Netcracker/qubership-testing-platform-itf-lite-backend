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

package org.qubership.atp.itf.lite.backend.components.export.strategies;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ei.node.constants.Constant;
import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.exceptions.ExportException;
import org.qubership.atp.itf.lite.backend.ei.ServiceScopeEntities;

public interface ExportStrategy {

    ExportFormat getFormat();

    void export(ExportImportData exportData, Path path);

    /**
     * Export Folders by path.
     */
    default void exportFolders(ExportImportData exportData, Path workDir) throws ExportException {
        Set<UUID> exportFolderIds = exportData.getExportScope().getEntities()
                .getOrDefault(ServiceScopeEntities.ENTITY_ITF_LITE_FOLDERS.getValue(), new HashSet<>())
                .stream()
                .map(UUID::fromString)
                .collect(Collectors.toSet());
        exportData.getExportScope().getEntities().getOrDefault(Constant.ENTITY_PROJECTS, new HashSet<>())
                .forEach(projectId -> exportFolders(exportFolderIds, workDir, UUID.fromString(projectId)));
    }

    void exportFolders(Set<UUID> exportFolderIds, Path workDir, UUID projectId);

    /**
     * Export requests by path.
     */
    default void exportRequests(ExportImportData exportData, Path workDir) throws ExportException {
        exportRequests(exportData, workDir, exportData.getExportScope().getEntities()
                .getOrDefault(ServiceScopeEntities.ENTITY_ITF_LITE_REQUESTS.getValue(), new HashSet<>())
                .stream()
                .map(UUID::fromString)
                .collect(Collectors.toSet()));
    }

    void exportRequests(ExportImportData exportData, Path workDir, Set<UUID> exportRequestIds) throws ExportException;
}
