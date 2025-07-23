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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.qubership.atp.ei.node.constants.Constant;
import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.exceptions.ExportException;
import org.qubership.atp.ei.node.services.FileService;
import org.qubership.atp.ei.node.services.ObjectSaverToDiskService;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.FolderRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestRepository;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileInfo;
import org.qubership.atp.itf.lite.backend.service.GridFsService;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AtpExportStrategy implements ExportStrategy {

    private final RequestRepository requestRepository;
    private final FolderRepository folderRepository;
    private final ObjectSaverToDiskService objectSaverToDiskService;
    private final FileService fileService;
    private final GridFsService gridFsService;
    private final ObjectMapper objectMapper;

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.ATP;
    }

    @Override
    public void export(ExportImportData exportData, Path path) {
        exportFolders(exportData, Paths.get(path.toString(), Constants.FOLDERS));
        exportRequests(exportData, Paths.get(path.toString(), Constants.REQUESTS));
    }

    @Override
    public void exportFolders(Set<UUID> exportFolderIds, Path workDir, UUID projectId) {
        Map<UUID, Folder> folders = folderRepository.findAllByProjectId(projectId)
                .stream().collect(Collectors.toMap(Folder::getId, Function.identity()));

        Set<UUID> parentFolders = new HashSet<>();
        exportFolderIds.forEach(folderId -> parentFolders.addAll(getParentFolders(folders, folderId)));

        folders.forEach((folderId, folder) -> {
            if (parentFolders.contains(folderId)) {
                final UUID originalPermissionFolderId = folder.getPermissionFolderId();
                final String originalPermissions = folder.getPermission();
                folder.setPermissionFolderId(null);
                folder.setPermission(null);
                try {
                    objectSaverToDiskService.exportAtpEntity(folderId, folder, workDir);
                } finally {
                    folder.setPermissionFolderId(originalPermissionFolderId);
                    folder.setPermission(originalPermissions);
                }
            }
        });
    }

    @Override
    public void exportRequests(ExportImportData exportData, Path workDir, Set<UUID> exportRequestIds)
            throws ExportException {
        exportData.getExportScope().getEntities().getOrDefault(Constant.ENTITY_PROJECTS, new HashSet<>())
                .forEach(projectId ->
                        requestRepository.findAllByProjectId(UUID.fromString(projectId)).stream()
                                .filter(request -> exportRequestIds.contains(request.getId()))
                                .forEach(request ->
                                        objectSaverToDiskService.exportAtpEntity(request.getId(), request, workDir))
                );
        exportFiles(exportRequestIds, workDir.getParent());
    }

    private Set<UUID> getParentFolders(Map<UUID, Folder> folders, UUID childFolderId) {
        Set<UUID> parentFolders = new HashSet<>();
        Folder childFolder = folders.get(childFolderId);
        if (childFolder == null) {
            return parentFolders;
        }
        parentFolders.add(childFolderId);
        parentFolders.addAll(getParentFolders(folders, childFolder.getParentId()));
        return parentFolders;
    }

    private void exportFiles(Set<UUID> exportRequestIds, Path workDir) {
        log.debug("Start export files for requests {}", exportRequestIds);
        Path fileDirectoryPath = workDir.resolve(Constants.FILES);
        Map<UUID, List<FileInfo>> files = gridFsService.getFileInfosByRequestIds(exportRequestIds);
        for (Map.Entry<UUID, List<FileInfo>> entry : files.entrySet()) {
            UUID requestId = entry.getKey();
            List<FileInfo> fileInfos = entry.getValue();

            Path requestDirectoryPath = fileDirectoryPath.resolve(requestId.toString());
            fileService.createDirectory(requestDirectoryPath);

            Map<UUID, InputStream> fileToFileId = gridFsService.getFilesByFileInfos(fileInfos);
            for (Map.Entry<UUID, InputStream> fileEntry : fileToFileId.entrySet()) {
                String fileName = fileEntry.getKey().toString();
                Path filePath = requestDirectoryPath.resolve(fileName);
                try (InputStream in = fileEntry.getValue()) {
                    Files.copy(in, filePath);
                } catch (IOException e) {
                    log.error("Error when writing file {} for Request {} to the file system.", fileName, requestId, e);
                }

                Path metadataFilePath = requestDirectoryPath.resolve(fileName + ".json");
                Optional<FileInfo> fileInfo = fileInfos.stream()
                        .filter(info -> fileName.equals(info.getFileId().toString())).findFirst();
                if (fileInfo.isPresent()) {
                    try {
                        objectMapper.writeValue(metadataFilePath.toFile(), fileInfo.get());
                    } catch (IOException e) {
                        log.error("Error when writing file info with fileId {} for Request {} to the file system.",
                                fileName, requestId, e);
                    }
                }
            }
        }
        log.debug("End export files for requests {}", exportRequestIds);
    }
}
