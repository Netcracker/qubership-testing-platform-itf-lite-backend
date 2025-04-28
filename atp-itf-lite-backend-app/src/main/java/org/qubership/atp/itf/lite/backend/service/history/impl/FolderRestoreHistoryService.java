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

import java.util.List;

import org.javers.core.Javers;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.feign.dto.history.HistoryItemTypeDto;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.service.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FolderRestoreHistoryService extends AbstractRestoreHistoryService<Folder> {

    @Autowired
    public FolderRestoreHistoryService(Javers javers, FolderService folderService,
                                       ValidateReferenceExistsService validateReferenceExistsService,
                                       ModelMapper modelMapper) {
        super(javers, folderService, validateReferenceExistsService, modelMapper);
    }

    @Override
    public HistoryItemTypeDto getItemType() {
        return HistoryItemTypeDto.FOLDER;
    }

    @Override
    public Class<Folder> getEntityClass() {
        return Folder.class;
    }

    @Override
    void updateObjectWithChild(Shadow<Folder> shadowFolder) {
    }

    @Override
    public List<Shadow<Object>> getChildShadows(Shadow<Folder> parentShadow, Class targetObject) {
        return javers.findShadows(QueryBuilder.byClass(targetObject).withCommitId(parentShadow.getCommitId()).build());
    }

    @Override
    protected void copyValues(Folder shadow, Folder actualObject) {
        modelMapper.map(shadow, actualObject);
    }
}
