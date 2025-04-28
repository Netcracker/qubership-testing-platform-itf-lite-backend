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

package org.qubership.atp.itf.lite.backend.ei;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.ei.node.ImportExecutor;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.itf.lite.backend.ei.service.FolderImporterService;
import org.qubership.atp.itf.lite.backend.ei.service.RequestImporterService;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AtpItfLiteImportExecutor implements ImportExecutor {

    private final FolderImporterService folderImporterService;
    private final RequestImporterService requestImporterService;
    private final ObjectLoaderFromDiskService objectLoaderFromDiskService;

    @Override
    public void importData(ExportImportData exportImportData, Path path) throws RuntimeException {
        log.info("Start import. Data: {}, WorkDir: {}", exportImportData, path);
        folderImporterService.importFolders(Paths.get(path.toString(), Constants.FOLDERS), exportImportData);
        requestImporterService.importRequests(Paths.get(path.toString(), Constants.REQUESTS), exportImportData);
        requestImporterService.importFiles(exportImportData, path);
        log.info("End of import");
    }

    @Override
    public ValidationResult preValidateData(ExportImportData exportImportData, Path path) {
        return null;
    }

    @Override
    public ValidationResult validateData(ExportImportData exportImportData, Path path) {
        log.info("Start validateData. Data: {}, WorkDir: {}", exportImportData, path);

        Map<UUID, UUID> replacementMap = new HashMap<>(exportImportData.getReplacementMap());
        replacementMap.putAll(requestImporterService
                .getReplacementMap(getWorkDir(path.toString(), Constants.REQUESTS)));

        if (exportImportData.isCreateNewProject()) {
            Set<UUID> ids = objectLoaderFromDiskService.getListOfObjects(
                    getWorkDir(path.toString(), Constants.FOLDERS), Folder.class).keySet();
            ids.addAll(objectLoaderFromDiskService.getListOfObjects(
                    getWorkDir(path.toString(), Constants.REQUESTS), Request.class).keySet());
            ids.forEach(id -> replacementMap.put(id, UUID.randomUUID()));
        } else if (exportImportData.isInterProjectImport()) {
            replacementMap.putAll(folderImporterService.getSourceTargetMap(
                    getWorkDir(path.toString(), Constants.FOLDERS), replacementMap));
            replacementMap.putAll(requestImporterService.getSourceTargetMap(
                    getWorkDir(path.toString(), Constants.REQUESTS), replacementMap));
            replacementMap.entrySet().forEach(entry -> {
                if (entry.getValue() == null) {
                    entry.setValue(UUID.randomUUID());
                }
            });
        }
        replacementMap.entrySet().forEach(entry -> {
            if (entry.getValue() == null) {
                entry.setValue(UUID.randomUUID());
            }
        });
        return new ValidationResult(null, replacementMap);
    }

    private Path getWorkDir(String path, String entityType) {
        return Paths.get(path.toString(), entityType);
    }
}
