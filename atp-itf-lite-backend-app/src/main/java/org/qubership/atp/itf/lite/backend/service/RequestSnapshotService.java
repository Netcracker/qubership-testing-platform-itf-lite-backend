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

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.transaction.Transactional;

import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestSnapshotRepository;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.model.api.request.BulkDeleteSnapshotsRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.FileInfoResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestSnapshotResponse;
import org.qubership.atp.itf.lite.backend.model.entities.FileBody;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.RequestSnapshot;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileInfo;
import org.qubership.atp.itf.lite.backend.model.entities.key.RequestSnapshotKey;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestSnapshotService {

    private final RequestService requestService;
    private final GridFsService gridFsService;
    private final ObjectMapper objectMapper;
    private final RequestAuthorizationService requestAuthorizationService;
    private final RequestSnapshotRepository repository;

    /**
     * Create request.
     *
     * @param requestSnapshot creation request
     * @return created request
     */
    public RequestSnapshot saveSnapshot(RequestSnapshot requestSnapshot,
                                        List<MultipartFile> files,
                                        Optional<FileBody> fileInfo) throws JsonProcessingException {
        Request request =
                objectMapper.readValue(requestSnapshot.getRequest(),
                        Request.class);
        RequestEntitySaveRequest requestEntitySaveRequest =
                objectMapper.readValue(requestSnapshot.getRequest(),
                        RequestEntitySaveRequest.class);
        requestService.checkFilesSize(files);
        if (nonNull(requestEntitySaveRequest.getAuthorization())) {
            AuthorizationSaveRequest authorizationSaveRequest = requestEntitySaveRequest.getAuthorization();
            requestAuthorizationService.encryptAuthorizationParameters(authorizationSaveRequest);
            request.setAuthorization(objectMapper.convertValue(authorizationSaveRequest,
                    AuthorizationUtils
                            .getRequestAuthorizationClassByAuthorizationSaveRequest(authorizationSaveRequest)));
        }
        RequestSnapshot existingSnapshot =
                repository.findById(new RequestSnapshotKey(requestSnapshot.getSessionId(),
                        requestSnapshot.getRequestId())).orElse(null);
        boolean isNewSnapshot = existingSnapshot == null;
        if (isNewSnapshot) {
            requestSnapshot.setCreatedWhen(new Date());
        }
        requestSnapshot.setRequest(objectMapper.writeValueAsString(request));
        return repository.save(requestSnapshot);
    }

    /**
     * Saves multipart file to file system and grid fs.
     *
     * @param sessionId session id
     * @param file      multipart file dictionary or binary
     * @throws IOException could be during file system operations
     */
    public Optional<FileBody> saveFileToFileSystemAndGridFs(UUID sessionId,
                                                            UUID sessionRequestId,
                                                            MultipartFile file,
                                                            TransportType transportType) throws IOException {
        gridFsService.removeFileBySessionId(sessionId);
        FileBody fileInfo = gridFsService.saveBinaryBySessionId(LocalDateTime.now().toString(),
                sessionId, sessionRequestId, file.getInputStream(),
                file.getOriginalFilename(), file.getContentType());
        log.debug("File for request {} was saved with parameters {}", sessionId, fileInfo);
        return Optional.of(fileInfo);
    }

    /**
     * Get snapshot by sessionId and requestId.
     *
     * @param sessionId session id
     * @param requestId request id.
     * @return RequestSnapshotResponse
     */
    public RequestSnapshotResponse getSnapshot(UUID sessionId, UUID requestId) {
        Optional<RequestSnapshot> requestSnapshotOpt = repository.findById(new RequestSnapshotKey(sessionId,
                requestId));
        if (!requestSnapshotOpt.isPresent()) {
            return null;
        }
        RequestSnapshot requestSnapshot = requestSnapshotOpt.get();
        UUID binaryFileId = requestSnapshot.getBinaryFileId();
        FileInfoResponse fileInfoResponse = null;
        if (binaryFileId != null) {
            FileInfo fileInfo =
                    gridFsService.getFileInfoByFileId(binaryFileId);
            if (fileInfo != null) {
                fileInfoResponse = FileInfoResponse.builder()
                        .fileId(binaryFileId)
                        .fileName(fileInfo.getFileName())
                        .fileType(fileInfo.getFileType())
                        .contentType(fileInfo.getContentType())
                        .size(fileInfo.getSize())
                        .build();
            }
        }
        return RequestSnapshotResponse.builder()
                .request(requestSnapshot.getRequest())
                .sessionId(sessionId)
                .requestId(requestId)
                .binaryFile(fileInfoResponse).build();
    }

    /**
     * Remove snapshot by sessionId and requestId.
     *
     * @param sessionId session Id.
     * @param requestId request Id.
     */
    @Transactional
    public void deleteSnapshotByRequestSnapshotKey(UUID sessionId, UUID requestId) {
        repository.deleteBySessionIdAndRequestId(sessionId, requestId);
        gridFsService.removeAllFilesBySessionIdAndSessionRequestId(sessionId,
                requestId);
    }

    /**
     * Remove snapshot by bulk request and sessionId.
     *
     */
    @Transactional
    public void bulkDeleteSnapshots(BulkDeleteSnapshotsRequest bulkDeleteSnapshotsRequest,
                                    UUID sessionId) {
        repository.deleteAllBySessionIdAndRequestIdIn(sessionId, bulkDeleteSnapshotsRequest.getRequestIds());
        gridFsService.bulkRemoveFilesBySnapshotKeys(sessionId, bulkDeleteSnapshotsRequest.getRequestIds());
    }

    /**
     * Remove snapshot by sessionId.
     *
     * @param sessionId session id
     * @param requestId request id
     */
    @Transactional
    public void preSaveRequestProcessing(UUID sessionId, UUID requestId) {
        gridFsService.moveFileFromSnapshotToRequest(sessionId, requestId);
        repository.deleteBySessionIdAndRequestId(sessionId, requestId);
    }

    public List<RequestSnapshotKey> getByCreatedWhenDifferenceGreaterThanReferenceDate(Date referenceDate,
                                                                                       Long expirationPeriod) {
        return repository.findAllByCreatedWhenDifferenceGreaterThanReferenceDate(referenceDate,
                expirationPeriod);
    }
}
