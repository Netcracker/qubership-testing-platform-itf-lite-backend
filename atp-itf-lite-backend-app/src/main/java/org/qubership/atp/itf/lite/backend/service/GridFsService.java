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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.qubership.atp.itf.lite.backend.dataaccess.repository.GridFsRepository;
import org.qubership.atp.itf.lite.backend.model.entities.FileBody;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GridFsService {

    private final GridFsRepository repository;

    @Autowired
    public GridFsService(GridFsRepository repository) {
        this.repository = repository;
    }

    /**
     * Find dictionary in storage and return its data.
     *
     * @param requestId request id
     * @return data of the file
     */
    public Optional<FileData> downloadFile(UUID requestId) {
        return repository.getRequestFileData(requestId);
    }

    /**
     * Find file in storage by file id and return its data.
     *
     * @param fileId file id
     * @return data of the file
     */
    public Optional<FileData> downloadFileByFileId(UUID fileId) {
        return repository.getFileDataByFileId(fileId);
    }

    /**
     * Find file in storage by session id and request id.
     *
     * @param sessionId session id
     * @param sessionRequestId request id
     * @return data of the file
     */
    public Optional<FileData> downloadFileBySessionIdAndRequestId(UUID sessionId, UUID sessionRequestId) {
        return repository.getFileDataBySessionIdAndRequestId(sessionId, sessionRequestId);
    }

    public void removeFileByRequestId(UUID requestId) {
        repository.removeFileByRequestId(requestId);
    }

    /**
     * Remove all files by request id.
     * @param requestId request id.
     */
    public void removeAllFilesByRequestId(UUID requestId) {
        repository.removeAllFilesByRequestId(requestId);
    }

    /**
     * Remove all files by session id and session request id.
     * @param sessionId session id.
     * @param requestId request id.
     */
    public void removeAllFilesBySessionIdAndSessionRequestId(UUID sessionId, UUID requestId) {
        repository.removeAllFilesBySessionIdAndSessionRequestId(sessionId, requestId);
    }

    /**
     * Remove all files for session requests.
     * @param sessionId session Id.
     * @param requestIds request Ids.
     */
    public void bulkRemoveFilesBySnapshotKeys(UUID sessionId, List<UUID> requestIds) {
        repository.bulkRemoveFilesBySnapshotKeys(sessionId, requestIds);
    }

    public void removeFileByFileId(UUID fileId) {
        repository.removeFileByFileId(fileId);
    }

    /**
     * Remove only binary files from gridfs by request ids.
     * @param requestId request id.
     */
    public void removeBinaryFileByRequestId(UUID requestId) {
        repository.removeBinaryFileByRequestId(requestId);
    }

    public void removeFilesByDate(Integer days) {
        repository.removeFilesByDate(days);
    }

    public UUID saveDictionaryByRequestId(String creationTime, UUID requestId,
                                          InputStream dictionaryInputStream, String dictionaryName) {
        return repository.saveDictionaryByRequestId(creationTime, requestId, dictionaryInputStream,
                dictionaryName);
    }

    /**
     * Save dictionary file by session id and request id.
     */
    public UUID saveDictionaryBySessionIdAndSessionRequestId(String creationTime,
                                                             UUID sessionId,
                                                             UUID sessionRequestId,
                                                             InputStream dictionaryInputStream,
                                                             String dictionaryName) {
        return repository.saveDictionaryBySessionIdAndSessionRequestId(creationTime, sessionId,
                sessionRequestId,
                dictionaryInputStream,
                dictionaryName);
    }

    public void saveFileByRequestId(String creationTime, UUID requestId,
                                    InputStream fileInputStream, String fileName, UUID fileId) {
        repository.saveFileByRequestId(creationTime, requestId, fileInputStream, fileName, fileId);
    }

    public void saveFileBySessionId(String creationTime, UUID sessionId,
                                    InputStream fileInputStream, String fileName, UUID fileId) {
        repository.saveFileBySessionId(creationTime, sessionId, fileInputStream, fileName, fileId);
    }

    /**
     * Get list of file data.
     *
     * @param requestId for find files
     * @return list
     */
    public List<FileData> getFilesDataByRequestId(UUID requestId) {
        return repository.getFilesDataList(requestId);
    }

    /**
     * Copies file with specified id.
     * If newRequestId is specified, it copies to the new request, otherwise it copies to the current request
     *
     * @param fileId file id to copy
     * @param newRequestId target request
     * @return newFileId or null if file not found by fileId
     */
    public UUID copyFileById(UUID fileId, @Nullable UUID newRequestId) {
        return repository.copyFileWithFileId(fileId, newRequestId);
    }

    public FileBody saveBinaryByRequestId(String creationTime, UUID requestId,
                                          InputStream inputStream, String name, String contentType) {
        return repository.saveBinaryByRequestId(creationTime, requestId, inputStream, name, contentType);
    }

    /**
     * Save binary file by session id and request id.
     */
    public FileBody saveBinaryBySessionId(String creationTime, UUID sessionId, UUID sessionRequestId,
                                          InputStream inputStream, String name, String contentType) {
        return repository.saveBinaryBySessionIdAndSessionRequestId(creationTime, sessionId,
                sessionRequestId, inputStream,
                name, contentType);
    }

    public UUID saveHistoryBinary(String creationTime, InputStream inputStream, String name) {
        return repository.saveHistoryBinary(creationTime, inputStream, name);
    }

    public void saveByFileInfo(FileInfo fileInfo, InputStream inputStream) {
        repository.saveByFileInfo(fileInfo, inputStream);
    }

    /**
     * Get map with key is request id and value is list of file infos.
     * @param requestIds request ids.
     * @return {@link java.util.HashMap}
     */
    public Map<UUID, List<FileInfo>> getFileInfosByRequestIds(Set<UUID> requestIds) {
        return repository.getFileInfosByRequestIds(requestIds);
    }

    /**
     * Get file infos.
     * @param requestId request id.
     * @return list of  {@link FileInfo}
     */
    public List<FileInfo> getFileInfosByRequestId(UUID requestId) {
        return repository.getFileInfosByRequestId(requestId);
    }

    /**
     * Get map with key is file id and value is file input stream.
     * @param fileInfos info about files.
     * @return {@link java.util.HashMap}
     */
    public Map<UUID, InputStream> getFilesByFileInfos(List<FileInfo> fileInfos) {
        return repository.getFileByFileInfos(fileInfos);
    }

    /**
     * Get file info by request id.
     * @param requestId request ids.
     * @return {@link java.util.HashMap}
     */
    public FileInfo getFileInfoByRequestId(UUID requestId) {
        return repository.getFileInfoByRequestId(requestId);
    }

    /**
     * Get file info by request id.
     * @param fileId file id.
     * @return {@link java.util.HashMap}
     */
    public FileInfo getFileInfoByFileId(UUID fileId) {
        return repository.getFileInfoByFileId(fileId);
    }

    /**
     * Get map with key is file id and value is file input stream.
     * @param fileInfo info about files.
     * @return {@link InputStream}
     */
    public InputStream getFileByFileInfo(FileInfo fileInfo) {
        return repository.getFileByFileInfo(fileInfo);
    }

    public void moveFileFromSnapshotToRequest(UUID sessionId, UUID requestId) {
        repository.moveFileFromSnapshotToRequest(sessionId, requestId);
    }

    public void removeFileBySessionId(UUID sessionId) {
        repository.removeFileBySessionId(sessionId);
    }
}
