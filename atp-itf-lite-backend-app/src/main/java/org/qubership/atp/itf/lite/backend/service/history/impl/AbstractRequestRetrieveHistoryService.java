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

package org.qubership.atp.itf.lite.backend.service.history.impl;

import java.util.UUID;

import org.javers.core.Javers;
import org.qubership.atp.itf.lite.backend.converters.history.AbstractVersioningMapper;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.FolderRepository;
import org.qubership.atp.itf.lite.backend.feign.dto.history.AbstractCompareEntityDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.HistoryItemTypeDto;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRequestRetrieveHistoryService<S extends Request, D extends AbstractCompareEntityDto>
        extends AbstractRetrieveHistoryService<S, D> {

    private final FolderRepository folderRepository;

    public AbstractRequestRetrieveHistoryService(Javers javers, AbstractVersioningMapper<S, D> abstractVersioningMapper,
                                                 FolderRepository folderRepository) {
        super(javers, abstractVersioningMapper);
        this.folderRepository = folderRepository;
    }

    @Override
    public HistoryItemTypeDto getItemType() {
        return HistoryItemTypeDto.REQUEST;
    }

    String getFolderName(UUID folderId) {
        if (folderId != null) {
            Folder parentFolder = null;
            try {
                parentFolder = folderRepository.getOne(folderId);
            } catch (Exception e) {
                log.error("Cannot get folder by folder id {}", folderId, e);
            }
            return parentFolder == null
                        ? "Parent folder with ID #" + folderId + " has been deleted"
                        : parentFolder.getName();
        } else {
            return "Root";
        }
    }
}
