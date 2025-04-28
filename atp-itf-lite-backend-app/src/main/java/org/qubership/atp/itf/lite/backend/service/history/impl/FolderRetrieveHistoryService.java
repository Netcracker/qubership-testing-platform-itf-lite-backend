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

import org.javers.core.Javers;
import org.javers.shadow.Shadow;
import org.qubership.atp.itf.lite.backend.converters.history.FolderVersioningMapper;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.FolderRepository;
import org.qubership.atp.itf.lite.backend.feign.dto.history.CompareEntityResponseDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.FolderHistoryChangeDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.HistoryItemTypeDto;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FolderRetrieveHistoryService extends AbstractRetrieveHistoryService {

    private final FolderRepository folderRepository;

    @Autowired
    public FolderRetrieveHistoryService(Javers javers,
                                        FolderVersioningMapper mapping,
                                        FolderRepository folderRepository) {
        super(javers, mapping);
        this.folderRepository = folderRepository;
    }

    @Override
    public Class<Folder> getEntityClass() {
        return Folder.class;
    }

    @Override
    public HistoryItemTypeDto getItemType() {
        return HistoryItemTypeDto.FOLDER;
    }

    public FolderVersioningMapper getMapper() {
        return (FolderVersioningMapper) abstractVersioningMapper;
    }

    /**
     * Build compare entity.
     * @param revision snapshot version.
     * @param entity shadow from history.
     * @return response with compare entity and version.
     */
    public CompareEntityResponseDto buildCompareEntity(String revision, Shadow<Object> entity) {
        log.debug("version={}, entity={}", revision, entity);
        Folder folder = (Folder) entity.get();
        FolderHistoryChangeDto resolvedEntity = getMapper().map(folder);
        if (folder.getParentId() != null) {
            Folder parentFolder = null;
            try {
                parentFolder = folderRepository.getOne(folder.getParentId());
            } finally {
                resolvedEntity.setParentFolder(parentFolder == null
                        ? "Parent folder with ID #" + folder.getParentId() + " has been deleted"
                        : parentFolder.getName());
            }
        } else {
            resolvedEntity.setParentFolder("Root");
        }
        setCommonFields(folder, resolvedEntity, entity.getCommitMetadata());
        CompareEntityResponseDto compareEntityResponseDto = new CompareEntityResponseDto();
        compareEntityResponseDto.setRevision(revision);
        compareEntityResponseDto.setCompareEntity(resolvedEntity);
        return compareEntityResponseDto;
    }
}
