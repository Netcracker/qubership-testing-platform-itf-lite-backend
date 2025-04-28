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

package org.qubership.atp.itf.lite.backend.ei.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.itf.lite.backend.exceptions.folders.ItfLiteImportFolderFileLoadException;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.service.FolderService;
import org.qubership.atp.itf.lite.backend.utils.StreamUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderImporterService {

    private final ObjectLoaderFromDiskService objectLoaderFromDiskService;
    private final FolderService folderService;

    /**
     * Imports folders.
     *
     * @param workDir    directory where folder's files store
     * @param importData data about imported objects
     */
    public void importFolders(Path workDir, ExportImportData importData) {
        Map<UUID, Path> folderFiles =
                objectLoaderFromDiskService.getListOfObjects(workDir, Folder.class);
        Map<UUID, UUID> replacementMap = importData.getReplacementMap();
        log.debug("importFolders list: {}", folderFiles);
        boolean isReplacement = importData.isInterProjectImport() || importData.isCreateNewProject();
        List<Folder> parsedFolders = new ArrayList<>();
        folderFiles.forEach((folderId, filePath) -> {
            log.debug("importFolders starts import: {}.", folderId);

            Folder folderObject = load(filePath, replacementMap, isReplacement);
            log.debug("Imports folder:{}", folderObject);
            if (folderObject == null) {
                final String path = filePath.toString();
                log.error("Failed to upload file using path: {}", filePath);
                throw new ItfLiteImportFolderFileLoadException(path);
            }

            folderObject.setSourceId(folderId);
            parsedFolders.add(folderObject);
        });

        Map<UUID, List<Folder>> foldersGroupedByParentFolders = parsedFolders.stream()
                .filter(folder -> nonNull(folder.getParentId()))
                .collect(Collectors.groupingBy(Folder::getParentId));
        for (Map.Entry<UUID, List<Folder>> foldersByParentId : foldersGroupedByParentFolders.entrySet()) {
            // list is not empty or null
            UUID projectId = foldersByParentId.getValue().get(0).getProjectId();
            List<Folder> destinationFolders = folderService.getAllByProjectIdAndParentId(
                    projectId, foldersByParentId.getKey());
            foldersByParentId.getValue().forEach(folderObject -> {
                List<Folder> foldersWithoutCurrentFolder = foldersByParentId.getValue()
                        .stream()
                        .filter(folder -> !folder.equals(folderObject))
                        .collect(Collectors.toList());
                foldersWithoutCurrentFolder.addAll(destinationFolders);
                foldersWithoutCurrentFolder = foldersWithoutCurrentFolder
                        .stream()
                        .filter(StreamUtils.distinctByKey(Folder::getId))
                        .collect(Collectors.toList());
                folderService.addPostfixIfFolderNameInDestinationIsTaken(foldersWithoutCurrentFolder, folderObject);
            });
        }

        List<Folder> rootFolderFolderObjects = parsedFolders.stream()
                .filter(folder -> isNull(folder.getParentId()))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(rootFolderFolderObjects)) {
            UUID projectId = rootFolderFolderObjects.get(0).getProjectId();
            List<Folder> rootFolderFolders = folderService.getAllByProjectIdAndParentId(projectId, null);
            rootFolderFolderObjects.forEach(folderObject -> {
                List<Folder> foldersWithoutCurrentFolder = rootFolderFolderObjects
                        .stream()
                        .filter(folder -> !folder.equals(folderObject))
                        .collect(Collectors.toList());
                foldersWithoutCurrentFolder.addAll(rootFolderFolders);
                foldersWithoutCurrentFolder = foldersWithoutCurrentFolder
                        .stream()
                        .filter(StreamUtils.distinctByKey(Folder::getId))
                        .collect(Collectors.toList());
                folderService.addPostfixIfFolderNameInDestinationIsTaken(foldersWithoutCurrentFolder, folderObject);
            });
        }

        // add all folders grouped by parent folders
        rootFolderFolderObjects.addAll(foldersGroupedByParentFolders.entrySet().stream().flatMap(uuidListEntry ->
                        uuidListEntry.getValue().stream())
                .collect(Collectors.toList()));
        folderService.saveAll(rootFolderFolderObjects);
    }

    private Folder load(Path filePath, Map<UUID, UUID> replacementMap, boolean isReplacement) {
        if (isReplacement) {
            log.debug("Load folder by path [{}] with replacementMap: {}", filePath, replacementMap);
            return objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(filePath, Folder.class,
                    replacementMap, true, false);
        } else {
            log.debug("Load folder by path [{}] without replacementMap", filePath);
            return objectLoaderFromDiskService.loadFileAsObject(filePath, Folder.class);
        }
    }

    /**
     * Gets existing by source id.
     *
     * @param workDir        work directory
     * @param replacementMap replacement map for object loader
     * @return the existing by source id
     */
    public Map<UUID, UUID> getSourceTargetMap(final Path workDir, final Map<UUID, UUID> replacementMap) {
        log.debug("Get source target replacement map");
        Map<UUID, UUID> result = new HashMap<>();
        Map<UUID, Path> objectsToImport = objectLoaderFromDiskService.getListOfObjects(workDir, Folder.class);

        objectsToImport.forEach((uuid, filePath) -> {
            Folder object = objectLoaderFromDiskService
                    .loadFileAsObjectWithReplacementMap(filePath, Folder.class, replacementMap);
            Folder existingObject = folderService.getByProjectIdAndSourceId(object.getProjectId(), uuid);
            if (existingObject == null) {
                log.debug("Folder by projectId: [{}] and sourceId: [{}] not found", object.getProjectId(), uuid);
                log.debug("Put {}: null to replacementMap", uuid);
                result.put(uuid, null);
            } else {
                log.debug("Folder by projectId: [{}] and sourceId: [{}] found", object.getProjectId(), uuid);
                log.debug("Put {}: {} to replacementMap", uuid, existingObject.getId());
                result.put(uuid, existingObject.getId());
            }
        });
        return result;
    }

}
