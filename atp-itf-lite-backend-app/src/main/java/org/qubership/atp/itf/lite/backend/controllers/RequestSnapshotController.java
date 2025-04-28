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

package org.qubership.atp.itf.lite.backend.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;

import org.qubership.atp.itf.lite.backend.model.api.ApiPath;
import org.qubership.atp.itf.lite.backend.model.api.request.BulkDeleteSnapshotsRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestSnapshotResponse;
import org.qubership.atp.itf.lite.backend.model.entities.FileBody;
import org.qubership.atp.itf.lite.backend.model.entities.RequestSnapshot;
import org.qubership.atp.itf.lite.backend.service.RequestSnapshotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(ApiPath.SERVICE_API_V1_PATH + ApiPath.REQUEST_SNAPSHOT_PATH)
@AllArgsConstructor
@Slf4j
public class RequestSnapshotController {

    private final RequestSnapshotService snapshotService;
    private final ObjectMapper objectMapper;

    /**
     * Save snapshot for request.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#projectId, 'UPDATE')")
    @PostMapping
    public ResponseEntity<Void> saveSnapshot(@RequestParam UUID projectId,
                                             @RequestPart(name = "file", required = false) MultipartFile file,
                                             @RequestPart(name = "files", required = false) List<MultipartFile> files,
                                             @RequestPart(name = "snapshotEntity") @Valid
                                                     RequestSnapshot snapshot) throws IOException {
        RequestEntitySaveRequest requestEntitySaveRequest = objectMapper.readValue(snapshot.getRequest(),
                RequestEntitySaveRequest.class);
        Optional<FileBody> fileInfo = Optional.empty();
        if (file != null) {
            fileInfo = snapshotService.saveFileToFileSystemAndGridFs(snapshot.getSessionId(),
                    snapshot.getRequestId(), file,
                    requestEntitySaveRequest.getTransportType());
        }
        snapshotService.saveSnapshot(snapshot, files, fileInfo);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Get snapshot for request by sessionId and requestId.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#projectId, 'READ')")
    @GetMapping
    public ResponseEntity<RequestSnapshotResponse> getSnapshotById(@RequestParam UUID projectId,
                                                                   @RequestParam UUID sessionId,
                                                                   @RequestParam UUID requestId) {
        RequestSnapshotResponse response = snapshotService.getSnapshot(sessionId, requestId);
        return response == null
                ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                : ResponseEntity.ok(response);
    }

    /**
     * Delete snapshot for request by sessionId.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#projectId, 'READ')")
    @DeleteMapping
    public ResponseEntity<Void> deleteSnapshotBySessionId(@RequestParam UUID projectId,
                                                          @RequestParam UUID requestId,
                                                          @RequestParam UUID sessionId) {
        snapshotService.deleteSnapshotByRequestSnapshotKey(sessionId, requestId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Bulk delete snapshot for request by bulkDeleteSnapshotsRequest.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.itf.lite.backend.utils.UserManagementEntities).REQUEST.getName(),"
            + "#projectId, 'READ')")
    @PostMapping(value = "/bulkDelete")
    public ResponseEntity<Void> bulkDeleteSnapshots(@RequestParam UUID projectId,
                                                    @RequestBody BulkDeleteSnapshotsRequest bulkDeleteSnapshotsRequest,
                                                    @RequestParam UUID sessionId) {
        snapshotService.bulkDeleteSnapshots(bulkDeleteSnapshotsRequest, sessionId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
