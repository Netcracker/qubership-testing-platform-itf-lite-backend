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

import static org.qubership.atp.itf.lite.backend.utils.Constants.ATP_ITF_LITE_ROOT_REQUESTS;
import static org.qubership.atp.itf.lite.backend.utils.Constants.COLLECTION;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.exceptions.ExportException;
import org.qubership.atp.ei.node.services.FileService;
import org.qubership.atp.ei.node.services.ObjectSaverToDiskService;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.FolderRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestRepository;
import org.qubership.atp.itf.lite.backend.model.ei.ToPostmanBody;
import org.qubership.atp.itf.lite.backend.model.ei.ToPostmanFile;
import org.qubership.atp.itf.lite.backend.model.ei.ToPostmanItem;
import org.qubership.atp.itf.lite.backend.model.ei.ToPostmanMode;
import org.qubership.atp.itf.lite.backend.model.ei.ToPostmanModel;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileInfo;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.service.GridFsService;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostmanExportStrategy implements ExportStrategy {
    private final RequestRepository requestRepository;
    private final FolderRepository folderRepository;
    private final ObjectSaverToDiskService objectSaverToDiskService;
    private final FileService fileService;
    private final GridFsService gridFsService;

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.POSTMAN;
    }

    @Override
    public void export(ExportImportData exportData, Path path) {
        exportFolders(exportData, path);
        exportRequests(exportData, path);
    }

    @Override
    public void exportFolders(Set<UUID> exportFolderIds, Path workDir, UUID projectId) {
        List<Folder> folders = folderRepository.findAllByIdInOrderByOrder(exportFolderIds);
        if (!CollectionUtils.isEmpty(folders)) {
            folders.forEach(folder -> {
                ToPostmanModel postmanModel = new ToPostmanModel(folder);
                exportChildFoldersToPostman(folder, postmanModel.getItems(), projectId, workDir);
                objectSaverToDiskService.writeAtpEntityToFile(folder.getName() + "." + COLLECTION, postmanModel,
                        COLLECTION, workDir, true);
            });
        }
    }

    @Override
    public void exportRequests(ExportImportData exportData, Path workDir, Set<UUID> exportRequestIds)
            throws ExportException {
        if (!exportRequestIds.isEmpty()) {
            UUID projectId = exportData.getProjectId();
            List<Request> requests = requestRepository.findAllByProjectIdAndIdInOrderByOrder(projectId,
                    exportRequestIds);
            if (requests != null) {
                requests = requests.stream().filter(r -> r.getFolderId() == null).collect(Collectors.toList());
                if (!requests.isEmpty()) {
                    String folderName = ATP_ITF_LITE_ROOT_REQUESTS;
                    ToPostmanModel postmanModel = new ToPostmanModel(folderName);
                    exportRequestsToPostman(requests, postmanModel.getItems(), workDir);
                    objectSaverToDiskService.writeAtpEntityToFile(folderName + "." + COLLECTION, postmanModel,
                            COLLECTION, workDir, true);
                }
            }
        }
    }

    private void exportChildFoldersToPostman(Folder parentFolder, List<ToPostmanItem> item, UUID projectId,
                                             Path workDir) {
        List<Folder> childFolders = folderRepository.findAllByProjectIdAndParentIdOrderByOrder(projectId,
                parentFolder.getId());
        if (childFolders != null && !childFolders.isEmpty()) {
            childFolders.forEach(folder -> {
                ToPostmanItem childItem = new ToPostmanItem(folder);
                item.add(childItem);
                exportChildFoldersToPostman(folder, childItem.getItems(), projectId, workDir);
            });
        }
        exportRequestsToPostman(requestRepository.findAllByFolderIdOrderByOrder(parentFolder.getId()), item, workDir);
    }

    private void exportRequestsToPostman(List<Request> requests, List<ToPostmanItem> item, Path workDir) {
        if (requests != null && !requests.isEmpty()) {
            requests.forEach(request -> {
                if (request instanceof HttpRequest) {
                    ToPostmanItem requestToPostman = new ToPostmanItem((HttpRequest) request);
                    Path workDirForFiles = workDir.resolve(COLLECTION).resolve(Constants.FILES);
                    fileService.createDirectory(workDirForFiles);

                    ToPostmanBody body = requestToPostman.getRequest().getBody();
                    if (body != null) {
                        ToPostmanMode mode = body.getMode();
                        if (ToPostmanMode.FILE.equals(mode)) {
                            body.setFile(exportBinaryFileToPostman(request.getId(), workDirForFiles));
                        }
                        if (ToPostmanMode.FORMDATA.equals(mode)) {
                            exportFormDataFilesToPostman(request.getId(), workDirForFiles);
                        }
                    }

                    item.add(requestToPostman);
                }
            });
        }
    }

    private ToPostmanFile exportBinaryFileToPostman(UUID requestId, Path workDir) {
        log.info("Start export binary files to POSTMAN for request {}", requestId);
        FileInfo fileInfo = gridFsService.getFileInfoByRequestId(requestId);
        Path requestPath = workDir.resolve(requestId.toString());
        fileService.createDirectory(requestPath);
        String fileName = fileInfo.getFileName();
        Path filePath = requestPath.resolve(fileName);
        ToPostmanFile toPostmanFile = new ToPostmanFile(COLLECTION + "/" + Constants.FILES + "/" + requestId + "/"
                + fileName);
        try (InputStream in = gridFsService.getFileByFileInfo(fileInfo)) {
            Files.copy(in, filePath);
        } catch (IOException e) {
            log.error("Error when writing file {} for Request {} to the file system.", fileName, requestId, e);
        }
        log.info("End export files for requests {}", requestId);
        return toPostmanFile;
    }

    private void exportFormDataFilesToPostman(UUID requestId, Path workDir) {
        log.info("Start export form data files to POSTMAN for request {}", requestId);
        Path requestPath = workDir.resolve(requestId.toString());
        fileService.createDirectory(requestPath);
        gridFsService.getFileInfosByRequestId(requestId).forEach(fileInfo -> {
            String fileName = fileInfo.getFileName();
            Path filePath = requestPath.resolve(fileName);
            try (InputStream in = gridFsService.getFileByFileInfo(fileInfo)) {
                Files.copy(in, filePath);
            } catch (IOException e) {
                log.error("Error when writing file {} for Request {} to the file system.", fileName, requestId, e);
            }
        });
        log.info("End export files for requests {}", requestId);
    }
}
