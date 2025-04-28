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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.qubership.atp.itf.lite.backend.dataaccess.repository.FormDataPartRepository;
import org.qubership.atp.itf.lite.backend.exceptions.ItfLiteException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormDataPartService {

    private final FormDataPartRepository formDataPartRepository;
    private final GridFsService gridFsService;

    /**
     * Update File.
     */
    @Transactional
    public void uploadFile(UUID requestId, UUID id, MultipartFile file) {
        UUID fileId = UUID.randomUUID();
        try {
            formDataPartRepository.updateFileInfoById(fileId, file.getSize(), file.getOriginalFilename(), id);
            gridFsService.saveFileByRequestId(LocalDateTime.now().toString(), requestId,
                    file.getInputStream(), file.getOriginalFilename(), fileId);
        } catch (IOException ex) {
            log.error("Failed to upload file for formDataPart (requestId: {}, id: {})", requestId, id);
            throw new ItfLiteException("Failed to upload file");
        }
    }

}
