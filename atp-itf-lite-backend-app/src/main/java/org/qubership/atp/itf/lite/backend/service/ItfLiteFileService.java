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

package org.qubership.atp.itf.lite.backend.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.qubership.atp.itf.lite.backend.components.replacer.ContextVariablesReplacer;
import org.qubership.atp.itf.lite.backend.model.context.SaveRequestResolvingContext;
import org.qubership.atp.itf.lite.backend.model.entities.FileBody;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.qubership.atp.itf.lite.backend.utils.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItfLiteFileService {
    private final GridFsService gridFsService;
    private final ContextVariablesReplacer replacer;
    private final Tika fileDetector = new Tika();

    @Value("${atp.itf.lite.clean.file.cache.time-sec:3600}")
    private String cleanFileCacheTimeout;

    /**
     * Get binary file as {@link FileData} from cache or gridfs by request id.
     * @param requestId request id.
     * @param modifiedWhen request modified date.
     * @param folder path to folder.
     * @return {@link FileData} with content and file name.
     */
    public FileData getRequestFileData(UUID requestId, Date modifiedWhen, Path folder) throws IOException {
        Optional<Path> requestFilePath = getPathToFile(requestId, modifiedWhen, folder);
        if (requestFilePath.isPresent()) {
            Path path = requestFilePath.get();
            String type = fileDetector.detect(path);
            return new FileData(Files.readAllBytes(path), path.getFileName().toString(), type);
        } else {
            Optional<FileData> fileDataOptional = gridFsService.downloadFile(requestId);
            if (fileDataOptional.isPresent()) {
                FileData fileData = fileDataOptional.get();
                FileUtils.saveFileDataDictionaryToFileSystem(folder, requestId, fileData);
                return fileData;
            }
        }
        return null;
    }

    /**
     * Get binary file as {@link FileData} from cache or gridfs by request id.
     * @param fileId file id.
     * @param modifiedWhen request modified date.
     * @param folder path to folder.
     * @return {@link FileData} with content and file name.
     */
    public FileData getFileDataById(UUID fileId, Date modifiedWhen, Path folder) throws IOException {
        Optional<Path> requestFilePath = getRequestPathToFileById(fileId, modifiedWhen, folder);
        if (requestFilePath.isPresent()) {
            return new FileData(Files.readAllBytes(requestFilePath.get()),
                    requestFilePath.get().getFileName().toString());
        }
        return null;
    }

    /**
     * Resolve parameters in multipart file body.
     * @param file Multipart file.
     * @param resolvingContext context.
     * @return array of bytes.
     */
    @Nullable
    public byte[] resolveParametersInMultipartFile(MultipartFile file, SaveRequestResolvingContext resolvingContext) {
        try {
            Optional<String> fileBody = FileUtils.readFileToString(file);
            if (fileBody.isPresent()) {
                return replacer.replace(fileBody.get(), resolvingContext.mergeScopes()).getBytes();
            }
            return file.getBytes();
        } catch (IOException e) {
            log.error(" Failed to read file with name '{}' and content type '{}' for resolving parameters",
                    file.getOriginalFilename(), file.getContentType());
        }
        log.warn("File '{}' body is empty.", file.getOriginalFilename());
        return null;
    }

    /**
     * Get binary file as {@link MultipartFile} from cache or gridfs by request id.
     * @param requestId request id.
     * @param modifiedWhen request modified date.
     * @param folder path to folder.
     * @return {@link MultipartFile}
     */
    public Optional<MultipartFile> getFileAsMultipartFileByRequestId(UUID requestId, Date modifiedWhen, Path folder)
            throws IOException {
        FileData fileData = getRequestFileData(requestId, modifiedWhen, folder);
        if (fileData == null) {
            return Optional.empty();
        }
        try {
            DiskFileItem fileItem = (DiskFileItem) new DiskFileItemFactory()
                    .createItem(null, fileData.getContentType(), true, fileData.getFileName());
            IOUtils.copy(new ByteArrayInputStream(fileData.getContent()), fileItem.getOutputStream());
            return Optional.of(new CommonsMultipartFile(fileItem));
        } catch (Exception exception) {
            log.warn("Can't read file with name {} by request id {}", fileData.getFileName(), requestId);
            return Optional.empty();
        }
    }

    /**
     * Get path to file in file system by request id.
     * @param requestId request id.
     * @param modifiedWhen request modified date.
     * @param folder path to folder.
     * @return {@link MultipartFile}
     */
    public Optional<Path> getRequestPathToFile(UUID requestId, Date modifiedWhen, Path folder) throws IOException {
        Optional<Path> requestFilePath = getPathToFile(requestId, modifiedWhen, folder);
        if (requestFilePath.isPresent()) {
            return requestFilePath;
        }

        Optional<FileData> fileDataOptional = gridFsService.downloadFile(requestId);
        if (fileDataOptional.isPresent()) {
            FileData fileData = fileDataOptional.get();
            return Optional.of(FileUtils.saveFileDataDictionaryToFileSystem(folder, requestId, fileData));
        }
        return Optional.empty();
    }

    /**
     * Get path to file in file system by request id.
     * @param fileId file id.
     * @param modifiedWhen request modified date.
     * @param folder path to folder.
     * @return {@link MultipartFile}
     */
    public Optional<Path> getRequestPathToFileById(UUID fileId, Date modifiedWhen, Path folder) throws IOException {
        Path requestFilePath = Paths.get(folder.toString(), fileId.toString());

        if (Files.exists(requestFilePath)) {
            Optional<Path> filePathOptional =
                    Files.list(requestFilePath).filter(file -> !Files.isDirectory(file)).findFirst();
            if (filePathOptional.isPresent()
                    && !needUpdateFileFromGridFs(filePathOptional.get(), modifiedWhen)) {
                return filePathOptional;
            }
        }

        Optional<FileData> fileDataOptional = gridFsService.downloadFileByFileId(fileId);
        if (fileDataOptional.isPresent()) {
            FileData fileData = fileDataOptional.get();
            return Optional.of(FileUtils.saveFileDataDictionaryToFileSystem(folder, fileId, fileData));
        }
        return Optional.empty();
    }

    /**
     * Copy file for new copied request.
     * @param oldRequestId old request id.
     * @param modifiedWhen modification date fot old request.
     * @param newRequestId id for new saved request.
     * @param folder path to folder with files.
     */
    public Optional<FileBody> copyFileForCopiedRequest(UUID oldRequestId, Date modifiedWhen,
                                         UUID newRequestId, Path folder) throws IOException {
        Optional<Path> oldPathToFile = getRequestPathToFile(oldRequestId, modifiedWhen, folder);
        if (oldPathToFile.isPresent()) {
            Path newPathToDirectory = Paths.get(folder.toString(), newRequestId.toString());
            Files.createDirectories(newPathToDirectory);

            String fileName = oldPathToFile.get().getFileName().toString();
            Path newPathToFile = Paths.get(newPathToDirectory.toString(), fileName);
            Files.copy(oldPathToFile.get(), newPathToFile);
            log.info("File by path '{}' was copied to '{}'", oldPathToFile.get(), newPathToFile);


            FileBody fileInfo = gridFsService.saveBinaryByRequestId(LocalDateTime.now().toString(), newRequestId,
                    Files.newInputStream(newPathToFile.toFile().toPath()), fileName,
                    Files.probeContentType(newPathToFile));
            log.debug("File for request {} was saved with parameters {}", newRequestId, fileInfo);
            return Optional.of(fileInfo);
        }
        return Optional.empty();
    }

    /**
     * Upload file to gridfs and file system by request id.
     * @param requestId request id.
     * @param folder files directory.
     * @param file file for upload.
     */
    public FileBody uploadFileForRequest(UUID requestId, Path folder, MultipartFile file)
            throws IOException {
        FileUtils.prepareDirectoriesBeforeSave(folder, requestId);
        FileUtils.saveMultipartFileDictionaryToFileSystem(folder, requestId, file);

        gridFsService.removeBinaryFileByRequestId(requestId);
        FileBody fileInfo = gridFsService.saveBinaryByRequestId(LocalDateTime.now().toString(), requestId,
                file.getInputStream(), file.getOriginalFilename(), file.getContentType());
        log.debug("File for request {} was saved with parameters {}", requestId, fileInfo);
        return fileInfo;
    }

    private Optional<Path> getPathToFile(UUID requestId, Date modifiedWhen, Path folder) throws IOException {
        Path requestFilePath = Paths.get(folder.toString(), requestId.toString());

        if (Files.exists(requestFilePath)) {
            Optional<Path> filePathOptional =
                    Files.list(requestFilePath).filter(file -> !Files.isDirectory(file)).findFirst();
            if (filePathOptional.isPresent()
                    && !needUpdateFileFromGridFs(filePathOptional.get(), modifiedWhen)) {
                return filePathOptional;
            }
        }
        return Optional.empty();
    }

    private boolean needUpdateFileFromGridFs(Path filePath, Date requestModifiedWhen) {
        File file = filePath.toFile();
        if (file.exists()) {
            long lastModifiedFile = file.lastModified();
            return requestModifiedWhen.after(new Date(lastModifiedFile))
                    || new Date().after(new Date(lastModifiedFile + Integer.parseInt(cleanFileCacheTimeout) * 1000L));
        }
        return false;
    }
}
